package br.com.oficina.execution.core.usecases.catalogo;

import br.com.oficina.execution.core.entities.catalogo.Servico;
import br.com.oficina.execution.core.interfaces.gateway.CatalogoGateway;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ListarServicosUseCase {
    private final CatalogoGateway catalogoGateway;

    public ListarServicosUseCase(CatalogoGateway catalogoGateway) {
        this.catalogoGateway = catalogoGateway;
    }

    public CompletableFuture<List<Servico>> executar() {
        return catalogoGateway.listarServicos();
    }
}
