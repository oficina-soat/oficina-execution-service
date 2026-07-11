package br.com.oficina.execution.core.usecases.status;

import java.util.concurrent.CompletableFuture;

public class ConsultarStatusUseCase {
    public CompletableFuture<Status> executar(Command command) {
        return CompletableFuture.completedFuture(new Status(command.service(), command.environment(), "UP"));
    }

    public record Command(String service, String environment) {
    }

    public record Status(String service, String environment, String status) {
    }
}
