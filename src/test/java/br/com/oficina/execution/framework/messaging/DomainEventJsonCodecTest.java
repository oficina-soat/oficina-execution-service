package br.com.oficina.execution.framework.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.oficina.execution.framework.dynamodb.OutboxEventRecord;
import br.com.oficina.execution.framework.dynamodb.OutboxEventRecord.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainEventJsonCodecTest {
    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 7, 12, 10, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void deveCodificarRegistroDeOutboxComoEnvelopeDeDominio() {
        var codec = codec();
        var event = outbox();

        var decoded = codec.decode(codec.encode(event));

        assertEquals(event.eventId(), decoded.eventId());
        assertEquals(event.eventType(), decoded.eventType());
        assertEquals(event.eventVersion(), decoded.eventVersion());
        assertEquals(event.producer(), decoded.producer());
        assertEquals(event.aggregateId(), decoded.aggregateId());
        assertEquals("INICIADO", decoded.payload().get("status"));
    }

    @Test
    void deveDecodificarEnvelopeTextualDoSns() throws Exception {
        var codec = codec();
        var encoded = codec.encode(outbox());
        var snsEnvelope = new ObjectMapper().writeValueAsString(Map.of("Message", encoded));

        var decoded = codec.decode(snsEnvelope);

        assertEquals("diagnosticoIniciado", decoded.eventType());
    }

    @Test
    void deveRejeitarMensagemInvalida() {
        var codec = codec();

        assertThrows(IllegalArgumentException.class, () -> codec.decode("{"));
    }

    private static DomainEventJsonCodec codec() {
        return new DomainEventJsonCodec(new ObjectMapper().findAndRegisterModules());
    }

    private static OutboxEventRecord outbox() {
        return new OutboxEventRecord(
                UUID.randomUUID(),
                "diagnosticoIniciado",
                1,
                "oficina.execution.diagnostico-iniciado",
                "oficina-execution-service",
                UUID.randomUUID().toString(),
                Map.of("status", "INICIADO"),
                OutboxStatus.PENDING,
                0,
                NOW,
                null,
                null,
                null,
                "corr-execution-codec",
                NOW,
                NOW);
    }
}
