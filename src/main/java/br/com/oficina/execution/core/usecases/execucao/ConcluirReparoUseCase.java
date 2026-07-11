package br.com.oficina.execution.core.usecases.execucao;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.interfaces.gateway.ExecucaoGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConcluirReparoUseCase {
    private final ExecucaoGateway execucaoGateway;

    public ConcluirReparoUseCase(ExecucaoGateway execucaoGateway) {
        this.execucaoGateway = execucaoGateway;
    }

    public CompletableFuture<Execucao> executar(Command command) {
        return execucaoGateway.concluirReparo(
                command.execucaoId(),
                command.observacoes(),
                command.correlationId());
    }

    public record Command(UUID execucaoId, String observacoes, String correlationId) {
    }
}
