# Concorrência na reserva de vagas

Problema: duas pessoas tentam reservar a última vaga de uma saída ao mesmo tempo. Sem controle, as duas
conseguem: **overbooking**.

## 1. A versão ingênua

```java
@Transactional
public ReservaResponse criar(CriarReservaRequest request) {
    Saida saida = saidaService.obterEntidade(request.saidaId()); // SELECT sem lock
    // ... valida pacote ativo, data
    Reserva reserva = Reserva.criar(..., saida, qtd, ...);       // saida.reservarVagas(qtd): if (qtd > vagas) lança; vagas -= qtd
    return ReservaResponse.from(repository.save(reserva));       // no commit: UPDATE saida SET vagas_disponiveis = ?
}
```

A regra em `Saida.reservarVagas` está certa. O problema é **onde ela roda**: na memória de cada transação,
sobre uma cópia da linha lida antes de as outras gravarem.

```
T1: SELECT vagas → 5          T2: SELECT vagas → 5
T1: 5 >= 1 ok, vagas = 4      T2: 5 >= 1 ok, vagas = 4
T1: UPDATE SET vagas = 4      T2: UPDATE SET vagas = 4   (espera T1 e grava por cima)
T1: COMMIT                    T2: COMMIT
→ 2 reservas, e a saída diz que só 1 vaga foi usada (lost update)
```

### Por que o `CHECK (vagas_disponiveis >= 0)` não salvou

O Hibernate grava o **valor absoluto** calculado em memória (`SET vagas_disponiveis = 4`), e não
`vagas_disponiveis - 1`. Nenhuma transação chega a gravar um número negativo, então a constraint nunca é
violada. A constraint protege contra um valor inválido, mas não contra uma atualização perdida.

## 2. A prova

`ConcorrenciaReservaIT.vinteThreadsDisputandoCincoVagas_criamExatamenteCincoReservas`:
uma saída com **5 vagas**, **20 threads** liberadas juntas por um `CountDownLatch`, cada uma pedindo 1 vaga,
contra um Postgres 17 real (Testcontainers). O teste não é `@Transactional`: cada thread abre e confirma
a sua própria transação.

Esperado: 5 reservas, 15 recusas, 0 vagas.

Resultado na versão ingênua (commit desta etapa), 3 rodadas seguidas:

| Rodada | Reservas gravadas | Recusadas por falta de vaga | Vagas que a saída informa |
|---|---|---|---|
| 1 | **20** | 0 | 3 |
| 2 | **20** | 0 | 3 |
| 3 | **20** | 0 | 2 |

Saída do AssertJ (rodada 1):

```
Multiple Failures (4 failures)
-- failure 1 --
[reservas gravadas no banco]
expected: 5L
 but was: 20L
-- failure 2 --
[reservas aceitas]
expected: 5
 but was: 20
-- failure 3 --
[recusadas por falta de vaga]
expected: 15
 but was: 0
-- failure 4 --
[vagas restantes na saída]
expected: 0
 but was: 3
```

Quatro vezes mais reservas do que vagas, e o número de vagas no banco ficou **errado e otimista**: a saída
continuava à venda, com 3 vagas "livres" num ônibus que já tinha 20 pessoas.

## 3. A correção

_Próxima etapa: lock pessimista na linha da saída, comparação das alternativas e resultado depois da correção._
