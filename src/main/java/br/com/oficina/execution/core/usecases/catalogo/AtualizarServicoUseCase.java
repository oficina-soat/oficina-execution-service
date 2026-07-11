package br.com.oficina.execution.core.usecases.catalogo;

import br.com.oficina.execution.core.entities.catalogo.Servico;
import br.com.oficina.execution.core.interfaces.gateway.CatalogoGateway;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AtualizarServicoUseCase {
    private final CatalogoGateway catalogoGateway;

    public AtualizarServicoUseCase(CatalogoGateway catalogoGateway) {
        this.catalogoGateway = catalogoGateway;
    }

    public CompletableFuture<Servico> executar(Command command) {
        return catalogoGateway.atualizarServico(
                command.servicoId(),
                command.nome(),
                command.descricao(),
                command.valorBase());
    }

    public record Command(UUID servicoId, String nome, String descricao, BigDecimal valorBase) {
    }
}
