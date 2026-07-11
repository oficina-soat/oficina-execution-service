package br.com.oficina.execution.interfaces.controllers;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import br.com.oficina.execution.core.usecases.execucao.BuscarExecucaoDaOrdemServicoUseCase;
import br.com.oficina.execution.core.usecases.execucao.BuscarExecucaoUseCase;
import br.com.oficina.execution.core.usecases.execucao.CancelarExecucaoUseCase;
import br.com.oficina.execution.core.usecases.execucao.ConcluirDiagnosticoUseCase;
import br.com.oficina.execution.core.usecases.execucao.ConcluirReparoUseCase;
import br.com.oficina.execution.core.usecases.execucao.CriarExecucaoUseCase;
import br.com.oficina.execution.core.usecases.execucao.IniciarDiagnosticoUseCase;
import br.com.oficina.execution.core.usecases.execucao.IniciarReparoUseCase;
import br.com.oficina.execution.core.usecases.execucao.ListarExecucoesUseCase;
import br.com.oficina.execution.core.usecases.execucao.ListarFilaExecucaoUseCase;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ExecucoesController {
    private final ExecucaoQueryUseCases queries;
    private final ExecucaoCommandUseCases commands;

    public ExecucoesController(ExecucaoQueryUseCases queries, ExecucaoCommandUseCases commands) {
        this.queries = queries;
        this.commands = commands;
    }

    public CompletableFuture<Execucao> criarExecucao(ExecucaoCreateRequest request) {
        return commands.criar().executar(new CriarExecucaoUseCase.Command(
                request.ordemServicoId(),
                request.prioridade()));
    }

    public CompletableFuture<List<Execucao>> consultarExecucoes(StatusExecucao status) {
        return queries.listar().executar(status);
    }

    public CompletableFuture<List<Execucao>> consultarFilaExecucao(StatusExecucao status) {
        return queries.listarFila().executar(status);
    }

    public CompletableFuture<Execucao> consultarExecucao(UUID execucaoId) {
        return queries.buscar().executar(execucaoId);
    }

    public CompletableFuture<Execucao> consultarExecucaoDaOrdemServico(UUID ordemServicoId) {
        return queries.buscarPorOrdemServico().executar(ordemServicoId);
    }

    public CompletableFuture<Execucao> iniciarDiagnostico(UUID execucaoId, String correlationId) {
        return commands.iniciarDiagnostico().executar(new IniciarDiagnosticoUseCase.Command(execucaoId, correlationId));
    }

    public CompletableFuture<Execucao> concluirDiagnostico(
            UUID execucaoId,
            ConclusaoDiagnosticoRequest request,
            String correlationId) {
        return commands.concluirDiagnostico().executar(new ConcluirDiagnosticoUseCase.Command(
                execucaoId,
                request == null ? null : request.diagnostico(),
                correlationId));
    }

    public CompletableFuture<Execucao> iniciarReparo(UUID execucaoId, String correlationId) {
        return commands.iniciarReparo().executar(new IniciarReparoUseCase.Command(execucaoId, correlationId));
    }

    public CompletableFuture<Execucao> concluirReparo(
            UUID execucaoId,
            ConclusaoReparoRequest request,
            String correlationId) {
        return commands.concluirReparo().executar(new ConcluirReparoUseCase.Command(
                execucaoId,
                request == null ? null : request.observacoes(),
                correlationId));
    }

    public CompletableFuture<Execucao> cancelarExecucao(UUID execucaoId, CancelamentoRequest request) {
        return commands.cancelar().executar(new CancelarExecucaoUseCase.Command(
                execucaoId,
                request == null ? null : request.motivo()));
    }

    public record ExecucaoCreateRequest(UUID ordemServicoId, Integer prioridade) {
    }

    public record ConclusaoDiagnosticoRequest(String diagnostico) {
    }

    public record ConclusaoReparoRequest(String observacoes) {
    }

    public record CancelamentoRequest(String motivo) {
    }

    public record ExecucaoQueryUseCases(
            ListarExecucoesUseCase listar,
            ListarFilaExecucaoUseCase listarFila,
            BuscarExecucaoUseCase buscar,
            BuscarExecucaoDaOrdemServicoUseCase buscarPorOrdemServico) {
    }

    public record ExecucaoCommandUseCases(
            CriarExecucaoUseCase criar,
            IniciarDiagnosticoUseCase iniciarDiagnostico,
            ConcluirDiagnosticoUseCase concluirDiagnostico,
            IniciarReparoUseCase iniciarReparo,
            ConcluirReparoUseCase concluirReparo,
            CancelarExecucaoUseCase cancelar) {
    }
}
