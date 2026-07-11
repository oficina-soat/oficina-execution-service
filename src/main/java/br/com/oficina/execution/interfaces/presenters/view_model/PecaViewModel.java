package br.com.oficina.execution.interfaces.presenters.view_model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PecaViewModel(
        UUID pecaId,
        String nome,
        String codigo,
        BigDecimal valorUnitario,
        boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm) {
}
