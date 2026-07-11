package br.com.oficina.execution.core.usecases.estoque;

import br.com.oficina.execution.core.entities.estoque.MovimentoEstoque;
import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import br.com.oficina.execution.core.interfaces.gateway.EstoqueGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RegistrarMovimentoEstoqueUseCase {
    private final EstoqueGateway estoqueGateway;

    public RegistrarMovimentoEstoqueUseCase(EstoqueGateway estoqueGateway) {
        this.estoqueGateway = estoqueGateway;
    }

    public CompletableFuture<MovimentoEstoque> executar(Command command) {
        return estoqueGateway.registrarMovimento(
                command.tipo(),
                command.pecaId(),
                command.ordemServicoId(),
                command.quantidade(),
                command.motivo(),
                command.correlationId());
    }

    public record Command(
            TipoMovimentoEstoque tipo,
            UUID pecaId,
            UUID ordemServicoId,
            int quantidade,
            String motivo,
            String correlationId) {
    }
}
