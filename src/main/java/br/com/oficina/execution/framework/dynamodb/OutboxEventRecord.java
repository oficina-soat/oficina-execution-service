package br.com.oficina.execution.framework.dynamodb;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record OutboxEventRecord(
        UUID eventId,
        String eventType,
        int eventVersion,
        String topic,
        String producer,
        String aggregateId,
        Map<String, Object> payload,
        OutboxStatus status,
        int attempts,
        OffsetDateTime nextAttemptAt,
        OffsetDateTime publishedAt,
        OffsetDateTime expiresAt,
        String lastError,
        String correlationId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }

    public OutboxEventRecord {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId da Outbox e obrigatorio.");
        }
        if (isBlank(eventType)) {
            throw new IllegalArgumentException("eventType da Outbox e obrigatorio.");
        }
        if (eventVersion <= 0) {
            throw new IllegalArgumentException("eventVersion da Outbox deve ser positivo.");
        }
        if (isBlank(topic)) {
            throw new IllegalArgumentException("Topico da Outbox e obrigatorio.");
        }
        if (isBlank(producer)) {
            throw new IllegalArgumentException("Producer da Outbox e obrigatorio.");
        }
        if (isBlank(aggregateId)) {
            throw new IllegalArgumentException("aggregateId da Outbox e obrigatorio.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status da Outbox e obrigatorio.");
        }
        if (attempts < 0) {
            throw new IllegalArgumentException("Tentativas da Outbox nao podem ser negativas.");
        }
        if (isBlank(correlationId)) {
            throw new IllegalArgumentException("correlationId da Outbox e obrigatorio.");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("Datas da Outbox sao obrigatorias.");
        }
        payload = Map.copyOf(payload == null ? Map.of() : new LinkedHashMap<>(payload));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
