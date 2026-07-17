package br.com.oficina.execution.core.usecases.dashboard;

import br.com.oficina.execution.core.entities.execucao.AcaoPermitidaExecucao;
import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import br.com.oficina.execution.core.interfaces.gateway.ExecucaoGateway;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ConsultarDashboardExecucaoUseCase {
    private static final int LIMITE_FILA = 5;
    private final ExecucaoGateway gateway;
    private final Clock clock;

    public ConsultarDashboardExecucaoUseCase(ExecucaoGateway gateway) {
        this(gateway, Clock.systemUTC());
    }

    ConsultarDashboardExecucaoUseCase(ExecucaoGateway gateway, Clock clock) {
        this.gateway = gateway;
        this.clock = clock;
    }

    public CompletableFuture<DashboardExecucao> executar() {
        var execucoes = gateway.listarExecucoes(null);
        var fila = gateway.listarFilaExecucao(null);
        return execucoes.thenCombine(fila, (todos, itensFila) -> {
            var now = OffsetDateTime.now(clock);
            var contagens = Arrays.stream(StatusExecucao.values())
                    .map(status -> new ContagemStatus(status, todos.stream().filter(e -> e.status() == status).count()))
                    .toList();
            var proximas = itensFila.stream()
                    .limit(LIMITE_FILA)
                    .map(execucao -> paraItemFila(execucao, itensFila.indexOf(execucao) + 1))
                    .toList();
            return new DashboardExecucao(
                    now,
                    now,
                    30,
                    contagens,
                    itensFila.size(),
                    proximas,
                    List.of());
        });
    }

    private static ItemFila paraItemFila(Execucao execucao, int posicao) {
        return new ItemFila(
                execucao.execucaoId(),
                execucao.ordemServicoId(),
                execucao.prioridade(),
                execucao.status(),
                execucao.diagnostico(),
                execucao.observacoesReparo(),
                execucao.criadoEm(),
                execucao.atualizadoEm(),
                execucao.acoesPermitidas(),
                posicao);
    }

    public record DashboardExecucao(
            OffsetDateTime generatedAt,
            OffsetDateTime dataAsOf,
            int refreshAfterSeconds,
            List<ContagemStatus> contagensPorStatus,
            long totalFila,
            List<ItemFila> proximasExecucoes,
            List<EstoqueAtencao> estoqueAtencoes) {
    }

    public record ContagemStatus(StatusExecucao status, long quantidade) {
    }

    public record ItemFila(
            UUID execucaoId,
            UUID ordemServicoId,
            int prioridade,
            StatusExecucao status,
            String diagnostico,
            String observacoesReparo,
            OffsetDateTime criadoEm,
            OffsetDateTime atualizadoEm,
            List<AcaoPermitidaExecucao> acoesPermitidas,
            int posicao) {
    }

    public record EstoqueAtencao(
            UUID pecaId,
            String nome,
            int saldoAtual,
            int limiteReposicao,
            OffsetDateTime atualizadoEm) {
    }
}
