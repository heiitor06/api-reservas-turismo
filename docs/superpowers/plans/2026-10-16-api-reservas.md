# API de Reservas: Plano de Implementação

> **Modo de execução (definido pelo Carlos):** uma etapa por vez. Em cada etapa o Claude entrega o código pronto para colar, explica as decisões importantes e diz o que testar. Só se passa para a próxima etapa com o critério de conclusão cumprido. Steps usam checkbox (`- [ ]`).

**Goal:** API REST de reservas para agência de turismo, com controle de vagas à prova de concorrência e provado por teste.

**Architecture:** Spring Boot em pacotes por funcionalidade (controller → service → repository, DTOs em `record`, mapeamento manual). Regras de estado nas entidades, cálculo de preço numa classe pura, lock pessimista na linha da saída. Postgres versionado por Flyway; testes de integração com Testcontainers.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Maven, Spring Web, Spring Data JPA, Validation, PostgreSQL 17, Flyway, Springdoc, JUnit 5, Mockito, AssertJ, Testcontainers, Docker Compose.

**Spec:** `docs/superpowers/specs/2026-10-16-api-reservas-design.md`

## Convenções do plano

- `B/` = `src/main/java/dev/carlosheitor/agencia/`, `T/` = `src/test/java/dev/carlosheitor/agencia/`, `M/` = `src/main/resources/db/migration/`
- groupId `dev.carlosheitor`, artifactId `api-agencia`, pacote base `dev.carlosheitor.agencia` (pode trocar na Etapa 0; o resto do plano segue o nome escolhido).
- Testes unitários terminam em `Test` (Surefire, `./mvnw test`). Testes de integração terminam em `IT` (Failsafe, `./mvnw verify`).
- Comandos no Git Bash (`./mvnw`); no PowerShell, `.\mvnw.cmd`.

## Global Constraints

- Java 25; Spring Boot 4.1.x; versões das dependências geridas pelo parent do Boot. Versão fora do parent (Springdoc) confirmada na documentação oficial na hora de adicionar.
- `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`. Schema só por Flyway.
- Dinheiro: `BigDecimal`, colunas `NUMERIC(12,2)`; percentuais como fração em `NUMERIC(5,4)` (10% = `0.1000`). Arredondamento `HALF_EVEN`, 2 casas, só no valor final.
- Datas: `Instant` / `timestamptz` (UTC) para momentos; `LocalDate` / `date` para `data_partida`. Toda leitura de "agora" via bean `Clock`.
- Enums gravados como `varchar` + `CHECK`; mapeados com `@Enumerated(EnumType.STRING)`.
- Entidade nunca sai do service: controller recebe e devolve `record`.
- Erros sempre em `ProblemDetail`, com `type` = `https://api.agencia.local/problemas/<slug>` (tabela da spec, seção 8).
- Postgres `postgres:17-alpine`, igual no Compose e no Testcontainers.
- Teste de concorrência nunca é `@Transactional`; o banco é limpo com `TRUNCATE ... RESTART IDENTITY CASCADE` antes de cada teste de integração.

## Review Focus

1. Dois cancelamentos simultâneos da mesma reserva → as vagas voltam uma vez só; o segundo recebe 409. (Etapa 7)
2. Dois pagamentos registrados ao mesmo tempo que, juntos, passam do saldo → só um é aceito; o outro recebe 409 `saldo-excedido`. (Etapa 8)
3. Mesmo CPF com e sem máscara (`529.982.247-25` e `52998224725`) → tratado como o mesmo cliente; o segundo cadastro recebe 409. (Etapa 4)
4. Preço quebrado com desconto (999,99 × 7 passageiros) → total exatamente `6299.94`. (Etapa 5)
5. Reserva em saída ainda `ABERTA` cuja data já passou → 409 `operacao-invalida`, sem debitar vaga. (Etapa 6)

---

## Pré-requisitos (antes da Etapa 0)

- [ ] Instalar o JDK 25 (Temurin ou Zulu) e conferir: `java -version` → `25.x`
- [ ] Docker Desktop aberto: `docker info` sem erro

---

### Etapa 0: Scaffold, banco e infra de teste

**Files:**
- Create: projeto via start.spring.io (Maven, Java 25, Boot 4.1.1, Jar). Dependências: Spring Web, Spring Data JPA, Validation, PostgreSQL Driver, Flyway Migration, Testcontainers. Sem Lombok e sem Docker Compose Support.
- Modify: `pom.xml`: adicionar `springdoc-openapi-starter-webmvc-ui` (versão compatível com Boot 4.1, conferida na doc do Springdoc) e `maven-failsafe-plugin` (goals `integration-test`, `verify`).
- Create: `.gitattributes` (`* text=auto`, `mvnw text eol=lf`, `*.cmd text eol=crlf`)
- Modify: `.gitignore` (gerado + `.env`, `.idea/`, `.vscode/`)
- Create: `compose.yaml`: serviço `postgres` (`postgres:17-alpine`, db/usuário/senha `agencia`, porta 5432, volume nomeado, healthcheck `pg_isready`)
- Create: `src/main/resources/application.yml`: datasource `jdbc:postgresql://localhost:5432/agencia` (usuário e senha lidos de `${DB_USER:agencia}` / `${DB_PASSWORD:agencia}`), `ddl-auto: validate`, `open-in-view: false`
- Keep: `T/TestcontainersConfiguration.java` gerado (container Postgres com `@ServiceConnection`); fixar a imagem `postgres:17-alpine`
- Rename: teste gerado para `T/ApiAgenciaApplicationIT.java`

