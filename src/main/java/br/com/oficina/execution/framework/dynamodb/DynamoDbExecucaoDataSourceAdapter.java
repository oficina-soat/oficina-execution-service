package br.com.oficina.execution.framework.dynamodb;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import br.com.oficina.execution.core.interfaces.gateway.ExecucaoGateway;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@ApplicationScoped
public class DynamoDbExecucaoDataSourceAdapter implements ExecucaoGateway {
    private final DynamoDbExecutionStore store;

    public DynamoDbExecucaoDataSourceAdapter(DynamoDbExecutionStore store) {
        this.store = store;
    }

    @Override
    public CompletableFuture<Execucao> criarExecucao(UUID ordemServicoId, Integer prioridade) {
        return completed(() -> store.criarExecucao(ordemServicoId, prioridade));
    }

    @Override
    public CompletableFuture<List<Execucao>> listarExecucoes(StatusExecucao status) {
        return completed(() -> store.listarExecucoes(status));
    }

    @Override
    public CompletableFuture<List<Execucao>> listarFilaExecucao(StatusExecucao status) {
        return completed(() -> store.listarFilaExecucao(status));
    }

    @Override
    public CompletableFuture<Execucao> buscarExecucao(UUID execucaoId) {
        return completed(() -> store.buscarExecucao(execucaoId));
    }

    @Override
    public CompletableFuture<Execucao> buscarExecucaoDaOrdemServico(UUID ordemServicoId) {
        return completed(() -> store.buscarExecucaoDaOrdemServico(ordemServicoId));
    }

    @Override
    public CompletableFuture<Execucao> iniciarDiagnostico(UUID execucaoId, String correlationId) {
        return completed(() -> store.iniciarDiagnostico(execucaoId, correlationId));
    }

    @Override
    public CompletableFuture<Execucao> concluirDiagnostico(UUID execucaoId, String diagnostico, String correlationId) {
        return completed(() -> store.concluirDiagnostico(execucaoId, diagnostico, correlationId));
    }

    @Override
    public CompletableFuture<Execucao> iniciarReparo(UUID execucaoId, String correlationId) {
        return completed(() -> store.iniciarReparo(execucaoId, correlationId));
    }

    @Override
    public CompletableFuture<Execucao> concluirReparo(UUID execucaoId, String observacoes, String correlationId) {
        return completed(() -> store.concluirReparo(execucaoId, observacoes, correlationId));
    }

    @Override
    public CompletableFuture<Execucao> cancelarExecucao(UUID execucaoId, String motivo) {
        return completed(() -> store.cancelarExecucao(execucaoId, motivo));
    }

    private static <T> CompletableFuture<T> completed(Supplier<T> supplier) {
        try {
            return CompletableFuture.completedFuture(supplier.get());
        } catch (RuntimeException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }
}
