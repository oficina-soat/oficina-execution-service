package br.com.oficina.execution.interfaces.controllers;

import br.com.oficina.execution.core.entities.estoque.Estoque;
import br.com.oficina.execution.core.entities.estoque.MovimentoEstoque;
import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import br.com.oficina.execution.core.usecases.estoque.ConsultarSaldoEstoqueUseCase;
import br.com.oficina.execution.core.usecases.estoque.ListarMovimentosEstoqueUseCase;
import br.com.oficina.execution.core.usecases.estoque.RegistrarMovimentoEstoqueUseCase;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EstoqueController {
    private final ConsultarSaldoEstoqueUseCase consultarSaldoEstoqueUseCase;
    private final ListarMovimentosEstoqueUseCase listarMovimentosEstoqueUseCase;
    private final RegistrarMovimentoEstoqueUseCase registrarMovimentoEstoqueUseCase;

    public EstoqueController(
            ConsultarSaldoEstoqueUseCase consultarSaldoEstoqueUseCase,
            ListarMovimentosEstoqueUseCase listarMovimentosEstoqueUseCase,
            RegistrarMovimentoEstoqueUseCase registrarMovimentoEstoqueUseCase) {
        this.consultarSaldoEstoqueUseCase = consultarSaldoEstoqueUseCase;
        this.listarMovimentosEstoqueUseCase = listarMovimentosEstoqueUseCase;
        this.registrarMovimentoEstoqueUseCase = registrarMovimentoEstoqueUseCase;
    }

    public CompletableFuture<Estoque> consultarSaldo(UUID pecaId) {
        return consultarSaldoEstoqueUseCase.executar(pecaId);
    }

    public CompletableFuture<List<MovimentoEstoque>> consultarMovimentos(UUID pecaId, UUID ordemServicoId) {
        return listarMovimentosEstoqueUseCase.executar(new ListarMovimentosEstoqueUseCase.Command(
                pecaId,
                ordemServicoId));
    }

    public CompletableFuture<MovimentoEstoque> registrarMovimento(
            TipoMovimentoEstoque tipo,
            MovimentoEstoqueRequest request,
            String correlationId) {
        return registrarMovimentoEstoqueUseCase.executar(new RegistrarMovimentoEstoqueUseCase.Command(
                tipo,
                request.pecaId(),
                request.ordemServicoId(),
                request.quantidade(),
                request.motivo(),
                correlationId));
    }

    public record MovimentoEstoqueRequest(
            UUID pecaId,
            UUID ordemServicoId,
            int quantidade,
            String motivo) {
    }
}
