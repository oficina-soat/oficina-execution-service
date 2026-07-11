package br.com.oficina.execution.interfaces.presenters.view_model;

import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MovimentoEstoqueViewModel(
        UUID movimentoId,
        UUID pecaId,
        UUID ordemServicoId,
        TipoMovimentoEstoque tipo,
        int quantidade,
        String motivo,
        OffsetDateTime criadoEm) {
}
