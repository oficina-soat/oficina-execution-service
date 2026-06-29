package br.com.oficina.execution.framework.dynamodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus;
import java.util.Map;
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
                        && item.attributes().get("quantidadeDisponivel").equals(10)));
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
}
