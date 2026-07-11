package br.com.oficina.execution.interfaces.presenters;

import br.com.oficina.execution.core.entities.estoque.Estoque;
import br.com.oficina.execution.interfaces.presenters.view_model.SaldoEstoqueViewModel;

public class SaldoEstoquePresenterAdapter {
    private SaldoEstoqueViewModel viewModel;

    public void present(Estoque estoque) {
        viewModel = new SaldoEstoqueViewModel(
                estoque.pecaId(),
                estoque.quantidadeDisponivel(),
                estoque.quantidadeReservada(),
                estoque.atualizadoEm());
    }

    public SaldoEstoqueViewModel viewModel() {
        return viewModel;
    }
}
