package br.com.oficina.execution.framework.dynamodb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus;
import br.com.oficina.execution.framework.dynamodb.OutboxEventRecord.OutboxStatus;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DynamoDbRecordValidationTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-12T12:00:00Z");

    @Test
    void deveValidarCamposObrigatoriosDoItemDynamoDb() {
        assertInvalido("Nome da tabela DynamoDB e obrigatorio.", () -> item(null, "pk", "sk", "entity", Map.of()));
        assertInvalido("Nome da tabela DynamoDB e obrigatorio.", () -> item(" ", "pk", "sk", "entity", Map.of()));
        assertInvalido("PK do item DynamoDB e obrigatoria.", () -> item("table", null, "sk", "entity", Map.of()));
        assertInvalido("PK do item DynamoDB e obrigatoria.", () -> item("table", " ", "sk", "entity", Map.of()));
        assertInvalido("SK do item DynamoDB e obrigatoria.", () -> item("table", "pk", null, "entity", Map.of()));
        assertInvalido("SK do item DynamoDB e obrigatoria.", () -> item("table", "pk", " ", "entity", Map.of()));
        assertInvalido("entityType do item DynamoDB e obrigatorio.", () -> item("table", "pk", "sk", null, Map.of()));
        assertInvalido("entityType do item DynamoDB e obrigatorio.", () -> item("table", "pk", "sk", " ", Map.of()));
    }

    @Test
    void deveNormalizarAtributosDoItemDynamoDb() {
        var attributes = new LinkedHashMap<String, Object>();
        attributes.put("status", "CRIADA");

        var item = item("table", "PK", "SK", "EXECUCAO", attributes);
        attributes.put("status", "ALTERADA");

        assertEquals("PK|SK", item.key());
        assertEquals("CRIADA", item.attributes().get("status"));
        assertTrue(item("table", "PK", "SK", "EXECUCAO", null).attributes().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> item.attributes().put("novo", "valor"));
    }

    @Test
    void deveValidarCamposObrigatoriosDaOutbox() {
        assertInvalido("eventId da Outbox e obrigatorio.", () -> outbox(null, "tipo", 1, "topico", "producer", "aggregate", OutboxStatus.PENDING, 0, "corr", NOW, NOW));
        assertInvalido("eventType da Outbox e obrigatorio.", () -> outbox(UUID.randomUUID(), null, 1, "topico", "producer", "aggregate", OutboxStatus.PENDING, 0, "corr", NOW, NOW));
        assertInvalido("eventType da Outbox e obrigatorio.", () -> outbox(UUID.randomUUID(), " ", 1, "topico", "producer", "aggregate", OutboxStatus.PENDING, 0, "corr", NOW, NOW));
        assertInvalido("eventVersion da Outbox deve ser positivo.", () -> outbox(UUID.randomUUID(), "tipo", 0, "topico", "producer", "aggregate", OutboxStatus.PENDING, 0, "corr", NOW, NOW));
        assertInvalido("Topico da Outbox e obrigatorio.", () -> outbox(UUID.randomUUID(), "tipo", 1, null, "producer", "aggregate", OutboxStatus.PENDING, 0, "corr", NOW, NOW));
        assertInvalido("Topico da Outbox e obrigatorio.", () -> outbox(UUID.randomUUID(), "tipo", 1, " ", "producer", "aggregate", OutboxStatus.PENDING, 0, "corr", NOW, NOW));
        assertInvalido("Producer da Outbox e obrigatorio.", () -> outbox(UUID.randomUUID(), "tipo", 1, "topico", null, "aggregate", OutboxStatus.PENDING, 0, "corr", NOW, NOW));
        assertInvalido("Producer da Outbox e obrigatorio.", () -> outbox(UUID.randomUUID(), "tipo", 1, "topico", " ", "aggregate", OutboxStatus.PENDING, 0, "corr", NOW, NOW));
        assertInvalido("aggregateId da Outbox e obrigatorio.", () -> outbox(UUID.randomUUID(), "tipo", 1, "topico", "producer", null, OutboxStatus.PENDING, 0, "corr", NOW, NOW));
        assertInvalido("aggregateId da Outbox e obrigatorio.", () -> outbox(UUID.randomUUID(), "tipo", 1, "topico", "producer", " ", OutboxStatus.PENDING, 0, "corr", NOW, NOW));
        assertInvalido("Status da Outbox e obrigatorio.", () -> outbox(UUID.randomUUID(), "tipo", 1, "topico", "producer", "aggregate", null, 0, "corr", NOW, NOW));
        assertInvalido("Tentativas da Outbox nao podem ser negativas.", () -> outbox(UUID.randomUUID(), "tipo", 1, "topico", "producer", "aggregate", OutboxStatus.PENDING, -1, "corr", NOW, NOW));
        assertInvalido("correlationId da Outbox e obrigatorio.", () -> outbox(UUID.randomUUID(), "tipo", 1, "topico", "producer", "aggregate", OutboxStatus.PENDING, 0, null, NOW, NOW));
        assertInvalido("correlationId da Outbox e obrigatorio.", () -> outbox(UUID.randomUUID(), "tipo", 1, "topico", "producer", "aggregate", OutboxStatus.PENDING, 0, " ", NOW, NOW));
        assertInvalido("Datas da Outbox sao obrigatorias.", () -> outbox(UUID.randomUUID(), "tipo", 1, "topico", "producer", "aggregate", OutboxStatus.PENDING, 0, "corr", null, NOW));
        assertInvalido("Datas da Outbox sao obrigatorias.", () -> outbox(UUID.randomUUID(), "tipo", 1, "topico", "producer", "aggregate", OutboxStatus.PENDING, 0, "corr", NOW, null));
    }

    @Test
    void deveNormalizarPayloadDaOutbox() {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("status", "CRIADA");

        var outbox = outbox(UUID.randomUUID(), "tipo", 1, "topico", "producer", "aggregate", OutboxStatus.PENDING, 0, "corr", NOW, NOW, payload);
        payload.put("status", "ALTERADA");

        assertEquals("CRIADA", outbox.payload().get("status"));
        assertTrue(outbox(UUID.randomUUID(), "tipo", 1, "topico", "producer", "aggregate", OutboxStatus.PENDING, 0, "corr", NOW, NOW, null).payload().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> outbox.payload().put("novo", "valor"));
    }

    @Test
    void deveValidarCamposObrigatoriosDoRegistroDeIdempotenciaDynamoDb() {
        assertInvalido("Escopo de idempotencia e obrigatorio.", () -> idempotency(null, "key", ProcessingStatus.PROCESSING, "corr", NOW, NOW, NOW));
        assertInvalido("Escopo de idempotencia e obrigatorio.", () -> idempotency(" ", "key", ProcessingStatus.PROCESSING, "corr", NOW, NOW, NOW));
        assertInvalido("Chave de idempotencia e obrigatoria.", () -> idempotency("scope", null, ProcessingStatus.PROCESSING, "corr", NOW, NOW, NOW));
        assertInvalido("Chave de idempotencia e obrigatoria.", () -> idempotency("scope", " ", ProcessingStatus.PROCESSING, "corr", NOW, NOW, NOW));
        assertInvalido("Status de processamento de idempotencia e obrigatorio.", () -> idempotency("scope", "key", null, "corr", NOW, NOW, NOW));
        assertInvalido("CorrelationId de idempotencia e obrigatorio.", () -> idempotency("scope", "key", ProcessingStatus.PROCESSING, null, NOW, NOW, NOW));
        assertInvalido("CorrelationId de idempotencia e obrigatorio.", () -> idempotency("scope", "key", ProcessingStatus.PROCESSING, " ", NOW, NOW, NOW));
        assertInvalido("Datas de idempotencia sao obrigatorias.", () -> idempotency("scope", "key", ProcessingStatus.PROCESSING, "corr", null, NOW, NOW));
        assertInvalido("Datas de idempotencia sao obrigatorias.", () -> idempotency("scope", "key", ProcessingStatus.PROCESSING, "corr", NOW, null, NOW));
        assertInvalido("Datas de idempotencia sao obrigatorias.", () -> idempotency("scope", "key", ProcessingStatus.PROCESSING, "corr", NOW, NOW, null));
    }

    private static DynamoDbItem item(
            String tableName,
            String pk,
            String sk,
            String entityType,
            Map<String, Object> attributes) {
        return new DynamoDbItem(tableName, pk, sk, entityType, attributes);
    }

    private static OutboxEventRecord outbox(
            UUID eventId,
            String eventType,
            int eventVersion,
            String topic,
            String producer,
            String aggregateId,
            OutboxStatus status,
            int attempts,
            String correlationId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
        return outbox(eventId, eventType, eventVersion, topic, producer, aggregateId, status, attempts, correlationId, createdAt, updatedAt, Map.of());
    }

    private static OutboxEventRecord outbox(
            UUID eventId,
            String eventType,
            int eventVersion,
            String topic,
            String producer,
            String aggregateId,
            OutboxStatus status,
            int attempts,
            String correlationId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            Map<String, Object> payload) {
        return new OutboxEventRecord(
                eventId,
                eventType,
                eventVersion,
                topic,
                producer,
                aggregateId,
                payload,
                status,
                attempts,
                null,
                null,
                null,
                null,
                correlationId,
                createdAt,
                updatedAt);
    }

    private static IdempotencyRecord idempotency(
            String scope,
            String key,
            ProcessingStatus status,
            String correlationId,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime expiresAt) {
        return new IdempotencyRecord(
                scope,
                key,
                "hash",
                null,
                null,
                status,
                correlationId,
                "request-id",
                createdAt,
                updatedAt,
                expiresAt);
    }

    private static void assertInvalido(String mensagem, Executable executable) {
        var exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(mensagem, exception.getMessage());
    }
}
