package br.com.oficina.execution.interfaces.presenters;

import br.com.oficina.execution.core.entities.estoque.MovimentoEstoque;
import br.com.oficina.execution.interfaces.presenters.view_model.MovimentoEstoqueViewModel;
import java.util.List;

public class MovimentoEstoquePresenterAdapter {
    private MovimentoEstoqueViewModel viewModel;
    private List<MovimentoEstoqueViewModel> viewModels = List.of();

    public void present(MovimentoEstoque movimento) {
        viewModel = toViewModel(movimento);
    }

    public void present(List<MovimentoEstoque> movimentos) {
        viewModels = movimentos.stream()
                .map(this::toViewModel)
                .toList();
    }

    public MovimentoEstoqueViewModel viewModel() {
        return viewModel;
    }

    public List<MovimentoEstoqueViewModel> viewModels() {
        return viewModels;
    }

    private MovimentoEstoqueViewModel toViewModel(MovimentoEstoque movimento) {
        return new MovimentoEstoqueViewModel(
                movimento.movimentoId(),
                movimento.pecaId(),
                movimento.ordemServicoId(),
                movimento.tipo(),
                movimento.quantidade(),
                movimento.motivo(),
                movimento.criadoEm());
    }
}
