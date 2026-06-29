package br.com.oficina.execution.interfaces.controllers;

import br.com.oficina.execution.core.entities.estoque.Estoque;
import br.com.oficina.execution.core.entities.estoque.MovimentoEstoque;
import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
    public Response registrarEntrada(MovimentoEstoqueRequest request) {
        return criarMovimento(TipoMovimentoEstoque.ENTRADA, request);
    }

    @POST
    @Path("movimentos/reserva")
    public Response reservar(MovimentoEstoqueRequest request) {
        return criarMovimento(TipoMovimentoEstoque.RESERVA, request);
    }

    @POST
    @Path("movimentos/consumo")
    public Response consumir(MovimentoEstoqueRequest request) {
        return criarMovimento(TipoMovimentoEstoque.CONSUMO, request);
    }

    @POST
    @Path("movimentos/estorno")
    public Response estornar(MovimentoEstoqueRequest request) {
        return criarMovimento(TipoMovimentoEstoque.ESTORNO, request);
    }

    private Response criarMovimento(TipoMovimentoEstoque tipo, MovimentoEstoqueRequest request) {
        var movimento = toResponse(store.registrarMovimento(
                tipo,
                request.pecaId(),
                request.ordemServicoId(),
                request.quantidade(),
                request.motivo()));
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
