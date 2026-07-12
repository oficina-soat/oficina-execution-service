package br.com.oficina.execution.framework.web;

import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import br.com.oficina.execution.interfaces.controllers.ExecucoesController;
import br.com.oficina.execution.interfaces.presenters.ExecucaoPresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.FilaExecucaoPresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.view_model.ExecucaoViewModel;
import br.com.oficina.execution.interfaces.presenters.view_model.FilaExecucaoItemViewModel;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

@Path("/api/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Blocking
public class ExecucoesResource {
    private final ExecucoesController execucoesController;
    private final ExecucaoPresenterAdapter execucaoPresenter;
    private final FilaExecucaoPresenterAdapter filaPresenter;

    @Inject
    public ExecucoesResource(
            ExecucoesController execucoesController,
            ExecucaoPresenterAdapter execucaoPresenter,
            FilaExecucaoPresenterAdapter filaPresenter) {
        this.execucoesController = execucoesController;
        this.execucaoPresenter = execucaoPresenter;
        this.filaPresenter = filaPresenter;
    }

    @POST
    @Path("execucoes")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> criarExecucao(ExecucoesController.ExecucaoCreateRequest request) {
        return uni(execucoesController.criarExecucao(request))
                .onItem().transform(execucao -> {
                    execucaoPresenter.present(execucao);
                    var viewModel = execucaoPresenter.viewModel();
                    return Response.created(URI.create("/api/v1/execucoes/" + viewModel.execucaoId()))
                            .entity(viewModel)
                            .build();
                });
    }

    @GET
    @Path("execucoes")
    public Uni<List<ExecucaoViewModel>> consultarExecucoes(@QueryParam("status") StatusExecucao status) {
        return uni(execucoesController.consultarExecucoes(status))
                .onItem().transform(execucoes -> {
                    execucaoPresenter.present(execucoes);
                    return execucaoPresenter.viewModels();
                });
    }

    @GET
    @Path("execucoes/fila")
    public Uni<List<FilaExecucaoItemViewModel>> consultarFilaExecucao(@QueryParam("status") StatusExecucao status) {
        return uni(execucoesController.consultarFilaExecucao(status))
                .onItem().transform(execucoes -> {
                    filaPresenter.present(execucoes);
                    return filaPresenter.viewModels();
                });
    }

    @GET
    @Path("execucoes/{execucaoId}")
    public Uni<ExecucaoViewModel> consultarExecucao(@PathParam("execucaoId") UUID execucaoId) {
        return uni(execucoesController.consultarExecucao(execucaoId))
                .onItem().transform(execucao -> {
                    execucaoPresenter.present(execucao);
                    return execucaoPresenter.viewModel();
                });
    }

    @GET
    @Path("ordens-servico/{ordemServicoId}/execucao")
    public Uni<ExecucaoViewModel> consultarExecucaoDaOrdemServico(@PathParam("ordemServicoId") UUID ordemServicoId) {
        return uni(execucoesController.consultarExecucaoDaOrdemServico(ordemServicoId))
                .onItem().transform(execucao -> {
                    execucaoPresenter.present(execucao);
                    return execucaoPresenter.viewModel();
                });
    }

    @POST
    @Path("execucoes/{execucaoId}/diagnostico/inicio")
    @Consumes(MediaType.WILDCARD)
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<ExecucaoViewModel> iniciarDiagnostico(
            @PathParam("execucaoId") UUID execucaoId,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return uni(execucoesController.iniciarDiagnostico(execucaoId, correlationId))
                .onItem().transform(execucao -> {
                    execucaoPresenter.present(execucao);
                    return execucaoPresenter.viewModel();
                });
    }

    @POST
    @Path("execucoes/{execucaoId}/diagnostico/conclusao")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<ExecucaoViewModel> concluirDiagnostico(
            @PathParam("execucaoId") UUID execucaoId,
            ExecucoesController.ConclusaoDiagnosticoRequest request,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return uni(execucoesController.concluirDiagnostico(execucaoId, request, correlationId))
                .onItem().transform(execucao -> {
                    execucaoPresenter.present(execucao);
                    return execucaoPresenter.viewModel();
                });
    }

    @POST
    @Path("execucoes/{execucaoId}/reparo/inicio")
    @Consumes(MediaType.WILDCARD)
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<ExecucaoViewModel> iniciarReparo(
            @PathParam("execucaoId") UUID execucaoId,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return uni(execucoesController.iniciarReparo(execucaoId, correlationId))
                .onItem().transform(execucao -> {
                    execucaoPresenter.present(execucao);
                    return execucaoPresenter.viewModel();
                });
    }

    @POST
    @Path("execucoes/{execucaoId}/reparo/conclusao")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<ExecucaoViewModel> concluirReparo(
            @PathParam("execucaoId") UUID execucaoId,
            ExecucoesController.ConclusaoReparoRequest request,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return uni(execucoesController.concluirReparo(execucaoId, request, correlationId))
                .onItem().transform(execucao -> {
                    execucaoPresenter.present(execucao);
                    return execucaoPresenter.viewModel();
                });
    }

    @POST
    @Path("execucoes/{execucaoId}/cancelamento")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<ExecucaoViewModel> cancelarExecucao(
            @PathParam("execucaoId") UUID execucaoId,
            ExecucoesController.CancelamentoRequest request) {
        return uni(execucoesController.cancelarExecucao(execucaoId, request))
                .onItem().transform(execucao -> {
                    execucaoPresenter.present(execucao);
                    return execucaoPresenter.viewModel();
                });
    }

    private static <T> Uni<T> uni(CompletableFuture<T> future) {
        return Uni.createFrom().completionStage(future);
    }
}
