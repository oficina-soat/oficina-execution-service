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
    private final ServicoUseCases servicos;
    private final PecaUseCases pecas;

    public CatalogoController(ServicoUseCases servicos, PecaUseCases pecas) {
        this.servicos = servicos;
        this.pecas = pecas;
    }

    public CompletableFuture<Servico> criarServico(ServicoRequest request) {
        return servicos.criar().executar(new CriarServicoUseCase.Command(
                request.nome(),
                request.descricao(),
                request.valorBase()));
    }

    public CompletableFuture<List<Servico>> consultarServicos() {
        return servicos.listar().executar();
    }

    public CompletableFuture<Servico> consultarServico(UUID servicoId) {
        return servicos.buscar().executar(servicoId);
    }

    public CompletableFuture<Servico> atualizarServico(UUID servicoId, ServicoRequest request) {
        return servicos.atualizar().executar(new AtualizarServicoUseCase.Command(
                servicoId,
                request.nome(),
                request.descricao(),
                request.valorBase()));
    }

    public CompletableFuture<Peca> criarPeca(PecaRequest request) {
        return pecas.criar().executar(new CriarPecaUseCase.Command(
                request.nome(),
                request.codigo(),
                request.valorUnitario()));
    }

    public CompletableFuture<List<Peca>> consultarPecas() {
        return pecas.listar().executar();
    }

    public CompletableFuture<Peca> consultarPeca(UUID pecaId) {
        return pecas.buscar().executar(pecaId);
    }

    public CompletableFuture<Peca> atualizarPeca(UUID pecaId, PecaRequest request) {
        return pecas.atualizar().executar(new AtualizarPecaUseCase.Command(
                pecaId,
                request.nome(),
                request.codigo(),
                request.valorUnitario()));
    }

    public record ServicoRequest(String nome, String descricao, BigDecimal valorBase) {
    }

    public record PecaRequest(String nome, String codigo, BigDecimal valorUnitario) {
    }

    public record ServicoUseCases(
            CriarServicoUseCase criar,
            ListarServicosUseCase listar,
            BuscarServicoUseCase buscar,
            AtualizarServicoUseCase atualizar) {
    }

    public record PecaUseCases(
            CriarPecaUseCase criar,
            ListarPecasUseCase listar,
            BuscarPecaUseCase buscar,
            AtualizarPecaUseCase atualizar) {
    }
}
