package br.com.oficina.execution.framework.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class DomainEventEnvelopeValidationTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-12T12:00:00Z");

    @Test
    void deveValidarCamposObrigatorios() {
        assertInvalido("eventId e obrigatorio.", () -> envelope(null, "execucaoIniciada", 1, NOW, "oficina-execution-service", "aggregate", Map.of()));
        assertInvalido("eventType e obrigatorio.", () -> envelope(UUID.randomUUID(), null, 1, NOW, "oficina-execution-service", "aggregate", Map.of()));
        assertInvalido("eventType e obrigatorio.", () -> envelope(UUID.randomUUID(), " ", 1, NOW, "oficina-execution-service", "aggregate", Map.of()));
        assertInvalido("eventVersion deve ser positivo.", () -> envelope(UUID.randomUUID(), "execucaoIniciada", 0, NOW, "oficina-execution-service", "aggregate", Map.of()));
        assertInvalido("occurredAt e obrigatorio.", () -> envelope(UUID.randomUUID(), "execucaoIniciada", 1, null, "oficina-execution-service", "aggregate", Map.of()));
        assertInvalido("producer e obrigatorio.", () -> envelope(UUID.randomUUID(), "execucaoIniciada", 1, NOW, null, "aggregate", Map.of()));
        assertInvalido("producer e obrigatorio.", () -> envelope(UUID.randomUUID(), "execucaoIniciada", 1, NOW, " ", "aggregate", Map.of()));
        assertInvalido("aggregateId e obrigatorio.", () -> envelope(UUID.randomUUID(), "execucaoIniciada", 1, NOW, "oficina-execution-service", null, Map.of()));
        assertInvalido("aggregateId e obrigatorio.", () -> envelope(UUID.randomUUID(), "execucaoIniciada", 1, NOW, "oficina-execution-service", " ", Map.of()));
    }

    @Test
    void deveNormalizarPayload() {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("status", "CRIADA");

        var envelope = envelope(UUID.randomUUID(), "execucaoIniciada", 1, NOW, "oficina-execution-service", "aggregate", payload);
        payload.put("status", "ALTERADA");

        assertEquals("CRIADA", envelope.payload().get("status"));
        assertTrue(envelope(UUID.randomUUID(), "execucaoIniciada", 1, NOW, "oficina-execution-service", "aggregate", null).payload().isEmpty());
        var envelopePayload = envelope.payload();
        assertThrows(UnsupportedOperationException.class, () -> envelopePayload.put("novo", "valor"));
    }

    private static DomainEventEnvelope envelope(
            UUID eventId,
            String eventType,
            int eventVersion,
            OffsetDateTime occurredAt,
            String producer,
            String aggregateId,
            Map<String, Object> payload) {
        return new DomainEventEnvelope(eventId, eventType, eventVersion, occurredAt, producer, aggregateId, payload);
    }

    private static void assertInvalido(String mensagem, Executable executable) {
        var exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals(mensagem, exception.getMessage());
    }
}
