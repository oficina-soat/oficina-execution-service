package br.com.oficina.execution.core.entities.execucao;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class Execucao {
    public static final int PRIORIDADE_PADRAO = 100;

    private final UUID execucaoId;
    private final UUID ordemServicoId;
    private final OffsetDateTime criadoEm;
    private final int prioridade;
    private StatusExecucao status;
    private String diagnostico;
    private String observacoesReparo;
    private OffsetDateTime atualizadoEm;

    public Execucao(UUID execucaoId, UUID ordemServicoId, OffsetDateTime criadoEm) {
        this(execucaoId, ordemServicoId, PRIORIDADE_PADRAO, criadoEm);
    }

    public Execucao(UUID execucaoId, UUID ordemServicoId, int prioridade, OffsetDateTime criadoEm) {
        if (execucaoId == null) {
            throw new IllegalArgumentException("Identificador da execucao e obrigatorio.");
        }
        if (ordemServicoId == null) {
            throw new IllegalArgumentException("ordemServicoId e obrigatorio.");
        }
        if (criadoEm == null) {
            throw new IllegalArgumentException("Data de criacao da execucao e obrigatoria.");
        }
        if (prioridade < 0) {
            throw new IllegalArgumentException("prioridade deve ser maior ou igual a zero.");
        }
        this.execucaoId = execucaoId;
        this.ordemServicoId = ordemServicoId;
        this.criadoEm = criadoEm;
        this.prioridade = prioridade;
        this.atualizadoEm = criadoEm;
        this.status = StatusExecucao.CRIADA;
    }

    public void iniciarDiagnostico(OffsetDateTime agora) {
        exigirStatus(StatusExecucao.CRIADA, "Diagnostico so pode iniciar em execucao CRIADA.");
        transicionar(StatusExecucao.EM_DIAGNOSTICO, agora);
    }

    public void concluirDiagnostico(String diagnostico, OffsetDateTime agora) {
        exigirStatus(StatusExecucao.EM_DIAGNOSTICO, "Diagnostico so pode ser concluido em execucao EM_DIAGNOSTICO.");
        this.diagnostico = textoOpcional(diagnostico);
        transicionar(StatusExecucao.DIAGNOSTICO_CONCLUIDO, agora);
    }

    public void iniciarReparo(OffsetDateTime agora) {
        exigirStatus(StatusExecucao.DIAGNOSTICO_CONCLUIDO, "Reparo so pode iniciar em execucao DIAGNOSTICO_CONCLUIDO.");
        transicionar(StatusExecucao.EM_REPARO, agora);
    }

    public void concluirReparo(String observacoes, OffsetDateTime agora) {
        exigirStatus(StatusExecucao.EM_REPARO, "Reparo so pode ser concluido em execucao EM_REPARO.");
        this.observacoesReparo = textoOpcional(observacoes);
        transicionar(StatusExecucao.REPARO_CONCLUIDO, agora);
    }

    public void cancelar(String motivo, OffsetDateTime agora) {
        if (status == StatusExecucao.REPARO_CONCLUIDO || status == StatusExecucao.CANCELADA) {
            throw new WebApplicationException("Execucao nao pode ser cancelada no status " + status + ".", Response.Status.CONFLICT);
        }
        this.observacoesReparo = textoOpcional(motivo);
        transicionar(StatusExecucao.CANCELADA, agora);
    }

    public UUID execucaoId() {
        return execucaoId;
    }

    public UUID ordemServicoId() {
        return ordemServicoId;
    }

    public StatusExecucao status() {
        return status;
    }

    public int prioridade() {
        return prioridade;
    }

    public String diagnostico() {
        return diagnostico;
    }

    public String observacoesReparo() {
        return observacoesReparo;
    }

    public OffsetDateTime criadoEm() {
        return criadoEm;
    }

    public OffsetDateTime atualizadoEm() {
        return atualizadoEm;
    }

    private void exigirStatus(StatusExecucao esperado, String mensagem) {
        if (status != esperado) {
            throw new WebApplicationException(mensagem, Response.Status.CONFLICT);
        }
    }

    private void transicionar(StatusExecucao novoStatus, OffsetDateTime agora) {
        status = novoStatus;
        atualizadoEm = agora;
    }

    private static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
