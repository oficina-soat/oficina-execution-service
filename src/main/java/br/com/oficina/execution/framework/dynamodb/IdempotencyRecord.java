package br.com.oficina.execution.framework.dynamodb;

import java.time.OffsetDateTime;

public record IdempotencyRecord(
        String scope,
        String key,
        String requestHash,
        Integer responseStatus,
        String responseBody,
        ProcessingStatus processingStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime expiresAt) {

    public enum ProcessingStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public IdempotencyRecord {
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("Escopo de idempotencia e obrigatorio.");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Chave de idempotencia e obrigatoria.");
        }
        if (processingStatus == null) {
            throw new IllegalArgumentException("Status de processamento de idempotencia e obrigatorio.");
        }
        if (createdAt == null || updatedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("Datas de idempotencia sao obrigatorias.");
        }
    }
}
