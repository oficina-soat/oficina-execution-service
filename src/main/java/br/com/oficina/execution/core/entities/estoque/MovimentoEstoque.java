package br.com.oficina.execution.core.entities.estoque;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MovimentoEstoque(
        UUID movimentoId,
        UUID pecaId,
        UUID ordemServicoId,
        TipoMovimentoEstoque tipo,
        int quantidade,
        String motivo,
        OffsetDateTime criadoEm) {

    public MovimentoEstoque {
        if (movimentoId == null) {
            throw new IllegalArgumentException("Identificador do movimento e obrigatorio.");
        }
        if (pecaId == null) {
            throw new IllegalArgumentException("Identificador da peca e obrigatorio.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo do movimento de estoque e obrigatorio.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }
        if (criadoEm == null) {
            throw new IllegalArgumentException("Data do movimento e obrigatoria.");
        }
        motivo = motivo == null || motivo.isBlank() ? null : motivo.trim();
    }
}
