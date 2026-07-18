package br.com.oficina.execution.framework.dynamodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus;
import java.util.Map;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

class DynamoDbExecutionStoreTest {
    private static GenericContainer<?> container;
    private static DynamoDbClient client;

    private DynamoDbTableNames tableNames;
    private DynamoDbExecutionStore store;

    @BeforeAll
    static void startDynamoDb() {
        container = DynamoDbLocalTestSupport.startContainer();
        client = DynamoDbLocalTestSupport.client(DynamoDbLocalTestSupport.endpoint(container));
    }

    @AfterAll
    static void stopDynamoDb() {
        if (client != null) {
            client.close();
        }
        if (container != null) {
            container.stop();
        }
    }

    @BeforeEach
    void setUp() {
        tableNames = new DynamoDbTableNames("oficina-execution-unit-" + UUID.randomUUID().toString().substring(0, 8));
        DynamoDbLocalTestSupport.createTables(client, tableNames);
        store = new DynamoDbExecutionStore(tableNames, client);
    }

    @Test
    void deveMaterializarSeedsComChavesCanonicasDoDynamoDb() {
        assertTrue(store.catalogoItems().stream().anyMatch(item ->
                item.tableName().equals(tableNames.catalogo())
                        && item.pk().equals("PECA#" + DynamoDbExecutionStore.SEED_PECA_ID)
                        && item.sk().equals("METADATA")
                        && item.entityType().equals("PECA")));

        assertTrue(store.catalogoItems().stream().anyMatch(item ->
                item.tableName().equals(tableNames.catalogo())
                        && item.pk().equals("SERVICO#" + DynamoDbExecutionStore.SEED_SERVICO_ID)
                        && item.sk().equals("METADATA")
                        && item.entityType().equals("SERVICO")));

        assertTrue(store.estoqueItems().stream().anyMatch(item ->
                item.tableName().equals(tableNames.estoque())
                        && item.pk().equals("PECA#" + DynamoDbExecutionStore.SEED_PECA_ID)
                        && item.sk().equals("SALDO")
                        && item.attributes().get("quantidadeDisponivel").equals(50)));
    }

    @Test
    void deveValidarTodasAsTabelasObrigatoriasNoStartup() {
        var dependencies = new DynamoDbRuntimeDependencies(tableNames, client);

        assertDoesNotThrow(dependencies::validar);
    }

    @Test
    void deveFalharQuandoTabelaObrigatoriaNaoExiste() {
        var missingTableNames = new DynamoDbTableNames("oficina-execution-missing-" + UUID.randomUUID());
        var dependencies = new DynamoDbRuntimeDependencies(missingTableNames, client);

        var exception = assertThrows(IllegalStateException.class, dependencies::validar);

        assertTrue(exception.getMessage().contains(missingTableNames.catalogo()));
    }

    @Test
    void deveAplicarSeedLimpoIdempotenteSemEventosOuExecucoes() {
        store.aplicarSeedLimpo();
        store.aplicarSeedLimpo();

        assertEquals(3, store.listarPecas().size());
        assertEquals(1, store.listarServicos().size());
        assertEquals(0, store.outboxItems().size());
        assertEquals(0, store.execucaoItems().size());
        assertTrue(store.catalogoItems().stream().anyMatch(item ->
                item.pk().equals("PECA#" + DynamoDbExecutionStore.SEED_PNEU_ID)
                        && item.attributes().get("nome").equals("Pneu")));
        assertTrue(store.catalogoItems().stream().anyMatch(item ->
                item.pk().equals("PECA#" + DynamoDbExecutionStore.SEED_TAPETE_ID)
                        && item.attributes().get("nome").equals("Tapete")));
    }

    @Test
    void devePersistirOutboxEIdempotenciaComChavesCanonicas() {
        var event = store.registrarOutbox(
                "diagnosticoIniciado",
                "oficina.execution.diagnostico-iniciado",
                "execucao-1",
                Map.of("execucaoId", "execucao-1"),
                "correlation-1");
        var idempotencia = store.registrarIdempotencia(
                "POST /api/v1/execucoes",
                "request-1",
                "hash-1",
                201,
                "{}",
                ProcessingStatus.COMPLETED);

        assertEquals("diagnosticoIniciado", event.eventType());
        assertEquals(ProcessingStatus.COMPLETED, idempotencia.processingStatus());
        assertTrue(idempotencia.correlationId().startsWith("local-"));

        store.concluirIdempotencia(
                idempotencia.scope(),
                idempotencia.key(),
                ProcessingStatus.FAILED_FINAL,
                409,
                "{\"code\":\"IDEMPOTENCY_CONFLICT\"}");
        var concluida = store.buscarIdempotencia(idempotencia.scope(), idempotencia.key()).orElseThrow();
        assertEquals(ProcessingStatus.FAILED_FINAL, concluida.processingStatus());
        assertEquals(409, concluida.responseStatus());
        assertEquals("{\"code\":\"IDEMPOTENCY_CONFLICT\"}", concluida.responseBody());
        assertEquals(idempotencia.correlationId(), concluida.correlationId());

        assertTrue(store.outboxItems().stream().anyMatch(item ->
                item.tableName().equals(tableNames.outbox())
                        && item.pk().equals("OUTBOX#" + event.eventId())
                        && item.sk().equals("EVENT")
                        && item.attributes().get("status").toString().equals("PENDING")
                        && item.attributes().containsKey("nextAttemptAt")));

        assertTrue(store.idempotenciaItems().stream().anyMatch(item ->
                item.tableName().equals(tableNames.idempotencia())
                        && item.pk().equals("IDEMPOTENCY#POST /api/v1/execucoes#request-1")
                        && item.sk().equals("REQUEST")
                        && item.attributes().get("processingStatus").toString().equals("FAILED_FINAL")
                        && item.attributes().containsKey("correlationId")));
    }

