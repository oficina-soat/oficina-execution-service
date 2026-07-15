package br.com.oficina.execution.interfaces.presenters;

import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.interfaces.presenters.view_model.ExecucaoViewModel;
import java.util.List;

public class ExecucaoPresenterAdapter {
    private ExecucaoViewModel viewModel;
    private List<ExecucaoViewModel> viewModels = List.of();

    public void present(Execucao execucao) {
        viewModel = toViewModel(execucao);
    }

    public void present(List<Execucao> execucoes) {
        viewModels = execucoes.stream()
                .map(this::toViewModel)
                .toList();
    }

    public ExecucaoViewModel viewModel() {
        return viewModel;
    }

    public List<ExecucaoViewModel> viewModels() {
        return viewModels;
    }

    private ExecucaoViewModel toViewModel(Execucao execucao) {
        return new ExecucaoViewModel(
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