- [ ] **Step 1:** Gerar o projeto, extrair na raiz do repositório, aplicar os arquivos acima.
- [ ] **Step 2:** `./mvnw verify` → `BUILD SUCCESS`, com `ApiAgenciaApplicationIT.contextLoads` rodando contra o container.
- [ ] **Step 3:** `docker compose up -d postgres` e depois `./mvnw spring-boot:run` → a aplicação sobe; `http://localhost:8080/swagger-ui.html` abre (vazio).
- [ ] **Step 4:** Commit: `chore: scaffold Spring Boot 4.1 com Postgres, Flyway e Testcontainers`

**Critério de conclusão:** `./mvnw verify` verde e Swagger UI abrindo com a app rodando contra o Compose.

---

### Etapa 1: Destino + padrão de camadas + tratamento de erros

**Files:**
- Create: `M/V1__criar_destino.sql`
- Create: `B/destino/Destino.java`, `DestinoRepository.java`, `DestinoService.java`, `DestinoController.java`, `dto/DestinoRequest.java`, `dto/DestinoResponse.java`
- Create: `B/compartilhado/erro/NegocioException.java`, `RecursoNaoEncontradoException.java`, `OperacaoInvalidaException.java`, `RecursoDuplicadoException.java`, `Problemas.java`, `GlobalExceptionHandler.java`
- Create: `B/compartilhado/config/ClockConfig.java`, `B/compartilhado/config/WebConfig.java`
- Create: `T/support/IntegrationTestBase.java`, `T/support/LimpezaBanco.java`, `T/support/RelogioTeste.java`
- Test: `T/destino/DestinoControllerIT.java`

**Interfaces:**
- Produces:
  - `abstract class NegocioException extends RuntimeException` com `NegocioException(HttpStatus status, String slug, String mensagem)`, `HttpStatus status()`, `String slug()`, `Map<String, Object> propriedades()` (padrão: vazio)
  - `RecursoNaoEncontradoException(String recurso, Object id)` → 404 `recurso-nao-encontrado`, mensagem `"<recurso> <id> não encontrado(a)"`
  - `OperacaoInvalidaException(String mensagem)` → 409 `operacao-invalida`
  - `RecursoDuplicadoException(String mensagem)` → 409 `recurso-duplicado`
  - `Problemas.tipo(String slug): URI`
  - `GlobalExceptionHandler extends ResponseEntityExceptionHandler`: trata `NegocioException` (copiando `propriedades()` para o ProblemDetail); sobrescreve `handleMethodArgumentNotValid` (400 `dados-invalidos`, extensão `erros: [{campo, mensagem}]`) e `handleHttpMessageNotReadable` (400 `dados-invalidos`); `Exception` → 500 `erro-interno` com log do erro e mensagem genérica.
  - Bean `Clock` (`Clock.systemUTC()`); `WebConfig` com `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`
  - `DestinoService`: `DestinoResponse criar(DestinoRequest)`, `Page<DestinoResponse> listar(Pageable)`, `DestinoResponse buscar(Long id)`, `DestinoResponse atualizar(Long id, DestinoRequest)`, `Destino obterEntidade(Long id)` (público, usado pelo Pacote)
  - `DestinoRequest(@NotBlank @Size(max=120) nome, @NotBlank @Size(max=120) cidade, @Size(max=2) uf, @NotBlank @Size(max=60) pais, descricao)`; `DestinoResponse.from(Destino)`
  - `RelogioTeste extends Clock`: começa em `2026-06-01T12:00:00Z`, `void avancar(Duration)`, `void definir(Instant)`; registrado como `@Primary` em `IntegrationTestBase`
  - `IntegrationTestBase`: `@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class)`; `@BeforeEach` chama `LimpezaBanco.limpar()` e reseta o relógio
  - `LimpezaBanco.limpar()`: lê as tabelas do schema `public` (exceto `flyway_schema_history`) e faz um `TRUNCATE ... RESTART IDENTITY CASCADE`

- [ ] **Step 1: Testes que falham** em `DestinoControllerIT`:
  - `criar_retorna201ComLocation`: POST válido → 201, header `Location` termina em `/api/destinos/1`, `$.nome` igual ao enviado
  - `buscarInexistente_retorna404ProblemDetail`: GET `/api/destinos/999` → 404, `$.type` = `https://api.agencia.local/problemas/recurso-nao-encontrado`, `$.status` = 404
  - `criarComNomeEmBranco_retorna400ComErros`: `$.erros[0].campo` = `"nome"`
  - `jsonMalformado_retorna400`: corpo `{nome:` → 400, `$.type` termina em `dados-invalidos`
  - `listar_retornaPaginado`: 3 criados, `?size=2` → `$.content.length()` = 2, `$.page.totalElements` = 3
  - `atualizar_alteraCampos`: PUT → 200 e GET reflete a mudança
