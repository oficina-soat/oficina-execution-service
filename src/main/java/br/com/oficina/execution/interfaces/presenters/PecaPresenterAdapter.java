package br.com.oficina.execution.interfaces.presenters;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.interfaces.presenters.view_model.PecaViewModel;
import java.util.List;

public class PecaPresenterAdapter {
    private PecaViewModel viewModel;
    private List<PecaViewModel> viewModels = List.of();

    public void present(Peca peca) {
        viewModel = toViewModel(peca);
    }

    public void present(List<Peca> pecas) {
        viewModels = pecas.stream()
                .map(this::toViewModel)
                .toList();
    }

    public PecaViewModel viewModel() {
        return viewModel;
    }

    public List<PecaViewModel> viewModels() {
        return viewModels;
    }

    private PecaViewModel toViewModel(Peca peca) {
        return new PecaViewModel(
                peca.pecaId(),
                peca.nome(),
                peca.codigo(),
                peca.valorUnitario(),
                peca.ativo(),
                peca.criadoEm(),
                peca.atualizadoEm());
    }
}
