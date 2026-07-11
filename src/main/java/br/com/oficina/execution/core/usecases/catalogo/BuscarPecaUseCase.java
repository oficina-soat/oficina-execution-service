package br.com.oficina.execution.core.usecases.catalogo;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.core.interfaces.gateway.CatalogoGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BuscarPecaUseCase {
    private final CatalogoGateway catalogoGateway;

    public BuscarPecaUseCase(CatalogoGateway catalogoGateway) {
        this.catalogoGateway = catalogoGateway;
    }

    public CompletableFuture<Peca> executar(UUID pecaId) {
        return catalogoGateway.buscarPeca(pecaId);
    }
}