- [ ] **Step 2:** `./mvnw verify -Dit.test=DestinoControllerIT` → FAIL (404 nas rotas)
- [ ] **Step 3:** Implementar a migration, a entidade (construtor protegido para o JPA + fábrica), o repository, o service (`@Transactional`, leitura `readOnly`), o controller e o handler.
- [ ] **Step 4:** `./mvnw verify` → PASS
- [ ] **Step 5:** Commit: `feat: CRUD de destinos e tratamento de erros com ProblemDetail`

**Critério de conclusão:** os 6 testes verdes; o padrão de camadas desta etapa vira o molde das próximas.

---

### Etapa 2: Pacote e roteiro

**Files:**
- Create: `M/V2__criar_pacote_e_item_roteiro.sql` (`CHECK (preco_base > 0)`, `CHECK (duracao_dias > 0)`, FK para `destino`, `item_roteiro.pacote_id` com `ON DELETE CASCADE`)
- Create: `B/pacote/Pacote.java`, `ItemRoteiro.java`, `TipoItem.java`, `PacoteRepository.java`, `PacoteService.java`, `PacoteController.java`, `dto/PacoteRequest.java`, `dto/PacoteResponse.java`, `dto/PacoteResumoResponse.java`, `dto/ItemRoteiroRequest.java`, `dto/ItemRoteiroResponse.java`
- Test: `T/pacote/PacoteControllerIT.java`

**Interfaces:**
- Consumes: `DestinoService.obterEntidade(Long)`
- Produces:
  - `enum TipoItem { HOTEL, TRANSPORTE, PASSEIO }`
  - `Pacote`: `@ManyToOne(LAZY) Destino`, `@OneToMany(mappedBy, cascade ALL, orphanRemoval) @OrderBy("dia, ordem") List<ItemRoteiro> itens`; `void adicionarItem(ItemRoteiro)` (lança `OperacaoInvalidaException` se `dia > duracaoDias`); `void removerItem(Long itemId)` (404 se não for deste pacote); `boolean isAtivo()`; `int getDuracaoDias()`; `BigDecimal getPrecoBase()`
  - `PacoteService`: `criar(PacoteRequest)`, `Page<PacoteResumoResponse> listar(Long destinoId, Boolean ativo, Pageable)` (filtros opcionais), `PacoteResponse buscar(Long id)` (com itens, mapeado dentro da transação), `atualizar(Long, PacoteRequest)`, `PacoteResponse adicionarItem(Long pacoteId, ItemRoteiroRequest)`, `void removerItem(Long pacoteId, Long itemId)`, `Pacote obterEntidade(Long id)`
  - `PacoteRequest(@NotNull destinoId, @NotBlank @Size(max=150) nome, descricao, @NotNull @Positive duracaoDias, @NotNull @Positive @Digits(integer=10, fraction=2) precoBase, @NotNull ativo)`
  - `ItemRoteiroRequest(@NotNull tipo, @NotBlank @Size(max=150) titulo, descricao, @NotNull @Positive dia, @NotNull @PositiveOrZero ordem)`

- [ ] **Step 1: Testes que falham** em `PacoteControllerIT`:
  - `criarComDestinoInexistente_retorna404`
  - `listarFiltrandoInativos_retornaSoInativos`: 2 ativos, 1 inativo, `?ativo=false` → `totalElements` = 1
  - `listarPorDestino_filtra`
  - `adicionarItemComDiaMaiorQueDuracao_retorna409`: pacote de 5 dias, item no dia 6 → 409 `operacao-invalida`
  - `detalhar_retornaItensOrdenadosPorDiaEOrdem`: itens criados fora de ordem → `$.itens[*].titulo` em ordem (dia, ordem)
  - `removerItem_retorna204ESomeDoDetalhe`
  - `precoBaseZero_retorna400`
- [ ] **Step 2:** `./mvnw verify -Dit.test=PacoteControllerIT` → FAIL
- [ ] **Step 3:** Implementar. Filtro de listagem: `@Query` com `(:destinoId is null or p.destino.id = :destinoId) and (:ativo is null or p.ativo = :ativo)`.
- [ ] **Step 4:** `./mvnw verify` → PASS
- [ ] **Step 5:** Commit: `feat: pacotes turísticos com itens de roteiro`

**Critério de conclusão:** os 7 testes verdes; detalhe do pacote sem `LazyInitializationException`.

---

### Etapa 3: Saída e regra de vagas no domínio

**Files:**
- Create: `M/V3__criar_saida.sql` (`CHECK (capacidade_total > 0)`, `CHECK (vagas_disponiveis BETWEEN 0 AND capacidade_total)`, `UNIQUE (pacote_id, data_partida)`, `CHECK status IN (...)`)
- Create: `B/saida/Saida.java`, `StatusSaida.java`, `VagasInsuficientesException.java`, `SaidaRepository.java`, `SaidaService.java`, `SaidaController.java`, `dto/SaidaRequest.java`, `dto/SaidaResponse.java`, `dto/AlterarStatusSaidaRequest.java`
- Test: `T/saida/SaidaTest.java`, `T/saida/SaidaControllerIT.java`