    @Test
    void deveCoordenarClaimDaOutboxEntreReplicasERecuperarLeaseExpirado() {
        var event = store.registrarOutbox(
                "diagnosticoIniciado",
                "oficina.execution.diagnostico-iniciado",
                "execucao-claim",
                Map.of("execucaoId", "execucao-claim"),
                "correlation-claim");
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var firstClaim = store.reivindicarOutboxPendente(10, "replica-a", now.plusMinutes(1));
        var concurrentClaim = store.reivindicarOutboxPendente(10, "replica-b", now.plusMinutes(1));

        assertTrue(firstClaim.stream().anyMatch(candidate -> candidate.eventId().equals(event.eventId())));
        assertTrue(concurrentClaim.stream().noneMatch(candidate -> candidate.eventId().equals(event.eventId())));
        assertThrows(RuntimeException.class, () -> store.marcarOutboxPublicado(event.eventId(), "replica-b"));

        var expiredEvent = store.registrarOutbox(
                "diagnosticoFinalizado",
                "oficina.execution.diagnostico-finalizado",
                "execucao-expired-claim",
                Map.of("execucaoId", "execucao-expired-claim"),
                "correlation-expired-claim");
        store.reivindicarOutboxPendente(10, "replica-a", now.minusSeconds(1));
        var recovered = store.reivindicarOutboxPendente(10, "replica-b", now.plusMinutes(1));
        assertTrue(recovered.stream().anyMatch(candidate -> candidate.eventId().equals(expiredEvent.eventId())));
        assertEquals("PUBLISHED", store.marcarOutboxPublicado(expiredEvent.eventId(), "replica-b").status().name());
    }

    @Test
    void deveGerarOutboxParaMovimentosContratadosDeEstoque() {
        store.registrarMovimento(
                TipoMovimentoEstoque.ENTRADA,
                DynamoDbExecutionStore.SEED_PECA_ID,
                null,
                2,
                "Entrada operacional",
                "corr-estoque-001");
        store.registrarMovimento(
                TipoMovimentoEstoque.RESERVA,
                DynamoDbExecutionStore.SEED_PECA_ID,
                DynamoDbExecutionStore.SEED_ORDEM_SERVICO_ID,
                1,
                "Reserva operacional",
                "corr-estoque-001");

        assertTrue(store.outboxEvents().stream().anyMatch(event ->
                event.eventType().equals("estoqueAcrescentado")
                        && event.eventVersion() == 1
                        && event.topic().equals("oficina.execution.estoque-acrescentado")));
        assertTrue(store.outboxEvents().stream().anyMatch(event ->
                event.eventType().equals("estoqueBaixado")
                        && event.eventVersion() == 1
                        && event.topic().equals("oficina.execution.estoque-baixado")));
    }

    @Test
    void deveMaterializarAtributosDeFilaDaExecucao() {
        var execucao = store.criarExecucao(UUID.randomUUID(), 7);

        assertEquals(execucao.execucaoId(), store.listarFilaExecucao(null).getFirst().execucaoId());
        assertTrue(store.execucaoItems().stream().anyMatch(item ->
                item.pk().equals("EXECUCAO#" + execucao.execucaoId())
                        && item.sk().equals("METADATA")
                        && item.attributes().get("prioridade").equals(7)
                        && item.attributes().get("filaStatus").equals("CRIADA")
                        && item.attributes().get("prioridadeCriadoEm").toString().startsWith("0000000007#")));

        store.iniciarDiagnostico(execucao.execucaoId(), "corr-fila-store-001");

        assertTrue(store.listarFilaExecucao(null).stream().anyMatch(item -> item.execucaoId().equals(execucao.execucaoId())));
        assertTrue(store.execucaoItems().stream().anyMatch(item ->
                item.pk().equals("EXECUCAO#" + execucao.execucaoId())
                        && item.sk().equals("METADATA")
                        && item.attributes().get("filaStatus").equals("EM_DIAGNOSTICO")
                        && item.attributes().containsKey("prioridadeCriadoEm")));

        store.concluirDiagnostico(execucao.execucaoId(), "Falha identificada", "corr-fila-store-002");

        assertTrue(store.listarFilaExecucao(null).stream().noneMatch(item -> item.execucaoId().equals(execucao.execucaoId())));
        assertTrue(store.execucaoItems().stream().anyMatch(item ->
                item.pk().equals("EXECUCAO#" + execucao.execucaoId())
                        && item.sk().equals("METADATA")
                        && !item.attributes().containsKey("filaStatus")
                        && !item.attributes().containsKey("prioridadeCriadoEm")));

        store.iniciarReparo(execucao.execucaoId(), "corr-fila-store-003");

        assertTrue(store.listarFilaExecucao(null).stream().anyMatch(item -> item.execucaoId().equals(execucao.execucaoId())));
        assertTrue(store.execucaoItems().stream().anyMatch(item ->
                item.pk().equals("EXECUCAO#" + execucao.execucaoId())
                        && item.sk().equals("METADATA")
                        && item.attributes().get("filaStatus").equals("EM_REPARO")
                        && item.attributes().containsKey("prioridadeCriadoEm")));
    }
}
