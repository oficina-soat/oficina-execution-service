package br.com.oficina.execution.framework.dynamodb;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.core.entities.catalogo.Servico;
import br.com.oficina.execution.core.interfaces.gateway.CatalogoGateway;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@ApplicationScoped
public class DynamoDbCatalogoDataSourceAdapter implements CatalogoGateway {
    private final DynamoDbExecutionStore store;

    public DynamoDbCatalogoDataSourceAdapter(DynamoDbExecutionStore store) {
        this.store = store;
    }

    @Override
    public CompletableFuture<Servico> criarServico(String nome, String descricao, BigDecimal valorBase) {
        return completed(() -> store.criarServico(nome, descricao, valorBase));
    }

    @Override
    public CompletableFuture<List<Servico>> listarServicos() {
        return completed(store::listarServicos);
    }

    @Override
    public CompletableFuture<Servico> buscarServico(UUID servicoId) {
        return completed(() -> store.buscarServico(servicoId));
    }

    @Override
    public CompletableFuture<Servico> atualizarServico(UUID servicoId, String nome, String descricao, BigDecimal valorBase) {
        return completed(() -> store.atualizarServico(servicoId, nome, descricao, valorBase));
    }

    @Override
    public CompletableFuture<Peca> criarPeca(String nome, String codigo, BigDecimal valorUnitario) {
        return completed(() -> store.criarPeca(nome, codigo, valorUnitario));
    }

    @Override
    public CompletableFuture<List<Peca>> listarPecas() {
        return completed(store::listarPecas);
    }

    @Override
    public CompletableFuture<Peca> buscarPeca(UUID pecaId) {
        return completed(() -> store.buscarPeca(pecaId));
    }

    @Override
    public CompletableFuture<Peca> atualizarPeca(UUID pecaId, String nome, String codigo, BigDecimal valorUnitario) {
        return completed(() -> store.atualizarPeca(pecaId, nome, codigo, valorUnitario));
    }

    private static <T> CompletableFuture<T> completed(Supplier<T> supplier) {
        try {
            return CompletableFuture.completedFuture(supplier.get());
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}
