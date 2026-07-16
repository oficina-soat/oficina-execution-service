package br.com.oficina.execution.framework.web;

import br.com.oficina.execution.interfaces.controllers.CatalogoController;
import br.com.oficina.execution.interfaces.presenters.PecaPresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.ServicoPresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.view_model.PecaViewModel;
import br.com.oficina.execution.interfaces.presenters.view_model.PaginaViewModel;
import br.com.oficina.execution.interfaces.presenters.view_model.ServicoViewModel;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

@Path("/api/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Blocking
public class CatalogoResource {
    private final CatalogoController catalogoController;
    private final ServicoPresenterAdapter servicoPresenter;
    private final PecaPresenterAdapter pecaPresenter;

    @Inject
    public CatalogoResource(
            CatalogoController catalogoController,
            ServicoPresenterAdapter servicoPresenter,
            PecaPresenterAdapter pecaPresenter) {
        this.catalogoController = catalogoController;
        this.servicoPresenter = servicoPresenter;
        this.pecaPresenter = pecaPresenter;
    }

    @POST
    @Path("servicos")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> criarServico(CatalogoController.ServicoRequest request) {
        return uni(catalogoController.criarServico(request))
                .onItem().transform(servico -> {
                    servicoPresenter.present(servico);
                    var viewModel = servicoPresenter.viewModel();
                    return Response.created(URI.create("/api/v1/servicos/" + viewModel.servicoId()))
                            .entity(viewModel)
                            .build();
                });
    }

    @GET
    @Path("servicos")
    public Uni<PaginaViewModel<ServicoViewModel>> consultarServicos(
            @QueryParam("nome") String nome,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        return uni(catalogoController.consultarServicos())
                .onItem().transform(servicos -> {
                    servicoPresenter.present(servicos);
                    var filtered = servicoPresenter.viewModels().stream()
                            .filter(item -> containsIgnoreCase(item.nome(), nome))
                            .sorted((left, right) -> left.nome().compareToIgnoreCase(right.nome()))
                            .toList();
                    return PaginaViewModel.from(filtered, defaultPage(page), defaultSize(size));
                });
    }

    @GET
    @Path("servicos/{servicoId}")
    public Uni<ServicoViewModel> consultarServico(@PathParam("servicoId") UUID servicoId) {
        return uni(catalogoController.consultarServico(servicoId))
                .onItem().transform(servico -> {
                    servicoPresenter.present(servico);
                    return servicoPresenter.viewModel();
                });
    }

    @PUT
    @Path("servicos/{servicoId}")
    public Uni<ServicoViewModel> atualizarServico(
            @PathParam("servicoId") UUID servicoId,
            CatalogoController.ServicoRequest request) {
        return uni(catalogoController.atualizarServico(servicoId, request))
                .onItem().transform(servico -> {
                    servicoPresenter.present(servico);
                    return servicoPresenter.viewModel();
                });
    }

    @POST
    @Path("pecas")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> criarPeca(CatalogoController.PecaRequest request) {
        return uni(catalogoController.criarPeca(request))
                .onItem().transform(peca -> {
                    pecaPresenter.present(peca);
                    var viewModel = pecaPresenter.viewModel();
                    return Response.created(URI.create("/api/v1/pecas/" + viewModel.pecaId()))
                            .entity(viewModel)
                            .build();
                });
    }

    @GET
    @Path("pecas")
    public Uni<PaginaViewModel<PecaViewModel>> consultarPecas(
            @QueryParam("nome") String nome,
            @QueryParam("codigo") String codigo,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        return uni(catalogoController.consultarPecas())
                .onItem().transform(pecas -> {
                    pecaPresenter.present(pecas);
                    var filtered = pecaPresenter.viewModels().stream()
                            .filter(item -> containsIgnoreCase(item.nome(), nome))
                            .filter(item -> containsIgnoreCase(item.codigo(), codigo))
                            .sorted((left, right) -> left.nome().compareToIgnoreCase(right.nome()))
                            .toList();
                    return PaginaViewModel.from(filtered, defaultPage(page), defaultSize(size));
                });
    }

    @GET
    @Path("pecas/{pecaId}")
    public Uni<PecaViewModel> consultarPeca(@PathParam("pecaId") UUID pecaId) {
        return uni(catalogoController.consultarPeca(pecaId))
                .onItem().transform(peca -> {
                    pecaPresenter.present(peca);
                    return pecaPresenter.viewModel();
                });
    }

    @PUT
    @Path("pecas/{pecaId}")
    public Uni<PecaViewModel> atualizarPeca(
            @PathParam("pecaId") UUID pecaId,
            CatalogoController.PecaRequest request) {
        return uni(catalogoController.atualizarPeca(pecaId, request))
                .onItem().transform(peca -> {
                    pecaPresenter.present(peca);
                    return pecaPresenter.viewModel();
                });
    }

    private static <T> Uni<T> uni(CompletableFuture<T> future) {
        return Uni.createFrom().completionStage(future);
    }

    private static boolean containsIgnoreCase(String value, String filter) {
        return filter == null || filter.isBlank()
                || value.toLowerCase(Locale.ROOT).contains(filter.trim().toLowerCase(Locale.ROOT));
    }

    private static int defaultPage(Integer page) {
        return page == null ? 0 : page;
    }

    private static int defaultSize(Integer size) {
        return size == null ? 20 : size;
    }
}
