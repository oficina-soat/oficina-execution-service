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
    @Test
    void deveCriarEAtualizarServico() {
        var criadaEm = OffsetDateTime.of(2026, 6, 23, 15, 30, 0, 0, ZoneOffset.UTC);
        var servico = new Servico(UUID.randomUUID(), "Troca de oleo", "Substituicao", new BigDecimal("250.00"), criadaEm);

        assertEquals("Troca de oleo", servico.nome());
        assertEquals("Substituicao", servico.descricao());
        assertEquals(new BigDecimal("250.00"), servico.valorBase());

        var atualizadaEm = criadaEm.plusHours(1);
        servico.atualizar("Alinhamento", " ", new BigDecimal("150.00"), atualizadaEm);

        assertEquals("Alinhamento", servico.nome());
        assertNull(servico.descricao());
        assertEquals(new BigDecimal("150.00"), servico.valorBase());
        assertEquals(atualizadaEm, servico.atualizadoEm());
    }

    @Test
    void deveRejeitarServicoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Servico(UUID.randomUUID(), " ", null, BigDecimal.ONE, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Servico(UUID.randomUUID(), null, null, BigDecimal.ONE, OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Servico(UUID.randomUUID(), "Troca", null, new BigDecimal("-1"), OffsetDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new Servico(UUID.randomUUID(), "Troca", null, null, OffsetDateTime.now()));
    }
}
