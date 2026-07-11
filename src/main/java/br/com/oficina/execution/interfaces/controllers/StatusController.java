package br.com.oficina.execution.interfaces.controllers;

import br.com.oficina.execution.core.usecases.status.ConsultarStatusUseCase;
import br.com.oficina.execution.core.usecases.status.ConsultarStatusUseCase.Status;
import java.util.concurrent.CompletableFuture;

public class StatusController {
    private final ConsultarStatusUseCase consultarStatusUseCase;

    public StatusController(ConsultarStatusUseCase consultarStatusUseCase) {
        this.consultarStatusUseCase = consultarStatusUseCase;
    }

    public CompletableFuture<Status> status(StatusRequest request) {
        return consultarStatusUseCase.executar(new ConsultarStatusUseCase.Command(
                request.service(),
                request.environment()));
    }

    public record StatusRequest(String service, String environment) {
    }
}
