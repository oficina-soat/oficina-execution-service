package br.com.oficina.execution.framework.messaging;

import br.com.oficina.execution.core.exceptions.ResourceNotFoundException;
import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
import br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus;
import br.com.oficina.execution.framework.observability.StructuredLog;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ExecutionEventConsumer {
    private static final Logger LOG = Logger.getLogger(ExecutionEventConsumer.class);
    private static final String SCOPE = "event-consumer";
    private static final String CONSUMER = "oficina-execution-service";
    private static final Set<String> EVENTOS_CONSUMIDOS = Set.of(
            "ordemDeServicoCriada",
            "pecaIncluidaNaOrdemDeServico",
            "servicoIncluidoNaOrdemDeServico",
            "orcamentoAprovado",
            "orcamentoRecusado",
            "ordemDeServicoFinalizada",
            "sagaCompensada",
            "sagaFinalizadaComSucesso");

    private final DynamoDbExecutionStore store;

    public ExecutionEventConsumer(DynamoDbExecutionStore store) {
        this.store = store;
    }

    public boolean consumir(DomainEventEnvelope envelope) {
        if (!EVENTOS_CONSUMIDOS.contains(envelope.eventType())) {
            throw new IllegalArgumentException("Evento nao consumido pelo oficina-execution-service: " + envelope.eventType());
        }
        var key = envelope.eventId().toString();
        var correlationId = correlationId(envelope);
        var existing = store.buscarIdempotencia(SCOPE, key);
        if (existing.isPresent() && existing.get().processingStatus() == ProcessingStatus.COMPLETED) {
            logEvent("domain event ignored", envelope, "DUPLICATE", correlationId);
            return false;
        }
        try {
            StructuredLog.withFields(eventFields(envelope, "PROCESSING", correlationId), () -> {
                store.registrarIdempotencia(SCOPE, key, envelope.eventType(), null, null, ProcessingStatus.PROCESSING);
                aplicarEvento(envelope);
                store.registrarIdempotencia(SCOPE, key, envelope.eventType(), null, null, ProcessingStatus.COMPLETED);
            });
        } catch (RuntimeException exception) {
            store.registrarIdempotencia(SCOPE, key, envelope.eventType(), null, null, ProcessingStatus.FAILED_RETRYABLE);
            throw exception;
        }
        logEvent("domain event consumed", envelope, "CONSUMED", correlationId);
        return true;
    }

    private void aplicarEvento(DomainEventEnvelope envelope) {
        switch (envelope.eventType()) {
            case "ordemDeServicoCriada" -> store.criarExecucaoSeAusente(ordemServicoId(envelope));
            case "orcamentoAprovado" -> store.iniciarReparoAposAprovacao(
                    ordemServicoId(envelope), correlationId(envelope));
            case "orcamentoRecusado" -> store.retomarDiagnosticoAposRecusa(ordemServicoId(envelope));
            case "pecaIncluidaNaOrdemDeServico" -> store.buscarPeca(uuidPayload(envelope, "peca", "pecaId"));
            case "servicoIncluidoNaOrdemDeServico" -> store.buscarServico(uuidPayload(envelope, "servico", "servicoId"));
            case "sagaCompensada" -> cancelarSeExistir(envelope);
            case "ordemDeServicoFinalizada", "sagaFinalizadaComSucesso" -> {
                // Eventos de fechamento sao registrados para idempotencia e auditoria local.
            }
            default -> throw new IllegalArgumentException("Evento nao suportado: " + envelope.eventType());
        }
    }

    private void cancelarSeExistir(DomainEventEnvelope envelope) {
        try {
            var execucao = store.buscarExecucaoDaOrdemServico(ordemServicoId(envelope));
            store.cancelarExecucao(execucao.execucaoId(), "Saga compensada");
        } catch (ResourceNotFoundException _) {
            // A compensacao pode chegar antes de existir contexto local de execucao.
        }
    }

    private UUID ordemServicoId(DomainEventEnvelope envelope) {
        var payloadValue = envelope.payload().get("ordemServicoId");
        return payloadValue == null ? UUID.fromString(envelope.aggregateId()) : toUuid(payloadValue);
    }

    private UUID uuidPayload(DomainEventEnvelope envelope, String objectField, String field) {
        var nested = envelope.payload().get(objectField);
        var value = nested instanceof Map<?, ?> values ? values.get(field) : null;
        if (value == null) {
            throw new IllegalArgumentException(field + " e obrigatorio no payload do evento " + envelope.eventType() + ".");
        }
        return toUuid(value);
    }

    private UUID toUuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    private void logEvent(String message, DomainEventEnvelope envelope, String messageStatus, String correlationId) {
        StructuredLog.info(LOG, message, eventFields(envelope, messageStatus, correlationId));
    }

    private Map<String, Object> eventFields(DomainEventEnvelope envelope, String messageStatus, String correlationId) {
        return Map.of(
                "correlationId", correlationId,
                "eventId", envelope.eventId().toString(),
                "eventType", envelope.eventType(),
                "eventVersion", envelope.eventVersion(),
                "producer", envelope.producer(),
                "consumer", CONSUMER,
                "aggregateId", envelope.aggregateId(),
                "messageStatus", messageStatus);
    }

    private String correlationId(DomainEventEnvelope envelope) {
        return envelope.correlationId() == null || envelope.correlationId().isBlank()
                ? envelope.eventId().toString()
                : envelope.correlationId();
    }
}
