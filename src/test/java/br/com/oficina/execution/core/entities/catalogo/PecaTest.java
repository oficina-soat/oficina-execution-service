package br.com.oficina.execution.core.entities.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PecaTest {
    @Test
    void deveCriarEAtualizarPeca() {
        var criadaEm = OffsetDateTime.of(2026, 6, 23, 15, 30, 0, 0, ZoneOffset.UTC);
        var peca = new Peca(UUID.randomUUID(), "Filtro", "flt-001", new BigDecimal("35.50"), criadaEm);

        assertEquals("Filtro", peca.nome());
        assertEquals("FLT-001", peca.codigo());
        assertEquals(new BigDecimal("35.50"), peca.valorUnitario());

        var atualizadaEm = criadaEm.plusHours(1);
        peca.atualizar("Filtro de ar", "flt-002", new BigDecimal("45.00"), atualizadaEm);

        assertEquals("Filtro de ar", peca.nome());
        assertEquals("FLT-002", peca.codigo());
        assertEquals(new BigDecimal("45.00"), peca.valorUnitario());
        assertEquals(atualizadaEm, peca.atualizadoEm());
    }

    @Test
    void deveRejeitarPecaInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> new Peca(UUID.randomUUID(), " ", "FLT-001", BigDecimal.ONE, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Peca(UUID.randomUUID(), null, "FLT-001", BigDecimal.ONE, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Peca(UUID.randomUUID(), "Filtro", " ", BigDecimal.ONE, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Peca(UUID.randomUUID(), "Filtro", null, BigDecimal.ONE, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Peca(UUID.randomUUID(), "Filtro", "FLT-001", new BigDecimal("-1"), OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Peca(UUID.randomUUID(), "Filtro", "FLT-001", null, OffsetDateTime.now()));
    }
}
