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

## 3. As opções

| | **Lock pessimista** | **Lock otimista (`@Version`)** | **Update condicional** |
|---|---|---|---|
| Como | `SELECT ... FOR UPDATE` na saída; quem chega depois espera | Lê sem lock; `UPDATE ... WHERE version = ?`; quem perde recebe `OptimisticLockException` | `UPDATE saida SET vagas = vagas - :q WHERE id = :id AND vagas >= :q`; 0 linhas = sem vaga |
| Muita gente na mesma vaga | Fila ordenada, cada um espera poucos ms | Ruim: muitas falhas e retentativas em cascata | Ótimo, uma instrução só |
| Retentativa | Não precisa | Precisa, e **fora** do `@Transactional` | Não precisa |
| Onde fica a regra | No domínio (`saida.reservarVagas`) | No domínio | No SQL |
| Mensagem de erro | Precisa (sem vaga × saída fechada) | Precisa | Genérica ("0 linhas"); precisa de outra leitura |
| Custo | Segura conexão enquanto espera; exige timeout; risco de deadlock se travar várias linhas em ordem diferente | Conflito falso: qualquer edição da saída derruba uma reserva | Contexto do JPA fica desatualizado (`@Modifying(clearAutomatically = true)`) |

**Escolha: lock pessimista.** A disputa é concentrada (muita gente na mesma saída, querendo a mesma vaga),
que é o pior caso do otimista. A transação é curta (trava, valida, debita, insere). A regra continua num
método de domínio testável sem banco, e o mesmo lock protege a devolução de vagas no cancelamento.

O update condicional é o mais rápido e funciona porque, no `READ COMMITTED` do Postgres, um `UPDATE` que
esperou o lock de outra transação **reavalia o `WHERE`** sobre a versão nova da linha. Ele não foi escolhido
porque tira a regra do domínio e piora as mensagens, e o volume de uma agência não precisa desse ganho.

## 4. A correção

```java
// SaidaRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
@Query("select s from Saida s where s.id = :id")
Optional<Saida> findByIdForUpdate(Long id);

// ReservaService.criar
Saida saida = saidaService.obterEntidadeComLock(request.saidaId()); // era obterEntidade (sem lock)
```

Uma linha mudou no `criar`. A regra (`Saida.reservarVagas`) é a mesma: agora ela roda com a linha travada,
então cada transação vê as vagas já descontadas pela anterior.

### O cancelamento também precisava

Com o lock só no `criar`, dois testes novos falharam:

| Teste | Esperado | Com lock só no `criar` |
|---|---|---|
| 5 cancelamentos + 10 reservas ao mesmo tempo | vagas = capacidade − passageiros ativos (0) | **6 vagas livres** com a saída lotada |
| Dois cancelamentos simultâneos da mesma reserva | 1 aceito, 1 recusado | **2 aceitos** |

O cancelamento lia a saída sem lock e gravava o valor absoluto por cima, o mesmo *lost update* no sentido
contrário. O lock no `criar` não impede isso: o `SELECT` comum (MVCC) não espera lock nenhum.

Correção, com **ordem fixa dos locks: saída, depois reserva**:

```java
@Transactional
public ReservaResponse cancelar(String codigo) {
    Long saidaId = repository.findSaidaIdByCodigo(codigo)...;   // só o id, sem carregar a reserva
    saidaService.obterEntidadeComLock(saidaId);                  // 1º lock: saída
    Reserva reserva = repository.findByCodigoForUpdate(codigo)...; // 2º lock: reserva (lida já travada)
    reserva.cancelar(clock.instant());                           // status atual: o 2º cancelamento vê CANCELADA
    ...
}
```

- **Ordem fixa evita deadlock.** Se o cancelamento travasse a reserva e depois a saída, enquanto outra
  operação travasse a saída e depois a reserva, as duas poderiam ficar esperando uma pela outra.
- **A reserva só é lida depois dos locks.** Se fosse carregada antes, o Hibernate devolveria a cópia em
  memória mesmo depois do `FOR UPDATE`, com o status antigo. Pelo mesmo motivo a saída tem que ser lida
  **pela primeira vez** já com lock na transação.

### Timeout do lock

Se uma transação segurar a saída por muito tempo, as outras não podem ficar presas indefinidamente segurando
conexões do pool. Com a hint `jakarta.persistence.lock.timeout = 3000`, quem não consegue o lock em 3 s
desiste, e a API responde **503 com `Retry-After: 1`**. Nada foi gravado, então repetir é seguro.
O teste `lockOcupadoAlemDoTimeout_retorna503` segura o lock por até 10 s numa outra transação e confirma que
a reserva desiste entre 2,5 s e 6 s.

Conferido no log (`spring.jpa.show-sql=true`): o Hibernate 7 traduz a hint em `set local lock_timeout = 3000`
antes da consulta e `set local lock_timeout = 0` depois. O Postgres cancela a espera com SQLState `55P03`,
o Spring converte em `PessimisticLockingFailureException`, e o `GlobalExceptionHandler` responde 503.

### `FOR NO KEY UPDATE`, não `FOR UPDATE`

O SQL gerado para `PESSIMISTIC_WRITE` no Postgres é:

```sql
select ... from saida s1_0 where s1_0.id=? for no key update of s1_0
```

`FOR UPDATE` também bloquearia o `FOR KEY SHARE` que o Postgres usa para conferir chaves estrangeiras.
Toda inserção em `reserva` confere a FK `saida_id`, então, com `FOR UPDATE`, inserir uma reserva esperaria
qualquer transação que estivesse com a saída travada. `FOR NO KEY UPDATE` trava contra outras escritas na
linha (que é o que importa para as vagas), mas deixa a checagem de FK passar, porque a chave da saída não muda.

## 5. Depois da correção

| Teste | Esperado | Resultado |
|---|---|---|
| 20 threads × 5 vagas | 5 reservas, 15 recusas, 0 vagas | ✅ 5 / 15 / 0 |
| 5 cancelamentos + 10 reservas simultâneas | saldo fecha | ✅ |
| 2 cancelamentos simultâneos da mesma reserva | 1 aceito, 1 recusado, vagas devolvidas uma vez | ✅ |
| Lock ocupado além do timeout | 503 em ~3 s | ✅ |

Suíte completa (`./mvnw verify`, 109 testes) rodada **3 vezes seguidas**: 3 × `BUILD SUCCESS`, sem falha
intermitente.

## 6. O que ficou de fora (de propósito)

- **`@Version` na saída:** resolveria outro problema, um `PUT` administrativo com dados velhos sobrescrevendo
  uma alteração. Fica para quando houver edição de saída.
- **Expiração de reserva pendente:** hoje a vaga fica presa até alguém cancelar. Um job agendado que cancela
  pendentes vencidas usaria o mesmo `cancelar`, com os mesmos locks.
