package br.com.oficina.execution.core.entities.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServicoTest {
    private static final OffsetDateTime CRIADA_EM = OffsetDateTime.of(2026, 6, 23, 15, 30, 0, 0, ZoneOffset.UTC);
    private static final BigDecimal VALOR_INVALIDO = new BigDecimal("-1");

    @Test
    void deveCriarEAtualizarServico() {
        var servico = new Servico(UUID.randomUUID(), "Troca de oleo", "Substituicao", new BigDecimal("250.00"), CRIADA_EM);

        assertEquals("Troca de oleo", servico.nome());
        assertEquals("Substituicao", servico.descricao());
        assertEquals(new BigDecimal("250.00"), servico.valorBase());

        var atualizadaEm = CRIADA_EM.plusHours(1);
        servico.atualizar("Alinhamento", " ", new BigDecimal("150.00"), atualizadaEm);

        assertEquals("Alinhamento", servico.nome());
        assertNull(servico.descricao());
        assertEquals(new BigDecimal("150.00"), servico.valorBase());
        assertEquals(atualizadaEm, servico.atualizadoEm());
    }

    @Test
    void deveRejeitarServicoInvalido() {
        assertServicoInvalido(" ", BigDecimal.ONE);
        assertServicoInvalido(null, BigDecimal.ONE);
        assertServicoInvalido("Troca", VALOR_INVALIDO);
        assertServicoInvalido("Troca", null);
    }

    private void assertServicoInvalido(String nome, BigDecimal valorBase) {
        var servicoId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> new Servico(servicoId, nome, null, valorBase, CRIADA_EM));
    }
}
