package br.com.oficina.execution.interfaces.controllers;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.core.entities.catalogo.Servico;
import br.com.oficina.execution.core.usecases.catalogo.AtualizarPecaUseCase;
import br.com.oficina.execution.core.usecases.catalogo.AtualizarServicoUseCase;
import br.com.oficina.execution.core.usecases.catalogo.BuscarPecaUseCase;
import br.com.oficina.execution.core.usecases.catalogo.BuscarServicoUseCase;
import br.com.oficina.execution.core.usecases.catalogo.CriarPecaUseCase;
import br.com.oficina.execution.core.usecases.catalogo.CriarServicoUseCase;
import br.com.oficina.execution.core.usecases.catalogo.ListarPecasUseCase;
import br.com.oficina.execution.core.usecases.catalogo.ListarServicosUseCase;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CatalogoController {
    private final CriarServicoUseCase criarServicoUseCase;
    private final ListarServicosUseCase listarServicosUseCase;
    private final BuscarServicoUseCase buscarServicoUseCase;
    private final AtualizarServicoUseCase atualizarServicoUseCase;
    private final CriarPecaUseCase criarPecaUseCase;
    private final ListarPecasUseCase listarPecasUseCase;
    private final BuscarPecaUseCase buscarPecaUseCase;
    private final AtualizarPecaUseCase atualizarPecaUseCase;

    public CatalogoController(
            CriarServicoUseCase criarServicoUseCase,
            ListarServicosUseCase listarServicosUseCase,
            BuscarServicoUseCase buscarServicoUseCase,
            AtualizarServicoUseCase atualizarServicoUseCase,
            CriarPecaUseCase criarPecaUseCase,
            ListarPecasUseCase listarPecasUseCase,
            BuscarPecaUseCase buscarPecaUseCase,
            AtualizarPecaUseCase atualizarPecaUseCase) {
        this.criarServicoUseCase = criarServicoUseCase;
        this.listarServicosUseCase = listarServicosUseCase;
        this.buscarServicoUseCase = buscarServicoUseCase;
        this.atualizarServicoUseCase = atualizarServicoUseCase;
        this.criarPecaUseCase = criarPecaUseCase;
        this.listarPecasUseCase = listarPecasUseCase;
        this.buscarPecaUseCase = buscarPecaUseCase;
        this.atualizarPecaUseCase = atualizarPecaUseCase;
    }

    public CompletableFuture<Servico> criarServico(ServicoRequest request) {
        return criarServicoUseCase.executar(new CriarServicoUseCase.Command(
                request.nome(),
                request.descricao(),
                request.valorBase()));
    }

    public CompletableFuture<List<Servico>> consultarServicos() {
        return listarServicosUseCase.executar();
    }

    public CompletableFuture<Servico> consultarServico(UUID servicoId) {
        return buscarServicoUseCase.executar(servicoId);
    }

    public CompletableFuture<Servico> atualizarServico(UUID servicoId, ServicoRequest request) {
        return atualizarServicoUseCase.executar(new AtualizarServicoUseCase.Command(
                servicoId,
                request.nome(),
                request.descricao(),
                request.valorBase()));
    }

    public CompletableFuture<Peca> criarPeca(PecaRequest request) {
        return criarPecaUseCase.executar(new CriarPecaUseCase.Command(
                request.nome(),
                request.codigo(),
                request.valorUnitario()));
    }

    public CompletableFuture<List<Peca>> consultarPecas() {
        return listarPecasUseCase.executar();
    }

    public CompletableFuture<Peca> consultarPeca(UUID pecaId) {
        return buscarPecaUseCase.executar(pecaId);
    }

    public CompletableFuture<Peca> atualizarPeca(UUID pecaId, PecaRequest request) {
        return atualizarPecaUseCase.executar(new AtualizarPecaUseCase.Command(
                pecaId,
                request.nome(),
                request.codigo(),
                request.valorUnitario()));
    }

    public record ServicoRequest(String nome, String descricao, BigDecimal valorBase) {
    }

    public record PecaRequest(String nome, String codigo, BigDecimal valorUnitario) {
    }
}
