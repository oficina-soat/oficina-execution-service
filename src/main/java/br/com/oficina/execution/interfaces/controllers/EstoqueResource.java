package br.com.oficina.execution.interfaces.controllers;

import br.com.oficina.execution.core.entities.estoque.Estoque;
import br.com.oficina.execution.core.entities.estoque.MovimentoEstoque;
import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
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
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/estoques")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class EstoqueResource {
    @Inject
    DynamoDbExecutionStore store;

    @GET
    @Path("pecas/{pecaId}/saldo")
    public SaldoEstoqueResponse consultarSaldo(@PathParam("pecaId") UUID pecaId) {
        return toResponse(store.buscarSaldo(pecaId));
    }

    @GET
    @Path("movimentos")
    public List<MovimentoEstoqueResponse> consultarMovimentos(
            @QueryParam("pecaId") UUID pecaId,
            @QueryParam("ordemServicoId") UUID ordemServicoId) {
        return store.listarMovimentos(pecaId, ordemServicoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @POST
    @Path("movimentos/entrada")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Response registrarEntrada(MovimentoEstoqueRequest request, @HeaderParam("X-Correlation-Id") String correlationId) {
        return criarMovimento(TipoMovimentoEstoque.ENTRADA, request, correlationId);
    }

    @POST
    @Path("movimentos/reserva")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Response reservar(MovimentoEstoqueRequest request, @HeaderParam("X-Correlation-Id") String correlationId) {
        return criarMovimento(TipoMovimentoEstoque.RESERVA, request, correlationId);
    }

    @POST
    @Path("movimentos/consumo")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Response consumir(MovimentoEstoqueRequest request, @HeaderParam("X-Correlation-Id") String correlationId) {
        return criarMovimento(TipoMovimentoEstoque.CONSUMO, request, correlationId);
    }

    @POST
    @Path("movimentos/estorno")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Response estornar(MovimentoEstoqueRequest request, @HeaderParam("X-Correlation-Id") String correlationId) {
        return criarMovimento(TipoMovimentoEstoque.ESTORNO, request, correlationId);
    }

    private Response criarMovimento(
            TipoMovimentoEstoque tipo,
            MovimentoEstoqueRequest request,
            String correlationId) {
        var movimento = toResponse(store.registrarMovimento(
                tipo,
                request.pecaId(),
                request.ordemServicoId(),
                request.quantidade(),
                request.motivo(),
                correlationId));
        return Response.created(URI.create("/api/v1/estoques/movimentos/" + movimento.movimentoId()))
                .entity(movimento)
                .build();
    }

    private SaldoEstoqueResponse toResponse(Estoque estoque) {
        return new SaldoEstoqueResponse(
                estoque.pecaId(),
                estoque.quantidadeDisponivel(),
                estoque.quantidadeReservada(),
                estoque.atualizadoEm());
    }

    private MovimentoEstoqueResponse toResponse(MovimentoEstoque movimento) {
        return new MovimentoEstoqueResponse(
                movimento.movimentoId(),
                movimento.pecaId(),
                movimento.ordemServicoId(),
                movimento.tipo(),
                movimento.quantidade(),
                movimento.motivo(),
                movimento.criadoEm());
    }

    public record MovimentoEstoqueRequest(
            UUID pecaId,
            UUID ordemServicoId,
            int quantidade,
            String motivo) {
    }

    public record SaldoEstoqueResponse(
            UUID pecaId,
            int quantidadeDisponivel,
            int quantidadeReservada,
            OffsetDateTime atualizadoEm) {
    }

    public record MovimentoEstoqueResponse(
            UUID movimentoId,
            UUID pecaId,
            UUID ordemServicoId,
            TipoMovimentoEstoque tipo,
            int quantidade,
            String motivo,
            OffsetDateTime criadoEm) {
    }
}
