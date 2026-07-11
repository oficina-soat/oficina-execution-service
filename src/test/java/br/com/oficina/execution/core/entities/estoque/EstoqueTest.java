package br.com.oficina.execution.core.entities.estoque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.oficina.execution.core.exceptions.BusinessConflictException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EstoqueTest {
    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 6, 23, 15, 30, 0, 0, ZoneOffset.UTC);

    @Test
    void deveRegistrarEntradaReservaConsumoEEstorno() {
        var pecaId = UUID.randomUUID();
        var ordemServicoId = UUID.randomUUID();
        var estoque = new Estoque(pecaId, 2, 0, AGORA);

        var entrada = estoque.registrar(TipoMovimentoEstoque.ENTRADA, 5, null, "Compra", AGORA.plusMinutes(1));
        assertEquals(TipoMovimentoEstoque.ENTRADA, entrada.tipo());
        assertEquals(7, estoque.quantidadeDisponivel());
        assertEquals(0, estoque.quantidadeReservada());

        estoque.registrar(TipoMovimentoEstoque.RESERVA, 3, ordemServicoId, "Reserva OS", AGORA.plusMinutes(2));
        assertEquals(4, estoque.quantidadeDisponivel());
        assertEquals(3, estoque.quantidadeReservada());

        estoque.registrar(TipoMovimentoEstoque.CONSUMO, 1, ordemServicoId, "Consumo OS", AGORA.plusMinutes(3));
        assertEquals(4, estoque.quantidadeDisponivel());
        assertEquals(2, estoque.quantidadeReservada());

        estoque.registrar(TipoMovimentoEstoque.ESTORNO, 1, ordemServicoId, "Estorno OS", AGORA.plusMinutes(4));
        assertEquals(5, estoque.quantidadeDisponivel());
        assertEquals(1, estoque.quantidadeReservada());
    }

    @Test
    void deveRejeitarQuantidadeInvalidaEOrdemAusente() {
        var estoque = new Estoque(UUID.randomUUID(), 1, 0, AGORA);

        assertRegistroInvalido(estoque, null, 1, null);
        assertRegistroInvalido(estoque, TipoMovimentoEstoque.ENTRADA, 0, null);
        assertRegistroInvalido(estoque, TipoMovimentoEstoque.RESERVA, 1, null);
        assertRegistroInvalido(estoque, TipoMovimentoEstoque.CONSUMO, 1, null);
        assertRegistroInvalido(estoque, TipoMovimentoEstoque.ESTORNO, 1, null);
    }

    @Test
    void deveRejeitarCriacaoDeEstoqueInvalido() {
        assertEstoqueInvalido(null, 1, 0);
        assertEstoqueInvalido(UUID.randomUUID(), -1, 0);
        assertEstoqueInvalido(UUID.randomUUID(), 1, -1);
    }

    @Test
    void deveRejeitarSaldoInsuficiente() {
        var estoque = new Estoque(UUID.randomUUID(), 1, 0, AGORA);
        var reservaOrdemServicoId = UUID.randomUUID();
        var consumoOrdemServicoId = UUID.randomUUID();

        assertThrows(BusinessConflictException.class,
                () -> estoque.registrar(TipoMovimentoEstoque.RESERVA, 2, reservaOrdemServicoId, null, AGORA));
        assertThrows(BusinessConflictException.class,
                () -> estoque.registrar(TipoMovimentoEstoque.CONSUMO, 1, consumoOrdemServicoId, null, AGORA));
    }

    private void assertRegistroInvalido(
            Estoque estoque,
            TipoMovimentoEstoque tipo,
            int quantidade,
            UUID ordemServicoId) {
        assertThrows(IllegalArgumentException.class,
                () -> estoque.registrar(tipo, quantidade, ordemServicoId, null, AGORA));
    }

    private void assertEstoqueInvalido(UUID pecaId, int quantidadeDisponivel, int quantidadeReservada) {
        assertThrows(IllegalArgumentException.class,
                () -> new Estoque(pecaId, quantidadeDisponivel, quantidadeReservada, AGORA));
    }
}
