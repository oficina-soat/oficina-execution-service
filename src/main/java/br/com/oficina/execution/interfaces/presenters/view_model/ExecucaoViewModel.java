package br.com.oficina.execution.interfaces.presenters.view_model;

import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import br.com.oficina.execution.core.entities.execucao.AcaoPermitidaExecucao;

public record ExecucaoViewModel(
        UUID execucaoId,
        UUID ordemServicoId,
        StatusExecucao status,
        int prioridade,
        String diagnostico,
        String observacoesReparo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm,
        List<AcaoPermitidaExecucao> acoesPermitidas) {
}
