package br.com.oficina.execution.core.entities.estoque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MovimentoEstoqueTest {
    private static final OffsetDateTime CRIADO_EM = OffsetDateTime.parse("2026-06-23T15:30:00Z");

    @Test
    void deveNormalizarMotivoVazio() {
        var movimento = new MovimentoEstoque(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                TipoMovimentoEstoque.ENTRADA,
                1,
                " ",
                CRIADO_EM);

        assertNull(movimento.motivo());
    }

    @Test
    void deveReterDadosDoMovimento() {
        var movimentoId = UUID.randomUUID();
        var pecaId = UUID.randomUUID();
        var ordemServicoId = UUID.randomUUID();
        var criadoEm = CRIADO_EM;
        var movimento = new MovimentoEstoque(
                movimentoId,
                pecaId,
                ordemServicoId,
                TipoMovimentoEstoque.RESERVA,
                2,
                " Reserva ",
                criadoEm);

        assertEquals(movimentoId, movimento.movimentoId());
        assertEquals(pecaId, movimento.pecaId());
        assertEquals(ordemServicoId, movimento.ordemServicoId());
        assertEquals(TipoMovimentoEstoque.RESERVA, movimento.tipo());
        assertEquals(2, movimento.quantidade());
        assertEquals("Reserva", movimento.motivo());
        assertEquals(criadoEm, movimento.criadoEm());
    }

    @Test
    void deveRejeitarMovimentoInvalido() {
        assertMovimentoInvalido(null, UUID.randomUUID(), TipoMovimentoEstoque.ENTRADA, 1, CRIADO_EM);
        assertMovimentoInvalido(UUID.randomUUID(), null, TipoMovimentoEstoque.ENTRADA, 1, CRIADO_EM);
        assertMovimentoInvalido(UUID.randomUUID(), UUID.randomUUID(), null, 1, CRIADO_EM);
        assertMovimentoInvalido(UUID.randomUUID(), UUID.randomUUID(), TipoMovimentoEstoque.ENTRADA, 0, CRIADO_EM);
        assertMovimentoInvalido(UUID.randomUUID(), UUID.randomUUID(), TipoMovimentoEstoque.ENTRADA, 1, null);
    }

    private void assertMovimentoInvalido(
            UUID movimentoId,
            UUID pecaId,
            TipoMovimentoEstoque tipo,
            int quantidade,
            OffsetDateTime criadoEm) {
        assertThrows(IllegalArgumentException.class,
                () -> new MovimentoEstoque(movimentoId, pecaId, null, tipo, quantidade, null, criadoEm));
    }
}
