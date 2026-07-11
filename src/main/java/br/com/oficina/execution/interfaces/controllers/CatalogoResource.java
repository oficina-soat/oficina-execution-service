package br.com.oficina.execution.interfaces.controllers;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.core.entities.catalogo.Servico;
import br.com.oficina.execution.framework.dynamodb.DynamoDbExecutionStore;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import java.math.BigDecimal;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class CatalogoResource {
    private final DynamoDbExecutionStore store;

    @Inject
    public CatalogoResource(DynamoDbExecutionStore store) {
        this.store = store;
    }

    @POST
    @Path("servicos")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Response criarServico(ServicoRequest request) {
        var servico = toResponse(store.criarServico(request.nome(), request.descricao(), request.valorBase()));
        return Response.created(URI.create("/api/v1/servicos/" + servico.servicoId()))
                .entity(servico)
                .build();
    }

    @GET
    @Path("servicos")
    public List<ServicoResponse> consultarServicos() {
        return store.listarServicos().stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("servicos/{servicoId}")
    public ServicoResponse consultarServico(@PathParam("servicoId") UUID servicoId) {
        return toResponse(store.buscarServico(servicoId));
    }

    @PUT
    @Path("servicos/{servicoId}")
    public ServicoResponse atualizarServico(@PathParam("servicoId") UUID servicoId, ServicoRequest request) {
        return toResponse(store.atualizarServico(servicoId, request.nome(), request.descricao(), request.valorBase()));
    }

    @POST
    @Path("pecas")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Response criarPeca(PecaRequest request) {
        var peca = toResponse(store.criarPeca(request.nome(), request.codigo(), request.valorUnitario()));
        return Response.created(URI.create("/api/v1/pecas/" + peca.pecaId()))
                .entity(peca)
                .build();
    }

    @GET
    @Path("pecas")
    public List<PecaResponse> consultarPecas() {
        return store.listarPecas().stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("pecas/{pecaId}")
    public PecaResponse consultarPeca(@PathParam("pecaId") UUID pecaId) {
        return toResponse(store.buscarPeca(pecaId));
    }

    @PUT
    @Path("pecas/{pecaId}")
    public PecaResponse atualizarPeca(@PathParam("pecaId") UUID pecaId, PecaRequest request) {
        return toResponse(store.atualizarPeca(pecaId, request.nome(), request.codigo(), request.valorUnitario()));
    }

    private ServicoResponse toResponse(Servico servico) {
        return new ServicoResponse(
                servico.servicoId(),
                servico.nome(),
                servico.descricao(),
                servico.valorBase(),
                servico.ativo(),
                servico.criadoEm(),
                servico.atualizadoEm());
    }

    private PecaResponse toResponse(Peca peca) {
        return new PecaResponse(
                peca.pecaId(),
                peca.nome(),
                peca.codigo(),
                peca.valorUnitario(),
                peca.ativo(),
                peca.criadoEm(),
                peca.atualizadoEm());
    }

    public record ServicoRequest(String nome, String descricao, BigDecimal valorBase) {
    }

    public record ServicoResponse(
            UUID servicoId,
            String nome,
            String descricao,
            BigDecimal valorBase,
            boolean ativo,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    public record PecaRequest(String nome, String codigo, BigDecimal valorUnitario) {
    }

    public record PecaResponse(
            UUID pecaId,
            String nome,
            String codigo,
            BigDecimal valorUnitario,
            boolean ativo,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }
}
