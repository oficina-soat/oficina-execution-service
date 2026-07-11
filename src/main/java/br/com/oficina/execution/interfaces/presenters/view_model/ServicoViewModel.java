package br.com.oficina.execution.interfaces.presenters.view_model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServicoViewModel(
        UUID servicoId,
        String nome,
        String descricao,
        BigDecimal valorBase,
        boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {
}
