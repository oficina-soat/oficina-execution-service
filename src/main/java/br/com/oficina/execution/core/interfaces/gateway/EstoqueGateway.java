package br.com.oficina.execution.core.interfaces.gateway;

import br.com.oficina.execution.core.entities.estoque.Estoque;
import br.com.oficina.execution.core.entities.estoque.MovimentoEstoque;
import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EstoqueGateway {
    CompletableFuture<Estoque> buscarSaldo(UUID pecaId);

    CompletableFuture<List<MovimentoEstoque>> listarMovimentos(UUID pecaId, UUID ordemServicoId);

    CompletableFuture<MovimentoEstoque> registrarMovimento(
            TipoMovimentoEstoque tipo,
            UUID pecaId,
            UUID ordemServicoId,
            int quantidade,
            String motivo,
            String correlationId);
}
