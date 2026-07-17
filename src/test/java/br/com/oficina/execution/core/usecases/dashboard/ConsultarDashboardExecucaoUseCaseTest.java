package br.com.oficina.execution.core.usecases.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import br.com.oficina.execution.core.interfaces.gateway.ExecucaoGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class ConsultarDashboardExecucaoUseCaseTest {
    @Test
    void agregaStatusEPreservaOrdemCanonicaDaFila() {
        var gateway = mock(ExecucaoGateway.class);
        var now = OffsetDateTime.parse("2026-07-17T18:30:00Z");
        var execucao = new Execucao(UUID.randomUUID(), UUID.randomUUID(), 10, now.minusHours(1));
        when(gateway.listarExecucoes(null)).thenReturn(CompletableFuture.completedFuture(List.of(execucao)));
        when(gateway.listarFilaExecucao(null)).thenReturn(CompletableFuture.completedFuture(List.of(execucao)));
        var useCase = new ConsultarDashboardExecucaoUseCase(
                gateway, Clock.fixed(Instant.from(now), ZoneOffset.UTC));

        var result = useCase.executar().join();

        assertEquals(now, result.generatedAt());
        assertEquals(1, result.totalFila());
        assertEquals(1, result.proximasExecucoes().getFirst().posicao());
        assertEquals(1, result.contagensPorStatus().stream()
                .filter(item -> item.status() == StatusExecucao.CRIADA).findFirst().orElseThrow().quantidade());
        assertEquals(List.of(), result.estoqueAtencoes());
    }
}
