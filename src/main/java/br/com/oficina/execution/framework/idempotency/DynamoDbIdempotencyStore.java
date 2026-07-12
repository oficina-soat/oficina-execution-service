package br.com.oficina.execution.framework.idempotency;

import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore.IdempotencyWrite;
import br.com.oficina.execution.framework.idempotency.IdempotencyRecord.ProcessingStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.util.Optional;

@ApplicationScoped
public class DynamoDbIdempotencyStore implements IdempotencyStore {
    private final DynamoDbExecutionStore store;

    public DynamoDbIdempotencyStore(DynamoDbExecutionStore store) {
        this.store = store;
    }

    @Override
    public Optional<IdempotencyRecord> find(String scope, String key) {
        return store.buscarIdempotencia(scope, key).map(this::toRecord);
    }

    @Override
    public IdempotencyRecord createProcessing(
            String scope,
            String key,
            String requestHash,
            String correlationId,
            String requestId,
            OffsetDateTime expiresAt) {
        return toRecord(store.registrarIdempotencia(new IdempotencyWrite(
                scope,
                key,
                requestHash,
                null,
                null,
                toDynamoStatus(ProcessingStatus.PROCESSING),
                correlationId,
                requestId,
                expiresAt)));
    }

    @Override
    public void complete(
            String scope,
            String key,
            ProcessingStatus processingStatus,
            int responseStatus,
            String responseBody) {
        store.concluirIdempotencia(
                scope,
                key,
                toDynamoStatus(processingStatus),
                responseStatus,
                responseBody);
    }

    private IdempotencyRecord toRecord(br.com.oficina.execution.framework.dynamodb.IdempotencyRecord persistedRecord) {
        return new IdempotencyRecord(
                persistedRecord.scope(),
                persistedRecord.key(),
                persistedRecord.requestHash(),
                toWebStatus(persistedRecord.processingStatus()),
                persistedRecord.responseStatus(),
                persistedRecord.responseBody(),
                persistedRecord.correlationId(),
                persistedRecord.requestId(),
                persistedRecord.createdAt(),
                persistedRecord.updatedAt(),
                persistedRecord.expiresAt());
    }

    private br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus toDynamoStatus(
            ProcessingStatus status) {
        return switch (status) {
            case PROCESSING -> br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus.PROCESSING;
            case COMPLETED -> br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus.COMPLETED;
            case FAILED_RETRYABLE -> br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus.FAILED_RETRYABLE;
            case FAILED_FINAL -> br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus.FAILED_FINAL;
        };
    }

    private ProcessingStatus toWebStatus(
            br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus status) {
        return switch (status) {
            case PROCESSING -> ProcessingStatus.PROCESSING;
            case COMPLETED -> ProcessingStatus.COMPLETED;
            case FAILED, FAILED_RETRYABLE -> ProcessingStatus.FAILED_RETRYABLE;
            case FAILED_FINAL -> ProcessingStatus.FAILED_FINAL;
        };
    }
}
