package br.com.oficina.execution.interfaces.presenters;

import br.com.oficina.execution.core.usecases.status.ConsultarStatusUseCase.Status;
import br.com.oficina.execution.interfaces.presenters.view_model.StatusViewModel;

public class StatusPresenterAdapter {
    private StatusViewModel viewModel;

    public void present(Status status) {
        viewModel = new StatusViewModel(status.service(), status.environment(), status.status());
    }

    public StatusViewModel viewModel() {
        return viewModel;
    }
}
