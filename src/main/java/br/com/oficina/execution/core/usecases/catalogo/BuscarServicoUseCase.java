package br.com.oficina.execution.core.usecases.catalogo;

import br.com.oficina.execution.core.entities.catalogo.Servico;
import br.com.oficina.execution.core.interfaces.gateway.CatalogoGateway;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BuscarServicoUseCase {
    private final CatalogoGateway catalogoGateway;

    public BuscarServicoUseCase(CatalogoGateway catalogoGateway) {
        this.catalogoGateway = catalogoGateway;
    }

    public CompletableFuture<Servico> executar(UUID servicoId) {
        return catalogoGateway.buscarServico(servicoId);
    }
}