**Interfaces:**
- Consumes: `PacoteService.obterEntidade(Long)`, `Clock`
- Produces:
  - `enum StatusSaida { ABERTA, ENCERRADA, CANCELADA }`
  - `static Saida nova(Pacote pacote, LocalDate dataPartida, int capacidade)`: vagas = capacidade, status `ABERTA`
  - `void reservarVagas(int qtd)`: status ≠ `ABERTA` → `OperacaoInvalidaException`; `qtd > vagasDisponiveis` → `VagasInsuficientesException`
  - `void devolverVagas(int qtd)`: `IllegalStateException` se passar da capacidade (erro de programação, não de negócio)
  - `void alterarStatus(StatusSaida novo)`: `ABERTA ↔ ENCERRADA`; `→ CANCELADA` só se `vagasDisponiveis == capacidadeTotal` (sem reservas ativas, porque toda reserva ativa ocupa vaga); `CANCELADA` é terminal
  - `LocalDate dataRetorno()` = `dataPartida.plusDays(pacote.getDuracaoDias() - 1)`
  - `boolean jaPartiu(LocalDate hoje)` = `!dataPartida.isAfter(hoje)`
  - `VagasInsuficientesException(int disponiveis, int solicitadas)` → 409 `vagas-insuficientes`, `propriedades()` = `{vagasDisponiveis}`
  - `SaidaService`: `SaidaResponse criar(Long pacoteId, SaidaRequest)` (data no passado ou hoje → 409; duplicada → 409 `recurso-duplicado` via `existsByPacoteIdAndDataPartida`), `List<SaidaResponse> listarPorPacote(Long pacoteId)`, `SaidaResponse buscar(Long id)`, `SaidaResponse alterarStatus(Long id, StatusSaida)`, `Saida obterEntidade(Long id)`
  - `SaidaRequest(@NotNull dataPartida, @NotNull @Positive capacidadeTotal)`; `SaidaResponse(id, pacoteId, pacoteNome, dataPartida, dataRetorno, capacidadeTotal, vagasDisponiveis, status)`

- [ ] **Step 1: Testes unitários que falham** em `SaidaTest` (sem Spring):
  - `reservarVagas_reduzVagas`: capacidade 10, reservar 3 → 7
  - `reservarMaisQueDisponivel_lancaComVagasDisponiveis`: capacidade 2, reservar 3 → `VagasInsuficientesException` com `propriedades().get("vagasDisponiveis")` = 2; vagas continuam 2
  - `reservarEmSaidaEncerrada_lancaOperacaoInvalida`
  - `devolverVagas_aumentaVagas`; `devolverAlemDaCapacidade_lancaIllegalState`
  - `cancelarComVagasOcupadas_lancaOperacaoInvalida`; `cancelada_naoVoltaParaAberta`
  - `dataRetorno_consideraDuracao`: partida 2026-12-01, pacote de 5 dias → 2026-12-05
- [ ] **Step 2:** `./mvnw test -Dtest=SaidaTest` → FAIL (não compila)
- [ ] **Step 3:** Implementar `Saida`, `StatusSaida` e `VagasInsuficientesException`.
- [ ] **Step 4:** `./mvnw test -Dtest=SaidaTest` → PASS
- [ ] **Step 5: Testes de integração que falham** em `SaidaControllerIT`:
  - `criar_iniciaComVagasIguaisACapacidade`
  - `criarComDataPassada_retorna409` (relógio em 2026-06-01, partida 2026-05-20)
  - `criarDuplicada_retorna409RecursoDuplicado`
  - `alterarStatusParaEncerrada_retorna200`; `statusInvalidoNoCorpo_retorna400`
- [ ] **Step 6:** Implementar o repository, o service e o controller; `./mvnw verify` → PASS
- [ ] **Step 7:** Commit: `feat: saídas com regra de vagas no domínio`

**Critério de conclusão:** testes unitários e de integração verdes; a regra de vagas é testável sem banco.

---

### Etapa 4: Cliente

**Files:**
- Create: `M/V4__criar_cliente.sql` (`documento varchar(11) UNIQUE`)
- Create: `B/cliente/Cliente.java`, `ClienteRepository.java`, `ClienteService.java`, `ClienteController.java`, `dto/ClienteRequest.java`, `dto/ClienteResponse.java`
- Test: `T/cliente/ClienteControllerIT.java`

**Interfaces:**
- Produces:
  - `ClienteRequest(@NotBlank @Size(max=150) nome, @NotBlank @CPF documento, @NotBlank @Email email, @NotBlank @Size(max=20) telefone)`, com `@CPF` de `org.hibernate.validator.constraints.br`
  - `ClienteService`: `criar(ClienteRequest)` (normaliza o documento para só dígitos antes de checar duplicidade e salvar; duplicado → 409 `recurso-duplicado`), `Page<ClienteResponse> listar(Pageable)`, `ClienteResponse buscar(Long id)`, `Cliente obterEntidade(Long id)`
  - `ClienteResponse` mostra o documento mascarado (`***.982.247-**`): dado pessoal não volta inteiro na listagem

- [ ] **Step 1: Testes que falham:**
  - `criar_retorna201ComDocumentoMascarado`
  - `mesmoCpfComESemMascara_segundoRetorna409`: `529.982.247-25` e depois `52998224725` → 409 (Review Focus 3)
  - `cpfComDigitoInvalido_retorna400`: `529.982.247-26`
  - `emailInvalido_retorna400`
  - `buscarInexistente_retorna404`
