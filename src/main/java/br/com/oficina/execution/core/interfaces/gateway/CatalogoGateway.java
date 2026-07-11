package br.com.oficina.execution.core.interfaces.gateway;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.core.entities.catalogo.Servico;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CatalogoGateway {
    CompletableFuture<Servico> criarServico(String nome, String descricao, BigDecimal valorBase);

    CompletableFuture<List<Servico>> listarServicos();

    CompletableFuture<Servico> buscarServico(UUID servicoId);

    CompletableFuture<Servico> atualizarServico(UUID servicoId, String nome, String descricao, BigDecimal valorBase);

    CompletableFuture<Peca> criarPeca(String nome, String codigo, BigDecimal valorUnitario);

    CompletableFuture<List<Peca>> listarPecas();

    CompletableFuture<Peca> buscarPeca(UUID pecaId);

    CompletableFuture<Peca> atualizarPeca(UUID pecaId, String nome, String codigo, BigDecimal valorUnitario);
}
