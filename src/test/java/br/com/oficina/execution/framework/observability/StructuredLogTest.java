package br.com.oficina.execution.framework.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class StructuredLogTest {
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldExposeQueryableEventTypeAliases() {
        StructuredLog.withFields(Map.of("eventType", "diagnosticoIniciado"), () -> {
            assertEquals("diagnosticoIniciado", MDC.get("eventType"));
            assertEquals("diagnosticoIniciado", MDC.get("domainEventType"));
            assertEquals("diagnosticoIniciado", MDC.get("event.type"));
        });
    }
}
