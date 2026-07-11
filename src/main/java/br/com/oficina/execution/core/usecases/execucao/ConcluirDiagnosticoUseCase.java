package br.com.oficina.execution.core.usecases.execucao;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.interfaces.gateway.ExecucaoGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConcluirDiagnosticoUseCase {
    private final ExecucaoGateway execucaoGateway;

    public ConcluirDiagnosticoUseCase(ExecucaoGateway execucaoGateway) {
        this.execucaoGateway = execucaoGateway;
    }

    public CompletableFuture<Execucao> executar(Command command) {
        return execucaoGateway.concluirDiagnostico(
                command.execucaoId(),
                command.diagnostico(),
                command.correlationId());
    }

    public record Command(UUID execucaoId, String diagnostico, String correlationId) {
    }
}
