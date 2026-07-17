package br.com.oficina.execution.framework.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
import br.com.oficina.execution.framework.dynamodb.DynamoDbLocalTestSupport;
import br.com.oficina.execution.framework.dynamodb.DynamoDbTableNames;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

class ExecutionEventConsumerTest {
    private static GenericContainer<?> container;
    private static DynamoDbClient client;

    private DynamoDbExecutionStore store;
    private ExecutionEventConsumer consumer;

    @BeforeAll
    static void startDynamoDb() {
        container = DynamoDbLocalTestSupport.startContainer();
        client = DynamoDbLocalTestSupport.client(DynamoDbLocalTestSupport.endpoint(container));
    }

    @AfterAll
    static void stopDynamoDb() {
        if (client != null) {
            client.close();
        }
        if (container != null) {
            container.stop();
        }
    }

    @BeforeEach
    void setUp() {
        var tableNames = new DynamoDbTableNames("oficina-execution-event-" + UUID.randomUUID().toString().substring(0, 8));
        DynamoDbLocalTestSupport.createTables(client, tableNames);
        store = new DynamoDbExecutionStore(tableNames, client);
        consumer = new ExecutionEventConsumer(store);
    }

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
    void deveIniciarReparoEPublicarEventoAposAprovacaoDoOrcamento() {
        var ordemServicoId = UUID.randomUUID();
        var execucao = store.criarExecucaoSeAusente(ordemServicoId);
        store.iniciarDiagnostico(execucao.execucaoId(), "correlation-diagnostico");
        store.concluirDiagnostico(execucao.execucaoId(), "Diagnostico concluido", "correlation-diagnostico");
        var aprovacao = envelope(
                UUID.randomUUID(),
                "orcamentoAprovado",
                ordemServicoId,
                Map.of("ordemServicoId", ordemServicoId, "orcamentoId", UUID.randomUUID()));

        assertTrue(consumer.consumir(aprovacao));
        assertFalse(consumer.consumir(aprovacao));
        assertEquals(StatusExecucao.EM_REPARO, store.buscarExecucaoDaOrdemServico(ordemServicoId).status());
        assertEquals(1, store.outboxEvents().stream()
                .filter(event -> event.eventType().equals("execucaoIniciada"))
                .count());
    }

    @Test
    void deveAceitarAprovacaoAntesDaCriacaoSemDuplicarExecucao() {
        var ordemServicoId = UUID.randomUUID();
        var aprovacao = envelope(
                UUID.randomUUID(),
                "orcamentoAprovado",
                ordemServicoId,
                Map.of("ordemServicoId", ordemServicoId, "orcamentoId", UUID.randomUUID()));

        assertTrue(consumer.consumir(aprovacao));
        assertEquals(StatusExecucao.CRIADA, store.buscarExecucaoDaOrdemServico(ordemServicoId).status());
        assertEquals(0, store.outboxEvents().stream()
                .filter(event -> event.eventType().equals("execucaoIniciada"))
                .count());
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
