package br.com.oficina.execution.interfaces.presenters;

import br.com.oficina.execution.core.entities.catalogo.Servico;
import br.com.oficina.execution.interfaces.presenters.view_model.ServicoViewModel;
import java.util.List;

public class ServicoPresenterAdapter {
    private ServicoViewModel viewModel;
    private List<ServicoViewModel> viewModels = List.of();

    public void present(Servico servico) {
        viewModel = toViewModel(servico);
    }

    public void present(List<Servico> servicos) {
        viewModels = servicos.stream()
                .map(this::toViewModel)
                .toList();
    }

    public ServicoViewModel viewModel() {
        return viewModel;
    }

    public List<ServicoViewModel> viewModels() {
        return viewModels;
    }

    private ServicoViewModel toViewModel(Servico servico) {
        return new ServicoViewModel(
                servico.servicoId(),
                servico.nome(),
                servico.descricao(),
                servico.valorBase(),
                servico.ativo(),
                servico.criadoEm(),
                servico.atualizadoEm());
    }
}
