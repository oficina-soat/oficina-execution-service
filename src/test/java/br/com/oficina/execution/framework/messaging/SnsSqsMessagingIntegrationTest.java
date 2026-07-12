package br.com.oficina.execution.framework.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
import br.com.oficina.execution.framework.dynamodb.DynamoDbLocalTestResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@QuarkusTest
@QuarkusTestResource(DynamoDbLocalTestResource.class)
@QuarkusTestResource(LocalStackMessagingTestResource.class)
class SnsSqsMessagingIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    @Inject
    DynamoDbExecutionStore store;

    @Inject
    OutboxPublisher outboxPublisher;

    @Inject
    SqsDomainEventConsumer sqsConsumer;

    @Inject
    MessagingRuntimeDependencies messagingDependencies;

    @Test
    void deveValidarTopicosProduzidosEFilasConsumidas() {
        assertDoesNotThrow(messagingDependencies::validar);
    }

    @Test
    void devePublicarOutboxNoSnsEEntregarNaFilaConsumidora() throws Exception {
        var ordemServicoId = UUID.randomUUID();
        var event = store.registrarOutbox(
                "diagnosticoIniciado",
                "oficina.execution.diagnostico-iniciado",
                ordemServicoId.toString(),
                Map.of("ordemServicoId", ordemServicoId.toString(), "execucaoId", UUID.randomUUID().toString()),
                "corr-execution-publish");

        var publicados = outboxPublisher.publicarPendentes();

        assertTrue(publicados.stream().anyMatch(candidate -> candidate.eventId().equals(event.eventId())));
        assertEquals("PUBLISHED", store.outboxEvents().stream()
                .filter(candidate -> candidate.eventId().equals(event.eventId()))
                .findFirst()
                .orElseThrow()
                .status()
                .name());

        try (var sqs = LocalStackMessagingTestResource.sqsClient()) {
            var queueUrl = queueUrl(sqs, "oficina.execution.diagnostico-iniciado", "oficina-os-service");
            var messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(2)
                    .build()).messages();
            assertEquals(1, messages.size());
            assertEquals("diagnosticoIniciado", JSON.readTree(messages.getFirst().body()).path("eventType").asText());
        }
    }

    @Test
    void deveMarcarFalhaQuandoEventoNaoForProduzidoPeloServico() {
        var event = store.registrarOutbox(
                "eventoNaoContratado",
                "oficina.execution.diagnostico-iniciado",
                UUID.randomUUID().toString(),
                Map.of("status", "INVALIDO"),
                "corr-execution-failure");

        var publicados = outboxPublisher.publicarPendentes();

        assertTrue(publicados.stream().noneMatch(candidate -> candidate.eventId().equals(event.eventId())));
        var failed = store.outboxEvents().stream()
                .filter(candidate -> candidate.eventId().equals(event.eventId()))
                .findFirst()
                .orElseThrow();
        assertEquals("FAILED", failed.status().name());
        assertEquals(1, failed.attempts());
        assertTrue(failed.lastError().contains("Evento nao produzido pelo oficina-execution-service"));
    }

    @Test
    void deveConsumirSqsEAckSomenteAposPersistirIdempotencia() throws Exception {
        var ordemServicoId = UUID.randomUUID();
        var eventId = UUID.randomUUID();
        var evento = new DomainEventEnvelope(
                eventId,
                "ordemDeServicoCriada",
                1,
                OffsetDateTime.now(ZoneOffset.UTC),
                "oficina-os-service",
                ordemServicoId.toString(),
                Map.of("ordemServicoId", ordemServicoId.toString()));

        var message = JSON.writeValueAsString(evento);
        try (var sns = LocalStackMessagingTestResource.snsClient()) {
            sns.publish(builder -> builder
                    .topicArn(LocalStackMessagingTestResource.topicArn("oficina.os.ordem-de-servico-criada"))
                    .message(message));
        }

        assertEquals(1, sqsConsumer.consumirDisponiveis());
        assertTrue(store.idempotenciaExiste("event-consumer", eventId.toString()));

        try (var sqs = LocalStackMessagingTestResource.sqsClient()) {
            var queueUrl = queueUrl(sqs, "oficina.os.ordem-de-servico-criada", "oficina-execution-service");
            var messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .waitTimeSeconds(1)
                    .build()).messages();
            assertTrue(messages.isEmpty());
        }
    }

    private static String queueUrl(software.amazon.awssdk.services.sqs.SqsClient sqs, String topic, String consumer) {
        return sqs.getQueueUrl(GetQueueUrlRequest.builder()
                .queueName(LocalStackMessagingTestResource.queueName(topic, consumer))
                .build()).queueUrl();
    }
}
