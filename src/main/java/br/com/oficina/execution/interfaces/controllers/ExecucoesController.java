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
    private final CriarExecucaoUseCase criarExecucaoUseCase;
    private final ListarExecucoesUseCase listarExecucoesUseCase;
    private final ListarFilaExecucaoUseCase listarFilaExecucaoUseCase;
    private final BuscarExecucaoUseCase buscarExecucaoUseCase;
    private final BuscarExecucaoDaOrdemServicoUseCase buscarExecucaoDaOrdemServicoUseCase;
    private final IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase;
    private final ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase;
    private final IniciarReparoUseCase iniciarReparoUseCase;
    private final ConcluirReparoUseCase concluirReparoUseCase;
    private final CancelarExecucaoUseCase cancelarExecucaoUseCase;

    public ExecucoesController(
            CriarExecucaoUseCase criarExecucaoUseCase,
            ListarExecucoesUseCase listarExecucoesUseCase,
            ListarFilaExecucaoUseCase listarFilaExecucaoUseCase,
            BuscarExecucaoUseCase buscarExecucaoUseCase,
            BuscarExecucaoDaOrdemServicoUseCase buscarExecucaoDaOrdemServicoUseCase,
            IniciarDiagnosticoUseCase iniciarDiagnosticoUseCase,
            ConcluirDiagnosticoUseCase concluirDiagnosticoUseCase,
            IniciarReparoUseCase iniciarReparoUseCase,
            ConcluirReparoUseCase concluirReparoUseCase,
            CancelarExecucaoUseCase cancelarExecucaoUseCase) {
        this.criarExecucaoUseCase = criarExecucaoUseCase;
        this.listarExecucoesUseCase = listarExecucoesUseCase;
        this.listarFilaExecucaoUseCase = listarFilaExecucaoUseCase;
        this.buscarExecucaoUseCase = buscarExecucaoUseCase;
        this.buscarExecucaoDaOrdemServicoUseCase = buscarExecucaoDaOrdemServicoUseCase;
        this.iniciarDiagnosticoUseCase = iniciarDiagnosticoUseCase;
        this.concluirDiagnosticoUseCase = concluirDiagnosticoUseCase;
        this.iniciarReparoUseCase = iniciarReparoUseCase;
        this.concluirReparoUseCase = concluirReparoUseCase;
        this.cancelarExecucaoUseCase = cancelarExecucaoUseCase;
    }

    public CompletableFuture<Execucao> criarExecucao(ExecucaoCreateRequest request) {
        return criarExecucaoUseCase.executar(new CriarExecucaoUseCase.Command(
                request.ordemServicoId(),
                request.prioridade()));
    }

    public CompletableFuture<List<Execucao>> consultarExecucoes(StatusExecucao status) {
        return listarExecucoesUseCase.executar(status);
    }

    public CompletableFuture<List<Execucao>> consultarFilaExecucao(StatusExecucao status) {
        return listarFilaExecucaoUseCase.executar(status);
    }

    public CompletableFuture<Execucao> consultarExecucao(UUID execucaoId) {
        return buscarExecucaoUseCase.executar(execucaoId);
    }

    public CompletableFuture<Execucao> consultarExecucaoDaOrdemServico(UUID ordemServicoId) {
        return buscarExecucaoDaOrdemServicoUseCase.executar(ordemServicoId);
    }

    public CompletableFuture<Execucao> iniciarDiagnostico(UUID execucaoId, String correlationId) {
        return iniciarDiagnosticoUseCase.executar(new IniciarDiagnosticoUseCase.Command(execucaoId, correlationId));
    }

    public CompletableFuture<Execucao> concluirDiagnostico(
            UUID execucaoId,
            ConclusaoDiagnosticoRequest request,
            String correlationId) {
        return concluirDiagnosticoUseCase.executar(new ConcluirDiagnosticoUseCase.Command(
                execucaoId,
                request == null ? null : request.diagnostico(),
                correlationId));
    }

    public CompletableFuture<Execucao> iniciarReparo(UUID execucaoId, String correlationId) {
        return iniciarReparoUseCase.executar(new IniciarReparoUseCase.Command(execucaoId, correlationId));
    }

    public CompletableFuture<Execucao> concluirReparo(
            UUID execucaoId,
            ConclusaoReparoRequest request,
            String correlationId) {
        return concluirReparoUseCase.executar(new ConcluirReparoUseCase.Command(
                execucaoId,
                request == null ? null : request.observacoes(),
                correlationId));
    }

    public CompletableFuture<Execucao> cancelarExecucao(UUID execucaoId, CancelamentoRequest request) {
        return cancelarExecucaoUseCase.executar(new CancelarExecucaoUseCase.Command(
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
}
