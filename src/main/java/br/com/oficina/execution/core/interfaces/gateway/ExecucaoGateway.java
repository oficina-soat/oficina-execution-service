package br.com.oficina.execution.core.interfaces.gateway;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ExecucaoGateway {
    CompletableFuture<Execucao> criarExecucao(UUID ordemServicoId, Integer prioridade);

    CompletableFuture<List<Execucao>> listarExecucoes(StatusExecucao status);

    CompletableFuture<List<Execucao>> listarFilaExecucao(StatusExecucao status);

    CompletableFuture<Execucao> buscarExecucao(UUID execucaoId);

    CompletableFuture<Execucao> buscarExecucaoDaOrdemServico(UUID ordemServicoId);

    CompletableFuture<Execucao> iniciarDiagnostico(UUID execucaoId, String correlationId);

    CompletableFuture<Execucao> concluirDiagnostico(UUID execucaoId, String diagnostico, String correlationId);

    CompletableFuture<Execucao> iniciarReparo(UUID execucaoId, String correlationId);

    CompletableFuture<Execucao> concluirReparo(UUID execucaoId, String observacoes, String correlationId);

    CompletableFuture<Execucao> cancelarExecucao(UUID execucaoId, String motivo);
}
