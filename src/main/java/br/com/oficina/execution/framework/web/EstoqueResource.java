package br.com.oficina.execution.framework.web;

import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import br.com.oficina.execution.interfaces.controllers.EstoqueController;
import br.com.oficina.execution.interfaces.presenters.MovimentoEstoquePresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.SaldoEstoquePresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.view_model.MovimentoEstoqueViewModel;
import br.com.oficina.execution.interfaces.presenters.view_model.PaginaViewModel;
import br.com.oficina.execution.interfaces.presenters.view_model.SaldoEstoqueViewModel;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

@Path("/api/v1/estoques")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Blocking
public class EstoqueResource {
    private final EstoqueController estoqueController;
    private final SaldoEstoquePresenterAdapter saldoPresenter;
    private final MovimentoEstoquePresenterAdapter movimentoPresenter;

    @Inject
    public EstoqueResource(
            EstoqueController estoqueController,
            SaldoEstoquePresenterAdapter saldoPresenter,
            MovimentoEstoquePresenterAdapter movimentoPresenter) {
        this.estoqueController = estoqueController;
        this.saldoPresenter = saldoPresenter;
        this.movimentoPresenter = movimentoPresenter;
    }

    @GET
    @Path("pecas/{pecaId}/saldo")
    public Uni<SaldoEstoqueViewModel> consultarSaldo(@PathParam("pecaId") UUID pecaId) {
        return uni(estoqueController.consultarSaldo(pecaId))
                .onItem().transform(estoque -> {
                    saldoPresenter.present(estoque);
                    return saldoPresenter.viewModel();
                });
    }

    @GET
    @Path("movimentos")
    public Uni<PaginaViewModel<MovimentoEstoqueViewModel>> consultarMovimentos(
            @QueryParam("pecaId") UUID pecaId,
            @QueryParam("ordemServicoId") UUID ordemServicoId,
            @QueryParam("tipo") TipoMovimentoEstoque tipo,
            @QueryParam("page") Integer page,
            @QueryParam("size") Integer size) {
        return uni(estoqueController.consultarMovimentos(pecaId, ordemServicoId))
                .onItem().transform(movimentos -> {
                    movimentoPresenter.present(movimentos);
                    var filtered = movimentoPresenter.viewModels().stream()
                            .filter(item -> tipo == null || item.tipo() == tipo)
                            .sorted((left, right) -> right.criadoEm().compareTo(left.criadoEm()))
                            .toList();
                    return PaginaViewModel.from(filtered, page == null ? 0 : page, size == null ? 20 : size);
                });
    }

    @POST
    @Path("movimentos/entrada")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> registrarEntrada(
            EstoqueController.MovimentoEstoqueRequest request,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return criarMovimento(TipoMovimentoEstoque.ENTRADA, request, correlationId);
    }

    @POST
    @Path("movimentos/reserva")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> reservar(
            EstoqueController.MovimentoEstoqueRequest request,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return criarMovimento(TipoMovimentoEstoque.RESERVA, request, correlationId);
    }

    @POST
    @Path("movimentos/consumo")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> consumir(
            EstoqueController.MovimentoEstoqueRequest request,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return criarMovimento(TipoMovimentoEstoque.CONSUMO, request, correlationId);
    }

    @POST
    @Path("movimentos/estorno")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Uni<Response> estornar(
            EstoqueController.MovimentoEstoqueRequest request,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return criarMovimento(TipoMovimentoEstoque.ESTORNO, request, correlationId);
    }

    private Uni<Response> criarMovimento(
            TipoMovimentoEstoque tipo,
            EstoqueController.MovimentoEstoqueRequest request,
            String correlationId) {
        return uni(estoqueController.registrarMovimento(tipo, request, correlationId))
                .onItem().transform(movimento -> {
                    movimentoPresenter.present(movimento);
                    var viewModel = movimentoPresenter.viewModel();
                    return Response.created(URI.create("/api/v1/estoques/movimentos/" + viewModel.movimentoId()))
                            .entity(viewModel)
                            .build();
                });
    }

    private static <T> Uni<T> uni(CompletableFuture<T> future) {
        return Uni.createFrom().completionStage(future);
    }
}
