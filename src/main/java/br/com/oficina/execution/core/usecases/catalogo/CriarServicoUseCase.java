package br.com.oficina.execution.core.usecases.catalogo;

import br.com.oficina.execution.core.entities.catalogo.Servico;
import br.com.oficina.execution.core.interfaces.gateway.CatalogoGateway;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class CriarServicoUseCase {
    private final CatalogoGateway catalogoGateway;

    public CriarServicoUseCase(CatalogoGateway catalogoGateway) {
        this.catalogoGateway = catalogoGateway;
    }

    public CompletableFuture<Servico> executar(Command command) {
        return catalogoGateway.criarServico(command.nome(), command.descricao(), command.valorBase());
    }

    public record Command(String nome, String descricao, BigDecimal valorBase) {
    }
}
