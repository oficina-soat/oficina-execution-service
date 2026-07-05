package br.com.oficina.execution.core.entities.estoque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MovimentoEstoqueTest {
    @Test
    void deveNormalizarMotivoVazio() {
        var movimento = new MovimentoEstoque(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                TipoMovimentoEstoque.ENTRADA,
                1,
                " ",
                OffsetDateTime.now());

        assertNull(movimento.motivo());
    }

    @Test
    void deveReterDadosDoMovimento() {
        var movimentoId = UUID.randomUUID();
        var pecaId = UUID.randomUUID();
        var ordemServicoId = UUID.randomUUID();
        var criadoEm = OffsetDateTime.now();
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
        assertThrows(IllegalArgumentException.class,
                () -> new MovimentoEstoque(null, UUID.randomUUID(), null, TipoMovimentoEstoque.ENTRADA, 1, null, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new MovimentoEstoque(UUID.randomUUID(), null, null, TipoMovimentoEstoque.ENTRADA, 1, null, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new MovimentoEstoque(UUID.randomUUID(), UUID.randomUUID(), null, null, 1, null, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new MovimentoEstoque(UUID.randomUUID(), UUID.randomUUID(), null, TipoMovimentoEstoque.ENTRADA, 0, null, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new MovimentoEstoque(UUID.randomUUID(), UUID.randomUUID(), null, TipoMovimentoEstoque.ENTRADA, 1, null, null));
    }
}
