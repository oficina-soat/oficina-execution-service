package br.com.oficina.execution.core.entities.estoque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.WebApplicationException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EstoqueTest {
    @Test
    void deveRegistrarEntradaReservaConsumoEEstorno() {
        var pecaId = UUID.randomUUID();
        var ordemServicoId = UUID.randomUUID();
        var agora = OffsetDateTime.of(2026, 6, 23, 15, 30, 0, 0, ZoneOffset.UTC);
        var estoque = new Estoque(pecaId, 2, 0, agora);

        var entrada = estoque.registrar(TipoMovimentoEstoque.ENTRADA, 5, null, "Compra", agora.plusMinutes(1));
        assertEquals(TipoMovimentoEstoque.ENTRADA, entrada.tipo());
        assertEquals(7, estoque.quantidadeDisponivel());
        assertEquals(0, estoque.quantidadeReservada());

        estoque.registrar(TipoMovimentoEstoque.RESERVA, 3, ordemServicoId, "Reserva OS", agora.plusMinutes(2));
        assertEquals(4, estoque.quantidadeDisponivel());
        assertEquals(3, estoque.quantidadeReservada());

        estoque.registrar(TipoMovimentoEstoque.CONSUMO, 1, ordemServicoId, "Consumo OS", agora.plusMinutes(3));
        assertEquals(4, estoque.quantidadeDisponivel());
        assertEquals(2, estoque.quantidadeReservada());

        estoque.registrar(TipoMovimentoEstoque.ESTORNO, 1, ordemServicoId, "Estorno OS", agora.plusMinutes(4));
        assertEquals(5, estoque.quantidadeDisponivel());
        assertEquals(1, estoque.quantidadeReservada());
    }

    @Test
    void deveRejeitarQuantidadeInvalidaEOrdemAusente() {
        var estoque = new Estoque(UUID.randomUUID(), 1, 0, OffsetDateTime.now());

        assertThrows(IllegalArgumentException.class,
                () -> estoque.registrar(null, 1, null, null, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> estoque.registrar(TipoMovimentoEstoque.ENTRADA, 0, null, null, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> estoque.registrar(TipoMovimentoEstoque.RESERVA, 1, null, null, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> estoque.registrar(TipoMovimentoEstoque.CONSUMO, 1, null, null, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> estoque.registrar(TipoMovimentoEstoque.ESTORNO, 1, null, null, OffsetDateTime.now()));
    }

    @Test
    void deveRejeitarCriacaoDeEstoqueInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Estoque(null, 1, 0, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Estoque(UUID.randomUUID(), -1, 0, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Estoque(UUID.randomUUID(), 1, -1, OffsetDateTime.now()));
    }

    @Test
    void deveRejeitarSaldoInsuficiente() {
        var estoque = new Estoque(UUID.randomUUID(), 1, 0, OffsetDateTime.now());

        assertThrows(WebApplicationException.class,
                () -> estoque.registrar(TipoMovimentoEstoque.RESERVA, 2, UUID.randomUUID(), null, OffsetDateTime.now()));
        assertThrows(WebApplicationException.class,
                () -> estoque.registrar(TipoMovimentoEstoque.CONSUMO, 1, UUID.randomUUID(), null, OffsetDateTime.now()));
    }
}
