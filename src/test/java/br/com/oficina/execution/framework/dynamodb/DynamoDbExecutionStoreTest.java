package br.com.oficina.execution.framework.dynamodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DynamoDbExecutionStoreTest {
    private final DynamoDbExecutionStore store = new DynamoDbExecutionStore(new DynamoDbTableNames("oficina-execution-lab"));

    @Test
    void deveMaterializarSeedsComChavesCanonicasDoDynamoDb() {
        assertTrue(store.catalogoItems().stream().anyMatch(item ->
                item.tableName().equals("oficina-execution-lab-catalogo")
                        && item.pk().equals("PECA#" + DynamoDbExecutionStore.SEED_PECA_ID)
                        && item.sk().equals("METADATA")
                        && item.entityType().equals("PECA")));

        assertTrue(store.catalogoItems().stream().anyMatch(item ->
                item.tableName().equals("oficina-execution-lab-catalogo")
                        && item.pk().equals("SERVICO#" + DynamoDbExecutionStore.SEED_SERVICO_ID)
                        && item.sk().equals("METADATA")
                        && item.entityType().equals("SERVICO")));

        assertTrue(store.estoqueItems().stream().anyMatch(item ->
                item.tableName().equals("oficina-execution-lab-estoque")
                        && item.pk().equals("PECA#" + DynamoDbExecutionStore.SEED_PECA_ID)
                        && item.sk().equals("SALDO")
                        && item.attributes().get("quantidadeDisponivel").equals(50)));
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

        assertTrue(store.outboxItems().stream().anyMatch(item ->
                item.tableName().equals("oficina-execution-lab-outbox")
                        && item.pk().equals("OUTBOX#" + event.eventId())
                        && item.sk().equals("EVENT")
                        && item.attributes().get("status").toString().equals("PENDING")));

        assertTrue(store.idempotenciaItems().stream().anyMatch(item ->
                item.tableName().equals("oficina-execution-lab-idempotencia")
                        && item.pk().equals("IDEMPOTENCY#POST /api/v1/execucoes#request-1")
                        && item.sk().equals("REQUEST")
                        && item.attributes().get("processingStatus").toString().equals("COMPLETED")));
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

        assertTrue(store.listarFilaExecucao(null).stream().noneMatch(item -> item.execucaoId().equals(execucao.execucaoId())));
        assertTrue(store.execucaoItems().stream().anyMatch(item ->
                item.pk().equals("EXECUCAO#" + execucao.execucaoId())
                        && item.sk().equals("METADATA")
                        && !item.attributes().containsKey("filaStatus")
                        && !item.attributes().containsKey("prioridadeCriadoEm")));
    }
}
