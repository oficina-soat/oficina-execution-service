package br.com.oficina.execution.core.usecases.execucao;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.interfaces.gateway.ExecucaoGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BuscarExecucaoDaOrdemServicoUseCase {
    private final ExecucaoGateway execucaoGateway;

    public BuscarExecucaoDaOrdemServicoUseCase(ExecucaoGateway execucaoGateway) {
        this.execucaoGateway = execucaoGateway;
    }

    public CompletableFuture<Execucao> executar(UUID ordemServicoId) {
        return execucaoGateway.buscarExecucaoDaOrdemServico(ordemServicoId);
    }
}
