package br.com.oficina.execution.framework.messaging;

import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
import br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ExecutionEventConsumer {
    private static final String SCOPE = "event-consumer";
    private static final Set<String> EVENTOS_CONSUMIDOS = Set.of(
            "ordemDeServicoCriada",
            "pecaIncluidaNaOrdemDeServico",
            "servicoIncluidoNaOrdemDeServico",
            "orcamentoAprovado",
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
        if (store.idempotenciaExiste(SCOPE, key)) {
            return false;
        }
        store.registrarIdempotencia(SCOPE, key, envelope.eventType(), null, null, ProcessingStatus.PROCESSING);
        aplicarEvento(envelope);
        store.registrarIdempotencia(SCOPE, key, envelope.eventType(), null, null, ProcessingStatus.COMPLETED);
        return true;
    }

    private void aplicarEvento(DomainEventEnvelope envelope) {
        switch (envelope.eventType()) {
            case "ordemDeServicoCriada", "orcamentoAprovado" -> store.criarExecucaoSeAusente(ordemServicoId(envelope));
            case "pecaIncluidaNaOrdemDeServico" -> store.buscarPeca(uuidPayload(envelope, "pecaId"));
            case "servicoIncluidoNaOrdemDeServico" -> store.buscarServico(uuidPayload(envelope, "servicoId"));
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
        } catch (jakarta.ws.rs.NotFoundException ignored) {
            // A compensacao pode chegar antes de existir contexto local de execucao.
        }
    }

    private UUID ordemServicoId(DomainEventEnvelope envelope) {
        var payloadValue = envelope.payload().get("ordemServicoId");
        return payloadValue == null ? UUID.fromString(envelope.aggregateId()) : toUuid(payloadValue);
    }

    private UUID uuidPayload(DomainEventEnvelope envelope, String field) {
        var value = envelope.payload().get(field);
        if (value == null) {
            throw new IllegalArgumentException(field + " e obrigatorio no payload do evento " + envelope.eventType() + ".");
        }
        return toUuid(value);
    }

    private UUID toUuid(Object value) {
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }
}
