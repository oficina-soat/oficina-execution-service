package br.com.oficina.execution.core.usecases.catalogo;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.core.interfaces.gateway.CatalogoGateway;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class CriarPecaUseCase {
    private final CatalogoGateway catalogoGateway;

    public CriarPecaUseCase(CatalogoGateway catalogoGateway) {
        this.catalogoGateway = catalogoGateway;
    }

    public CompletableFuture<Peca> executar(Command command) {
        return catalogoGateway.criarPeca(command.nome(), command.codigo(), command.valorUnitario());
    }

    public record Command(String nome, String codigo, BigDecimal valorUnitario) {
    }
}
