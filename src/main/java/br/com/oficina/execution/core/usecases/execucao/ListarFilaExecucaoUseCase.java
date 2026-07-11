package br.com.oficina.execution.core.usecases.execucao;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import br.com.oficina.execution.core.interfaces.gateway.ExecucaoGateway;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ListarFilaExecucaoUseCase {
    private final ExecucaoGateway execucaoGateway;

    public ListarFilaExecucaoUseCase(ExecucaoGateway execucaoGateway) {
        this.execucaoGateway = execucaoGateway;
    }

    public CompletableFuture<List<Execucao>> executar(StatusExecucao status) {
        return execucaoGateway.listarFilaExecucao(status);
    }
}
