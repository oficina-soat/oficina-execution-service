package br.com.oficina.execution.framework.dynamodb;

import br.com.oficina.execution.core.entities.estoque.Estoque;
import br.com.oficina.execution.core.entities.estoque.MovimentoEstoque;
import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import br.com.oficina.execution.core.interfaces.gateway.EstoqueGateway;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@ApplicationScoped
public class DynamoDbEstoqueDataSourceAdapter implements EstoqueGateway {
    private final DynamoDbExecutionStore store;

    public DynamoDbEstoqueDataSourceAdapter(DynamoDbExecutionStore store) {
        this.store = store;
    }

    @Override
    public CompletableFuture<Estoque> buscarSaldo(UUID pecaId) {
        return completed(() -> store.buscarSaldo(pecaId));
    }

    @Override
    public CompletableFuture<List<MovimentoEstoque>> listarMovimentos(UUID pecaId, UUID ordemServicoId) {
        return completed(() -> store.listarMovimentos(pecaId, ordemServicoId));
    }

    @Override
    public CompletableFuture<MovimentoEstoque> registrarMovimento(
            TipoMovimentoEstoque tipo,
            UUID pecaId,
            UUID ordemServicoId,
            int quantidade,
            String motivo,
            String correlationId) {
        return completed(() -> store.registrarMovimento(tipo, pecaId, ordemServicoId, quantidade, motivo, correlationId));
    }

    private static <T> CompletableFuture<T> completed(Supplier<T> supplier) {
        try {
            return CompletableFuture.completedFuture(supplier.get());
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}
