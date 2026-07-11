package br.com.oficina.execution.core.entities.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PecaTest {
    private static final OffsetDateTime CRIADA_EM = OffsetDateTime.of(2026, 6, 23, 15, 30, 0, 0, ZoneOffset.UTC);
    private static final BigDecimal VALOR_INVALIDO = new BigDecimal("-1");

    @Test
    void deveCriarEAtualizarPeca() {
        var peca = new Peca(UUID.randomUUID(), "Filtro", "flt-001", new BigDecimal("35.50"), CRIADA_EM);

        assertEquals("Filtro", peca.nome());
        assertEquals("FLT-001", peca.codigo());
        assertEquals(new BigDecimal("35.50"), peca.valorUnitario());

        var atualizadaEm = CRIADA_EM.plusHours(1);
        peca.atualizar("Filtro de ar", "flt-002", new BigDecimal("45.00"), atualizadaEm);

        assertEquals("Filtro de ar", peca.nome());
        assertEquals("FLT-002", peca.codigo());
        assertEquals(new BigDecimal("45.00"), peca.valorUnitario());
        assertEquals(atualizadaEm, peca.atualizadoEm());
    }

    @Test
    void deveRejeitarPecaInvalida() {
        assertPecaInvalida(" ", "FLT-001", BigDecimal.ONE);
        assertPecaInvalida(null, "FLT-001", BigDecimal.ONE);
        assertPecaInvalida("Filtro", " ", BigDecimal.ONE);
        assertPecaInvalida("Filtro", null, BigDecimal.ONE);
        assertPecaInvalida("Filtro", "FLT-001", VALOR_INVALIDO);
        assertPecaInvalida("Filtro", "FLT-001", null);
    }

    private void assertPecaInvalida(String nome, String codigo, BigDecimal valorUnitario) {
        var pecaId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> new Peca(pecaId, nome, codigo, valorUnitario, CRIADA_EM));
    }
}
