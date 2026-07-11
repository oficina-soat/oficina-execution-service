package br.com.oficina.execution.interfaces.presenters.view_model;

import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FilaExecucaoItemViewModel(
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
