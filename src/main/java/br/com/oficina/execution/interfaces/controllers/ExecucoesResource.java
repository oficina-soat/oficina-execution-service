package br.com.oficina.execution.interfaces.controllers;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
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

@Path("/api/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class ExecucoesResource {
    @Inject
    DynamoDbExecutionStore store;

    @POST
    @Path("execucoes")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public Response criarExecucao(ExecucaoCreateRequest request) {
        var execucao = toResponse(store.criarExecucao(request.ordemServicoId(), request.prioridade()));
        return Response.created(URI.create("/api/v1/execucoes/" + execucao.execucaoId()))
                .entity(execucao)
                .build();
    }

    @GET
    @Path("execucoes")
    public List<ExecucaoResponse> consultarExecucoes(@QueryParam("status") StatusExecucao status) {
        return store.listarExecucoes(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("execucoes/fila")
    public List<FilaExecucaoItemResponse> consultarFilaExecucao(@QueryParam("status") StatusExecucao status) {
        var execucoes = store.listarFilaExecucao(status);
        var itens = new java.util.ArrayList<FilaExecucaoItemResponse>(execucoes.size());
        for (var i = 0; i < execucoes.size(); i++) {
            itens.add(toFilaResponse(execucoes.get(i), i + 1));
        }
        return List.copyOf(itens);
    }

    @GET
    @Path("execucoes/{execucaoId}")
    public ExecucaoResponse consultarExecucao(@PathParam("execucaoId") UUID execucaoId) {
        return toResponse(store.buscarExecucao(execucaoId));
    }

    @GET
    @Path("ordens-servico/{ordemServicoId}/execucao")
    public ExecucaoResponse consultarExecucaoDaOrdemServico(@PathParam("ordemServicoId") UUID ordemServicoId) {
        return toResponse(store.buscarExecucaoDaOrdemServico(ordemServicoId));
    }

    @POST
    @Path("execucoes/{execucaoId}/diagnostico/inicio")
    @Consumes(MediaType.WILDCARD)
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public ExecucaoResponse iniciarDiagnostico(
            @PathParam("execucaoId") UUID execucaoId,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return toResponse(store.iniciarDiagnostico(execucaoId, correlationId));
    }

    @POST
    @Path("execucoes/{execucaoId}/diagnostico/conclusao")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public ExecucaoResponse concluirDiagnostico(
            @PathParam("execucaoId") UUID execucaoId,
            ConclusaoDiagnosticoRequest request,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return toResponse(store.concluirDiagnostico(execucaoId, request == null ? null : request.diagnostico(), correlationId));
    }

    @POST
    @Path("execucoes/{execucaoId}/reparo/inicio")
    @Consumes(MediaType.WILDCARD)
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public ExecucaoResponse iniciarReparo(
            @PathParam("execucaoId") UUID execucaoId,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return toResponse(store.iniciarReparo(execucaoId, correlationId));
    }

    @POST
    @Path("execucoes/{execucaoId}/reparo/conclusao")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public ExecucaoResponse concluirReparo(
            @PathParam("execucaoId") UUID execucaoId,
            ConclusaoReparoRequest request,
            @HeaderParam("X-Correlation-Id") String correlationId) {
        return toResponse(store.concluirReparo(execucaoId, request == null ? null : request.observacoes(), correlationId));
    }

    @POST
    @Path("execucoes/{execucaoId}/cancelamento")
    @Parameter(name = "X-Idempotency-Key", in = ParameterIn.HEADER, required = true, description = "Chave de idempotência da operação mutável.")
    public ExecucaoResponse cancelarExecucao(
            @PathParam("execucaoId") UUID execucaoId,
            CancelamentoRequest request) {
        return toResponse(store.cancelarExecucao(execucaoId, request == null ? null : request.motivo()));
    }

    private ExecucaoResponse toResponse(Execucao execucao) {
        return new ExecucaoResponse(
                execucao.execucaoId(),
                execucao.ordemServicoId(),
                execucao.status(),
                execucao.prioridade(),
                execucao.diagnostico(),
                execucao.observacoesReparo(),
                execucao.criadoEm(),
                execucao.atualizadoEm());
    }

    private FilaExecucaoItemResponse toFilaResponse(Execucao execucao, int posicao) {
        return new FilaExecucaoItemResponse(
                posicao,
                execucao.execucaoId(),
                execucao.ordemServicoId(),
                execucao.status(),
                execucao.prioridade(),
                execucao.diagnostico(),
                execucao.observacoesReparo(),
                execucao.criadoEm(),
                execucao.atualizadoEm());
    }

    public record ExecucaoCreateRequest(UUID ordemServicoId, Integer prioridade) {
    }

    public record ConclusaoDiagnosticoRequest(String diagnostico) {
    }

    public record ConclusaoReparoRequest(String observacoes) {
    }

    public record CancelamentoRequest(String motivo) {
    }

    public record ExecucaoResponse(
            UUID execucaoId,
            UUID ordemServicoId,
            StatusExecucao status,
            int prioridade,
            String diagnostico,
            String observacoesReparo,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }

    public record FilaExecucaoItemResponse(
            int posicao,
            UUID execucaoId,
            UUID ordemServicoId,
            StatusExecucao status,
            int prioridade,
            String diagnostico,
            String observacoesReparo,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm) {
    }
}
