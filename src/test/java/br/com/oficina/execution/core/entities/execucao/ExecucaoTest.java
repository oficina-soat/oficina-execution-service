package br.com.oficina.execution.core.entities.execucao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExecucaoTest {

    @Test
    void deveDerivarAsAcoesPermitidasConformeAsTransicoes() {
        var agora = OffsetDateTime.parse("2026-07-15T12:00:00Z");
        var execucao = new Execucao(UUID.randomUUID(), UUID.randomUUID(), agora);

        assertEquals(
                List.of(AcaoPermitidaExecucao.INICIAR_DIAGNOSTICO, AcaoPermitidaExecucao.CANCELAR),
                execucao.acoesPermitidas());

        execucao.iniciarDiagnostico(agora.plusMinutes(1));
        assertEquals(
                List.of(AcaoPermitidaExecucao.CONCLUIR_DIAGNOSTICO, AcaoPermitidaExecucao.CANCELAR),
                execucao.acoesPermitidas());

        execucao.concluirDiagnostico("Falha elétrica", agora.plusMinutes(2));
        assertEquals(
                List.of(AcaoPermitidaExecucao.INICIAR_REPARO, AcaoPermitidaExecucao.CANCELAR),
                execucao.acoesPermitidas());

        execucao.iniciarReparo(agora.plusMinutes(3));
        assertEquals(
                List.of(AcaoPermitidaExecucao.CONCLUIR_REPARO, AcaoPermitidaExecucao.CANCELAR),
                execucao.acoesPermitidas());

        execucao.concluirReparo("Reparo concluído", agora.plusMinutes(4));
        assertEquals(List.of(), execucao.acoesPermitidas());
    }
}