- [ ] **Step 2:** `./mvnw verify -Dit.test=ClienteControllerIT` → FAIL
- [ ] **Step 3:** Implementar. A duplicidade é checada com `existsByDocumento`; a `UNIQUE` do banco é a rede de segurança.
- [ ] **Step 4:** `./mvnw verify` → PASS
- [ ] **Step 5:** Commit: `feat: cadastro de clientes com validação de CPF`

**Critério de conclusão:** 5 testes verdes.

---

### Etapa 5: Cálculo de preço e cotação

**Files:**
- Create: `B/preco/PrecoProperties.java`, `CalculadoraPreco.java`, `CalculoReserva.java`, `CotacaoService.java`, `CotacaoController.java`, `dto/CotacaoRequest.java`, `dto/CotacaoResponse.java`
- Create: `B/pagamento/FormaPagamento.java`
- Modify: `application.yml` (bloco `agencia.preco` com os valores abaixo)
- Test: `T/preco/CalculadoraPrecoTest.java`, `T/preco/CotacaoControllerIT.java`

**Interfaces:**
- Consumes: `SaidaService.obterEntidade(Long)`
- Produces:
  - `enum FormaPagamento { PIX, CARTAO, BOLETO }`
  - `@ConfigurationProperties("agencia.preco") record PrecoProperties(int minimoPassageirosGrupo, BigDecimal percentualDescontoGrupo, int parcelasSemTaxa, int maximoParcelas, BigDecimal taxaPorParcelaExtra)`. Valores no yml: `6`, `0.10`, `3`, `10`, `0.015`
  - `record CalculoReserva(BigDecimal valorUnitario, BigDecimal subtotal, BigDecimal percentualDesconto, BigDecimal valorTotal)`
  - `CalculadoraPreco` (`@Component`, construtor recebendo `PrecoProperties`):
    - `CalculoReserva calcularReserva(BigDecimal precoBase, int qtdPassageiros)`
    - `BigDecimal taxaParcelamento(FormaPagamento forma, int parcelas)`: `IllegalArgumentException` se PIX/BOLETO com parcelas ≠ 1, ou se parcelas fora de 1..`maximoParcelas`
    - `BigDecimal valorCobrado(BigDecimal valor, BigDecimal taxa)`
  - `CotacaoRequest(@NotNull saidaId, @NotNull @Min(1) qtdPassageiros, @NotNull forma, @NotNull @Min(1) @Max(10) parcelas)`, com `@AssertTrue(message = "PIX e boleto só aceitam 1 parcela") boolean isParcelasCompativeis()`
  - `CotacaoResponse(valorUnitario, subtotal, percentualDesconto, valorDesconto, valorTotal, taxaParcelamento, valorCobrado, parcelas, valorParcela, vagasDisponiveis)`
  - Cotação não trava nada nem valida vagas: só informa `vagasDisponiveis`

- [ ] **Step 1: Testes que falham** em `CalculadoraPrecoTest` (properties instanciadas na mão; `@ParameterizedTest` + `@CsvSource`):

  | precoBase | qtd | subtotal | percentualDesconto | valorTotal |
  |---|---|---|---|---|
  | 1500.00 | 2 | 3000.00 | 0 | 3000.00 |
  | 1500.00 | 5 | 7500.00 | 0 | 7500.00 |
  | 1500.00 | 6 | 9000.00 | 0.10 | 8100.00 |
  | 999.99 | 7 | 6999.93 | 0.10 | 6299.94 |

  | forma | parcelas | taxa |
  |---|---|---|
  | PIX | 1 | 0 |
  | BOLETO | 1 | 0 |
  | CARTAO | 3 | 0 |
  | CARTAO | 4 | 0.015 |
  | CARTAO | 6 | 0.045 |
  | CARTAO | 10 | 0.105 |

  - `pixParcelado_lancaIllegalArgument`; `cartaoEm11x_lancaIllegalArgument`
  - `valorCobrado_aplicaTaxa`: `valorCobrado(3000.00, 0.045)` = `3135.00`
  - comparar `BigDecimal` com `isEqualByComparingTo`
- [ ] **Step 2:** `./mvnw test -Dtest=CalculadoraPrecoTest` → FAIL
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** `./mvnw test` → PASS
- [ ] **Step 5: Testes de integração** em `CotacaoControllerIT`:
  - `cotarGrupoNoCartao6x`: preço 1500, 6 passageiros, CARTAO 6x → `valorTotal` 8100.00, `valorCobrado` 8464.50, `valorParcela` 1410.75
  - `pixEm3x_retorna400`
  - `saidaInexistente_retorna404`
- [ ] **Step 6:** Implementar o service e o controller; `./mvnw verify` → PASS
- [ ] **Step 7:** Commit: `feat: cálculo de preço configurável e endpoint de cotação`

**Critério de conclusão:** tabelas de preço e taxa verdes; cotação respondendo pela API.

---

### Etapa 6: Reserva (versão ingênua) + teste que prova o overbooking

