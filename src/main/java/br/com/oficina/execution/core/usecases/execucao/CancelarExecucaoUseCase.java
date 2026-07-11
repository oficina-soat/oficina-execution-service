package br.com.oficina.execution.core.usecases.execucao;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.interfaces.gateway.ExecucaoGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CancelarExecucaoUseCase {
    private final ExecucaoGateway execucaoGateway;

    public CancelarExecucaoUseCase(ExecucaoGateway execucaoGateway) {
        this.execucaoGateway = execucaoGateway;
    }

    public CompletableFuture<Execucao> executar(Command command) {
        return execucaoGateway.cancelarExecucao(command.execucaoId(), command.motivo());
    }

    public record Command(UUID execucaoId, String motivo) {
    }
}
