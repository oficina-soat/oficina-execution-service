package br.com.oficina.execution.framework.web;

import br.com.oficina.execution.interfaces.controllers.StatusController;
import br.com.oficina.execution.interfaces.presenters.StatusPresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.view_model.StatusViewModel;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

@Path("/api/v1/status")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class StatusResource {
    private final StatusController statusController;
    private final StatusPresenterAdapter statusPresenter;

    @ConfigProperty(name = "quarkus.application.name")
    String applicationName;

    @ConfigProperty(name = "oficina.observability.deployment-environment")
    String environment;

    @Inject
    public StatusResource(StatusController statusController, StatusPresenterAdapter statusPresenter) {
        this.statusController = statusController;
        this.statusPresenter = statusPresenter;
    }

    @GET
    @PermitAll
    public Uni<StatusViewModel> status() {
        return statusViewModel();
    }

    @POST
    @PermitAll
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<StatusViewModel> mutatingStatusProbe() {
        return statusViewModel();
    }

    private Uni<StatusViewModel> statusViewModel() {
        var request = new StatusController.StatusRequest(applicationName, environment);
        return uni(statusController.status(request))
                .onItem().transform(status -> {
                    statusPresenter.present(status);
                    return statusPresenter.viewModel();
                });
    }

    private static <T> Uni<T> uni(CompletableFuture<T> future) {
        return Uni.createFrom().completionStage(future);
    }
}