**Files:**
- Create: `M/V5__criar_reserva.sql` (`CHECK (qtd_passageiros > 0)`, `UNIQUE (codigo)`, `CHECK status IN (...)`, índice em `saida_id`)
- Create: `B/reserva/Reserva.java`, `StatusReserva.java`, `GeradorCodigoReserva.java`, `ReservaRepository.java`, `ReservaService.java`, `ReservaController.java`, `dto/CriarReservaRequest.java`, `dto/ReservaResponse.java`
- Test: `T/reserva/ReservaTest.java`, `T/reserva/ReservaControllerIT.java`, `T/reserva/ConcorrenciaReservaIT.java`
- Create: `docs/concorrencia.md`

**Interfaces:**
- Consumes: `ClienteService.obterEntidade`, `SaidaService.obterEntidade`, `Saida.reservarVagas/devolverVagas/jaPartiu`, `CalculadoraPreco.calcularReserva`, `Clock`
- Produces:
  - `enum StatusReserva { PENDENTE, CONFIRMADA, CANCELADA }`
  - `static Reserva criar(String codigo, Cliente cliente, Saida saida, int qtdPassageiros, CalculoReserva calculo, Instant agora)`
  - `void confirmar()` (só de `PENDENTE`); `void cancelar(Instant agora)` (de `PENDENTE` ou `CONFIRMADA`); transição inválida → `OperacaoInvalidaException`
  - `GeradorCodigoReserva.gerar(): String`: `"RSV-"` + 6 caracteres de `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` via `SecureRandom`
  - `ReservaService`: `ReservaResponse criar(CriarReservaRequest)`, `ReservaResponse buscar(String codigo)`, `ReservaResponse cancelar(String codigo)`
  - `CriarReservaRequest(@NotNull clienteId, @NotNull saidaId, @NotNull @Min(1) qtdPassageiros)`
  - `ReservaResponse(codigo, clienteId, clienteNome, saidaId, pacoteNome, dataPartida, dataRetorno, qtdPassageiros, valorUnitario, percentualDesconto, valorTotal, status, criadaEm, canceladaEm)`
  - Regras do `criar`: pacote inativo → 409; `saida.jaPartiu(hoje)` → 409; depois `saida.reservarVagas(qtd)`
  - **Versão ingênua de propósito:** `saidaRepository.findById` sem lock

- [ ] **Step 1: Unitários** em `ReservaTest`: `confirmarPendente_ok`, `confirmarCancelada_lanca`, `cancelarConfirmada_ok`, `cancelarCancelada_lanca`, `codigoGerado_temFormatoEsperado` (regex `^RSV-[A-HJ-NP-Z2-9]{6}$`) → FAIL, implementar, PASS
- [ ] **Step 2: Integração** em `ReservaControllerIT`:
  - `criar_debitaVagasEGravaPreco`: saída de 10 vagas, reserva de 2 → 201, saída com 8; `valorUnitario` = preço do pacote
  - `alterarPrecoDoPacote_naoMudaReservaExistente`
  - `grupoDe6_aplicaDesconto`
  - `saidaSemVagas_retorna409ComVagasDisponiveis`: `$.vagasDisponiveis` = 1
  - `qtdZero_retorna400`
  - `pacoteInativo_retorna409`
  - `saidaQueJaPartiu_retorna409SemDebitarVagas`: cria saída para 2026-06-10, `relogio.avancar(30 dias)`, reserva → 409 e vagas inalteradas (Review Focus 5)
  - `cancelar_devolveVagas`; `cancelarDuasVezes_retorna409`; `codigoInexistente_retorna404`
- [ ] **Step 3:** Implementar; `./mvnw verify` → PASS
- [ ] **Step 4: Teste de concorrência** em `ConcorrenciaReservaIT` (estende `IntegrationTestBase`, chama `ReservaService` direto, não MockMvc):
  - `vinteThreadsDisputandoCincoVagas_criamExatamenteCincoReservas`: saída com 5 vagas; 20 tarefas num `ExecutorService` de 20 threads; um `CountDownLatch largada` faz todas esperarem e começarem juntas; cada uma chama `criar(qtd=1)`; conta sucessos e `VagasInsuficientesException` em `AtomicInteger`s. Asserts: sucessos = 5, falhas = 15, `vagasDisponiveis` = 0, `reservaRepository.count()` = 5
- [ ] **Step 5:** `./mvnw verify -Dit.test=ConcorrenciaReservaIT` → **FAIL esperado** (mais de 5 reservas). O `CHECK (vagas >= 0)` não pega: cada thread grava o valor absoluto calculado em memória, e não "vagas − 1". Se passar por sorte, rodar de novo. Registrar a saída do teste em `docs/concorrencia.md`.
- [ ] **Step 6:** Commit: `test: prova de overbooking na versão sem lock (falha esperada)`

**Critério de conclusão:** CRUD de reserva verde e o teste de concorrência **reproduzindo o overbooking**, com a evidência registrada.

---

### Etapa 7: Lock pessimista

**Files:**
- Modify: `B/saida/SaidaRepository.java`, `B/reserva/ReservaRepository.java`, `B/reserva/ReservaService.java`, `B/compartilhado/erro/GlobalExceptionHandler.java`
- Modify: `T/reserva/ConcorrenciaReservaIT.java`, `docs/concorrencia.md`

