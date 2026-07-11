package br.com.oficina.execution.core.usecases.estoque;

import br.com.oficina.execution.core.entities.estoque.MovimentoEstoque;
import br.com.oficina.execution.core.interfaces.gateway.EstoqueGateway;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ListarMovimentosEstoqueUseCase {
    private final EstoqueGateway estoqueGateway;

    public ListarMovimentosEstoqueUseCase(EstoqueGateway estoqueGateway) {
        this.estoqueGateway = estoqueGateway;
    }

    public CompletableFuture<List<MovimentoEstoque>> executar(Command command) {
        return estoqueGateway.listarMovimentos(command.pecaId(), command.ordemServicoId());
    }

    public record Command(UUID pecaId, UUID ordemServicoId) {
    }
}
