package br.com.oficina.execution.core.usecases.catalogo;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.core.interfaces.gateway.CatalogoGateway;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ListarPecasUseCase {
    private final CatalogoGateway catalogoGateway;

    public ListarPecasUseCase(CatalogoGateway catalogoGateway) {
        this.catalogoGateway = catalogoGateway;
    }

    public CompletableFuture<List<Peca>> executar() {
        return catalogoGateway.listarPecas();
    }
}
