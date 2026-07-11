package br.com.oficina.execution.core.usecases.execucao;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.interfaces.gateway.ExecucaoGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CriarExecucaoUseCase {
    private final ExecucaoGateway execucaoGateway;

    public CriarExecucaoUseCase(ExecucaoGateway execucaoGateway) {
        this.execucaoGateway = execucaoGateway;
    }

    public CompletableFuture<Execucao> executar(Command command) {
        return execucaoGateway.criarExecucao(command.ordemServicoId(), command.prioridade());
    }

    public record Command(UUID ordemServicoId, Integer prioridade) {
    }
}