**Interfaces:**
- Produces:
  - `SaidaRepository.findByIdForUpdate(Long id): Optional<Saida>`: `@Lock(PESSIMISTIC_WRITE)`, `@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))`, `@Query("select s from Saida s where s.id = :id")`
  - `ReservaRepository.findByCodigoForUpdate(String codigo): Optional<Reserva>` (mesmo padrão) e `ReservaRepository.findSaidaIdByCodigo(String codigo): Optional<Long>` (consulta escalar, sem carregar a entidade)
  - `criar`: trava a saída antes de validar e debitar
  - `cancelar`: `findSaidaIdByCodigo` → trava a saída → trava a reserva → revalida o status → `cancelar` + `devolverVagas`. A ordem Saída → Reserva é fixa.
  - Handler: `PessimisticLockingFailureException` → 503 `tente-novamente`, header `Retry-After: 1`

- [ ] **Step 1:** Rodar `ConcorrenciaReservaIT` de novo → FAIL (estado da Etapa 6)
- [ ] **Step 2:** Implementar o lock no `criar`; rodar → PASS
- [ ] **Step 3: Novos testes de concorrência** (falham antes do lock no `cancelar`):
  - `reservasECancelamentosSimultaneos_fechamOSaldo`: saída de 10 vagas com 5 reservas prévias de 1 passageiro; ao mesmo tempo, cancela as 5 e tenta 10 reservas novas. Assert: `vagasDisponiveis` = 10 − (soma de `qtd` das reservas não canceladas)
  - `doisCancelamentosSimultaneosDaMesmaReserva_devolvemVagasUmaVez`: reserva de 3 em saída de 10 (7 livres); 2 threads cancelam → vagas = 10, um sucesso, uma `OperacaoInvalidaException` (Review Focus 1)
  - `lockOcupado_retorna503`: uma thread trava a saída num `TransactionTemplate` e segura por 5 s; enquanto isso, POST `/api/reservas` → 503 em menos de 5 s, `$.type` termina em `tente-novamente`
- [ ] **Step 4:** Implementar o `cancelar` com a ordem de locks; `./mvnw verify` → PASS
- [ ] **Step 5: Verificar o timeout:** se `lockOcupado_retorna503` esperar os 5 s inteiros, o dialeto ignorou a hint. Nesse caso, trocar por `spring.datasource.hikari.connection-init-sql: SET lock_timeout = '3s'` e registrar o motivo em `docs/concorrencia.md`.
- [ ] **Step 6:** Completar `docs/concorrencia.md`: comparação das 3 opções (spec, seção 5), saída do teste antes e depois, ordem dos locks.
- [ ] **Step 7:** Commit: `fix: lock pessimista na saída elimina overbooking`

**Critério de conclusão:** a suíte inteira verde, rodando `./mvnw verify` 3 vezes seguidas sem falha intermitente.

---

### Etapa 8: Pagamentos e confirmação

**Files:**
- Create: `M/V6__criar_pagamento.sql` (`CHECK (valor > 0)`, `CHECK (parcelas BETWEEN 1 AND 10)`, `CHECK forma/status IN (...)`, índice em `reserva_id`)
- Create: `B/pagamento/Pagamento.java`, `StatusPagamento.java`, `SaldoExcedidoException.java`, `PagamentoRepository.java`, `PagamentoService.java`, `PagamentoController.java`, `dto/RegistrarPagamentoRequest.java`, `dto/PagamentoResponse.java`
- Modify: `B/reserva/ReservaRepository.java` (`findByIdForUpdate`), `B/reserva/dto/ReservaResponse.java` (+ `valorPago`, `saldo`, `pagamentos`), `B/reserva/ReservaService.java` (`buscar` monta esses campos)
- Test: `T/pagamento/PagamentoTest.java`, `T/pagamento/PagamentoControllerIT.java`, `T/pagamento/ConcorrenciaPagamentoIT.java`

**Interfaces:**
- Consumes: `ReservaRepository.findByCodigoForUpdate`, `Reserva.confirmar()`, `CalculadoraPreco.taxaParcelamento/valorCobrado`, `Clock`
- Produces:
  - `enum StatusPagamento { PENDENTE, APROVADO, RECUSADO, ESTORNADO }`
  - `static Pagamento novo(Reserva, BigDecimal valor, FormaPagamento, int parcelas, BigDecimal taxa, BigDecimal valorCobrado, Instant agora)`; `void aprovar()`, `void recusar()` (só de `PENDENTE`, senão `OperacaoInvalidaException`)
  - `SaldoExcedidoException(BigDecimal saldo)` → 409 `saldo-excedido`, `propriedades()` = `{saldo}`
  - `PagamentoRepository.somarValorPorStatus(Long reservaId, Collection<StatusPagamento> status): BigDecimal` (`coalesce(sum(p.valor), 0)`); `findByReservaIdOrderByCriadoEm(Long)`; `findReservaIdByPagamentoId(Long): Optional<Long>`
  - `PagamentoService`:
    - `PagamentoResponse registrar(String codigoReserva, RegistrarPagamentoRequest)`: trava a reserva; reserva ≠ `PENDENTE` → 409; saldo = `valorTotal − soma(APROVADO, PENDENTE)`; `valor > saldo` → `SaldoExcedidoException`
    - `PagamentoResponse aprovar(Long pagamentoId)`: `findReservaIdByPagamentoId` → trava a reserva → **só então** carrega o pagamento (lido depois do lock, nunca desatualizado) → reserva `CANCELADA` → 409 → `pagamento.aprovar()` → se soma(APROVADO) = `valorTotal`, `reserva.confirmar()`
    - `PagamentoResponse recusar(Long pagamentoId)`: mesmo caminho de lock, sem confirmar
  - `RegistrarPagamentoRequest(@NotNull @Positive @Digits(integer=10, fraction=2) valor, @NotNull forma, @NotNull @Min(1) @Max(10) parcelas)` + o mesmo `@AssertTrue` da cotação
  - `PagamentoResponse(id, valor, forma, parcelas, taxaPercentual, valorCobrado, status, criadoEm)`

