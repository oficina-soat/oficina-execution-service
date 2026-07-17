package br.com.oficina.execution.framework.web;

import br.com.oficina.execution.core.usecases.dashboard.ConsultarDashboardExecucaoUseCase;
import br.com.oficina.execution.core.usecases.dashboard.ConsultarDashboardExecucaoUseCase.DashboardExecucao;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/dashboard/execucao")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"administrativo", "mecanico"})
@Blocking
public class DashboardResource {
    private final ConsultarDashboardExecucaoUseCase useCase;

    @Inject
    public DashboardResource(ConsultarDashboardExecucaoUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public Uni<DashboardExecucao> consultar() {
        return Uni.createFrom().completionStage(useCase.executar());
    }
}
