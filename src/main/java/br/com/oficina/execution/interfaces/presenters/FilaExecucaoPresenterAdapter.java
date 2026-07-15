package br.com.oficina.execution.interfaces.presenters;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.interfaces.presenters.view_model.FilaExecucaoItemViewModel;
import java.util.ArrayList;
import java.util.List;

public class FilaExecucaoPresenterAdapter {
    private List<FilaExecucaoItemViewModel> viewModels = List.of();

    public void present(List<Execucao> execucoes) {
        var itens = new ArrayList<FilaExecucaoItemViewModel>(execucoes.size());
        for (var i = 0; i < execucoes.size(); i++) {
            itens.add(toViewModel(execucoes.get(i), i + 1));
        }
        viewModels = List.copyOf(itens);
    }

    public List<FilaExecucaoItemViewModel> viewModels() {
        return viewModels;
    }

    private FilaExecucaoItemViewModel toViewModel(Execucao execucao, int posicao) {
        return new FilaExecucaoItemViewModel(
                posicao,
                execucao.execucaoId(),
                execucao.ordemServicoId(),
                execucao.status(),
                execucao.prioridade(),
                execucao.diagnostico(),
                execucao.observacoesReparo(),
                execucao.criadoEm(),
                execucao.atualizadoEm(),
                execucao.acoesPermitidas());
    }
}