- [ ] **Step 1: Unitários** em `PagamentoTest`: `aprovarPendente_ok`, `aprovarAprovado_lanca`, `recusarRecusado_lanca` → FAIL, implementar, PASS
- [ ] **Step 2: Integração** em `PagamentoControllerIT` (reserva base: total 3000.00):
  - `pagamentoIntegralAprovado_confirmaReserva`
  - `sinalMaisSaldo_confirmaSoNoSegundo`: 900.00 aprovado → `PENDENTE`, `saldo` 2100.00; 2100.00 aprovado → `CONFIRMADA`
  - `valorAcimaDoSaldo_retorna409ComSaldo`
  - `pagamentoPendenteSeguraSaldo`: 3000.00 pendente; novo de 1.00 → 409
  - `recusadoLiberaSaldo`: 3000.00 recusado; novo de 3000.00 → 201
  - `cartao6x_cobraComTaxa`: 3000.00 → `valorCobrado` 3135.00
  - `pixEm3x_retorna400`
  - `aprovarDeReservaCancelada_retorna409`; `aprovarJaAprovado_retorna409`; `pagarReservaConfirmada_retorna409`
- [ ] **Step 3:** Implementar; `./mvnw verify` → PASS
- [ ] **Step 4: Concorrência** em `ConcorrenciaPagamentoIT`:
  - `duasAprovacoesSimultaneasDePagamentosDiferentes_confirmamUmaVez`: dois pendentes de 1500.00, aprovados ao mesmo tempo → os dois `APROVADO`, reserva `CONFIRMADA`, nenhuma exceção
  - `mesmaAprovacaoEmParalelo_umSucessoUmConflito`
  - `doisPagamentosSimultaneosAcimaDoSaldo_soUmAceito`: dois `registrar(3000.00)` ao mesmo tempo → 1 sucesso, 1 `SaldoExcedidoException` (Review Focus 2)
- [ ] **Step 5:** `./mvnw verify` → PASS
- [ ] **Step 6:** Commit: `feat: pagamentos com controle de saldo e confirmação da reserva`

**Critério de conclusão:** suíte verde; fluxo reservar → pagar → confirmar funcionando pela API.

---

### Etapa 9: Docker da aplicação, Swagger e README

**Files:**
- Create: `Dockerfile` (multi-stage: build em `eclipse-temurin:25-jdk` com `./mvnw -DskipTests package`; runtime em `eclipse-temurin:25-jre`, usuário não-root, `EXPOSE 8080`)
- Create: `.dockerignore` (`target/`, `.git/`, `.idea/`, `.vscode/`)
- Modify: `compose.yaml`: serviço `app` (build `.`, `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/agencia`, `depends_on: postgres: condition: service_healthy`, porta 8080)
- Create: `B/compartilhado/config/OpenApiConfig.java` (título, descrição, versão); `@Tag` por controller e `@Operation(summary)` nos endpoints de reserva e pagamento
- Create: `README.md`: o que é; diagrama mermaid (da spec); decisões (link para `docs/concorrencia.md`); como rodar (`docker compose up --build`); como testar (`./mvnw test` e `./mvnw verify`); tabela de endpoints; roteiro de `curl` do fluxo completo (destino → pacote → saída → cliente → cotação → reserva → pagamento → aprovação)

- [ ] **Step 1:** `docker compose up --build` → os dois serviços sobem; os logs mostram o Flyway aplicando V1 a V6
- [ ] **Step 2:** Rodar o roteiro de `curl` do README contra o container → a reserva termina `CONFIRMADA`
- [ ] **Step 3:** `http://localhost:8080/swagger-ui.html` lista todos os endpoints agrupados por tag
- [ ] **Step 4:** `./mvnw verify` → PASS
- [ ] **Step 5:** Commit: `docs: README, Dockerfile e documentação OpenAPI`

**Critério de conclusão:** clonando o repositório do zero, `docker compose up --build` + o roteiro do README funcionam sem passo extra.

---

## Depois do plano (fora deste escopo)

Spring Security + JWT, expiração de reserva pendente, passageiros, estorno, preço por saída, `@Version` na edição administrativa de saída, CI no GitHub Actions. Cada um vira uma nova rodada de spec e plano.
