package br.com.oficina.execution.framework.web;

import br.com.oficina.execution.core.interfaces.gateway.CatalogoGateway;
import br.com.oficina.execution.core.interfaces.gateway.EstoqueGateway;
import br.com.oficina.execution.core.interfaces.gateway.ExecucaoGateway;
import br.com.oficina.execution.core.usecases.catalogo.AtualizarPecaUseCase;
import br.com.oficina.execution.core.usecases.catalogo.AtualizarServicoUseCase;
import br.com.oficina.execution.core.usecases.catalogo.BuscarPecaUseCase;
import br.com.oficina.execution.core.usecases.catalogo.BuscarServicoUseCase;
import br.com.oficina.execution.core.usecases.catalogo.CriarPecaUseCase;
import br.com.oficina.execution.core.usecases.catalogo.CriarServicoUseCase;
import br.com.oficina.execution.core.usecases.catalogo.ListarPecasUseCase;
import br.com.oficina.execution.core.usecases.catalogo.ListarServicosUseCase;
import br.com.oficina.execution.core.usecases.estoque.ConsultarSaldoEstoqueUseCase;
import br.com.oficina.execution.core.usecases.estoque.ListarMovimentosEstoqueUseCase;
import br.com.oficina.execution.core.usecases.estoque.RegistrarMovimentoEstoqueUseCase;
import br.com.oficina.execution.core.usecases.execucao.BuscarExecucaoDaOrdemServicoUseCase;
import br.com.oficina.execution.core.usecases.execucao.BuscarExecucaoUseCase;
import br.com.oficina.execution.core.usecases.execucao.CancelarExecucaoUseCase;
import br.com.oficina.execution.core.usecases.execucao.ConcluirDiagnosticoUseCase;
import br.com.oficina.execution.core.usecases.execucao.ConcluirReparoUseCase;
import br.com.oficina.execution.core.usecases.execucao.CriarExecucaoUseCase;
import br.com.oficina.execution.core.usecases.execucao.IniciarDiagnosticoUseCase;
import br.com.oficina.execution.core.usecases.execucao.IniciarReparoUseCase;
import br.com.oficina.execution.core.usecases.execucao.ListarExecucoesUseCase;
import br.com.oficina.execution.core.usecases.execucao.ListarFilaExecucaoUseCase;
import br.com.oficina.execution.core.usecases.status.ConsultarStatusUseCase;
import br.com.oficina.execution.interfaces.controllers.CatalogoController;
import br.com.oficina.execution.interfaces.controllers.EstoqueController;
import br.com.oficina.execution.interfaces.controllers.ExecucoesController;
import br.com.oficina.execution.interfaces.controllers.StatusController;
import br.com.oficina.execution.interfaces.presenters.ExecucaoPresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.FilaExecucaoPresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.MovimentoEstoquePresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.PecaPresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.SaldoEstoquePresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.ServicoPresenterAdapter;
import br.com.oficina.execution.interfaces.presenters.StatusPresenterAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class ExecutionConfiguration {
    @Produces
    CatalogoController catalogoController(CatalogoGateway catalogoGateway) {
        return new CatalogoController(
                new CriarServicoUseCase(catalogoGateway),
                new ListarServicosUseCase(catalogoGateway),
                new BuscarServicoUseCase(catalogoGateway),
                new AtualizarServicoUseCase(catalogoGateway),
                new CriarPecaUseCase(catalogoGateway),
                new ListarPecasUseCase(catalogoGateway),
                new BuscarPecaUseCase(catalogoGateway),
                new AtualizarPecaUseCase(catalogoGateway));
    }

    @Produces
    EstoqueController estoqueController(EstoqueGateway estoqueGateway) {
        return new EstoqueController(
                new ConsultarSaldoEstoqueUseCase(estoqueGateway),
                new ListarMovimentosEstoqueUseCase(estoqueGateway),
                new RegistrarMovimentoEstoqueUseCase(estoqueGateway));
    }

    @Produces
    ExecucoesController execucoesController(ExecucaoGateway execucaoGateway) {
        return new ExecucoesController(
                new CriarExecucaoUseCase(execucaoGateway),
                new ListarExecucoesUseCase(execucaoGateway),
                new ListarFilaExecucaoUseCase(execucaoGateway),
                new BuscarExecucaoUseCase(execucaoGateway),
                new BuscarExecucaoDaOrdemServicoUseCase(execucaoGateway),
                new IniciarDiagnosticoUseCase(execucaoGateway),
                new ConcluirDiagnosticoUseCase(execucaoGateway),
                new IniciarReparoUseCase(execucaoGateway),
                new ConcluirReparoUseCase(execucaoGateway),
                new CancelarExecucaoUseCase(execucaoGateway));
    }

    @Produces
    StatusController statusController() {
        return new StatusController(new ConsultarStatusUseCase());
    }

    @Produces
    @RequestScoped
    ServicoPresenterAdapter servicoPresenter() {
        return new ServicoPresenterAdapter();
    }

    @Produces
    @RequestScoped
    PecaPresenterAdapter pecaPresenter() {
        return new PecaPresenterAdapter();
    }

    @Produces
    @RequestScoped
    SaldoEstoquePresenterAdapter saldoEstoquePresenter() {
        return new SaldoEstoquePresenterAdapter();
    }

    @Produces
    @RequestScoped
    MovimentoEstoquePresenterAdapter movimentoEstoquePresenter() {
        return new MovimentoEstoquePresenterAdapter();
    }

    @Produces
    @RequestScoped
    ExecucaoPresenterAdapter execucaoPresenter() {
        return new ExecucaoPresenterAdapter();
    }

    @Produces
    @RequestScoped
    FilaExecucaoPresenterAdapter filaExecucaoPresenter() {
        return new FilaExecucaoPresenterAdapter();
    }

    @Produces
    @RequestScoped
    StatusPresenterAdapter statusPresenter() {
        return new StatusPresenterAdapter();
    }
}
