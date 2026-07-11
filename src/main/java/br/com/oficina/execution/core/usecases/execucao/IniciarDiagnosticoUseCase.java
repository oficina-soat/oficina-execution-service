package br.com.oficina.execution.core.usecases.execucao;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.interfaces.gateway.ExecucaoGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class IniciarDiagnosticoUseCase {
    private final ExecucaoGateway execucaoGateway;

    public IniciarDiagnosticoUseCase(ExecucaoGateway execucaoGateway) {
        this.execucaoGateway = execucaoGateway;
    }

    public CompletableFuture<Execucao> executar(Command command) {
        return execucaoGateway.iniciarDiagnostico(command.execucaoId(), command.correlationId());
    }

    public record Command(UUID execucaoId, String correlationId) {
    }
}
