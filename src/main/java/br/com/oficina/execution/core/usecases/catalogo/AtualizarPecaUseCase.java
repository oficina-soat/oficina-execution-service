package br.com.oficina.execution.core.usecases.catalogo;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.core.interfaces.gateway.CatalogoGateway;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AtualizarPecaUseCase {
    private final CatalogoGateway catalogoGateway;

    public AtualizarPecaUseCase(CatalogoGateway catalogoGateway) {
        this.catalogoGateway = catalogoGateway;
    }

    public CompletableFuture<Peca> executar(Command command) {
        return catalogoGateway.atualizarPeca(
                command.pecaId(),
                command.nome(),
                command.codigo(),
                command.valorUnitario());
    }

    public record Command(UUID pecaId, String nome, String codigo, BigDecimal valorUnitario) {
    }
}
