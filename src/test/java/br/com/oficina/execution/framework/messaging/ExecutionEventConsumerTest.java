package br.com.oficina.execution.framework.messaging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
import br.com.oficina.execution.framework.dynamodb.DynamoDbTableNames;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExecutionEventConsumerTest {
    private final DynamoDbExecutionStore store = new DynamoDbExecutionStore(new DynamoDbTableNames("oficina-execution-lab"));
    private final ExecutionEventConsumer consumer = new ExecutionEventConsumer(store);

    @Test
    void deveConsumirEventoCriandoExecucaoComIdempotencia() {
        var eventId = UUID.randomUUID();
        var ordemServicoId = UUID.randomUUID();
        var envelope = envelope(eventId, "ordemDeServicoCriada", ordemServicoId, Map.of("ordemServicoId", ordemServicoId));

        assertTrue(consumer.consumir(envelope));
        assertFalse(consumer.consumir(envelope));
        var execucao = store.buscarExecucaoDaOrdemServico(ordemServicoId);
        assertNotNull(execucao.execucaoId());
    }

    @Test
    void deveValidarCatalogoEmEventosDeItensDaOrdemDeServico() {
        assertTrue(consumer.consumir(envelope(
                UUID.randomUUID(),
                "pecaIncluidaNaOrdemDeServico",
                UUID.randomUUID(),
                Map.of("pecaId", DynamoDbExecutionStore.SEED_PECA_ID))));
        assertTrue(consumer.consumir(envelope(
                UUID.randomUUID(),
                "servicoIncluidoNaOrdemDeServico",
                UUID.randomUUID(),
                Map.of("servicoId", DynamoDbExecutionStore.SEED_SERVICO_ID))));
    }

    @Test
    void deveRejeitarEventoNaoContratadoParaConsumo() {
        var envelope = envelope(UUID.randomUUID(), "eventoInexistente", UUID.randomUUID(), Map.of());

        assertThrows(IllegalArgumentException.class, () -> consumer.consumir(envelope));
    }

    private DomainEventEnvelope envelope(UUID eventId, String eventType, UUID ordemServicoId, Map<String, Object> payload) {
        return new DomainEventEnvelope(
                eventId,
                eventType,
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "oficina-os-service",
                ordemServicoId.toString(),
                payload);
    }
}
