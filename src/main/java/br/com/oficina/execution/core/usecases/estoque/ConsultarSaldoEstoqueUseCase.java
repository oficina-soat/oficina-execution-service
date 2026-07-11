package br.com.oficina.execution.core.usecases.estoque;

import br.com.oficina.execution.core.entities.estoque.Estoque;
import br.com.oficina.execution.core.interfaces.gateway.EstoqueGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConsultarSaldoEstoqueUseCase {
    private final EstoqueGateway estoqueGateway;

    public ConsultarSaldoEstoqueUseCase(EstoqueGateway estoqueGateway) {
        this.estoqueGateway = estoqueGateway;
    }

    public CompletableFuture<Estoque> executar(UUID pecaId) {
        return estoqueGateway.buscarSaldo(pecaId);
    }
}
