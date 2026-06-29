package br.com.oficina.execution.framework.db;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.core.entities.catalogo.Servico;
import br.com.oficina.execution.core.entities.estoque.Estoque;
import br.com.oficina.execution.core.entities.estoque.MovimentoEstoque;
import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ExecutionSeedStore {
    public static final UUID SEED_PECA_ID = UUID.fromString("19fdd9ab-cf1f-4074-a96b-80ae86fba7b0");
    public static final UUID SEED_SERVICO_ID = UUID.fromString("b96e7e7f-b1f7-4c55-b42a-61c53ab06caa");
    public static final UUID SEED_ORDEM_SERVICO_ID = UUID.fromString("d290f1ee-6c54-4b01-90e6-d701748f0851");

    private final LinkedHashMap<UUID, Peca> pecas = new LinkedHashMap<>();
    private final LinkedHashMap<String, UUID> pecaIdsPorCodigo = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, Servico> servicos = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, Estoque> saldos = new LinkedHashMap<>();
    private final List<MovimentoEstoque> movimentos = new ArrayList<>();

    public ExecutionSeedStore() {
        var seedTime = OffsetDateTime.of(2026, 6, 23, 15, 30, 0, 0, ZoneOffset.UTC);
        var peca = new Peca(SEED_PECA_ID, "Volante", "VOL-001", new BigDecimal("50.00"), seedTime);
        pecas.put(peca.pecaId(), peca);
        pecaIdsPorCodigo.put(peca.codigo(), peca.pecaId());
        servicos.put(SEED_SERVICO_ID, new Servico(SEED_SERVICO_ID, "Troca de oleo", "Substituicao do oleo do motor", new BigDecimal("250.00"), seedTime));
        saldos.put(SEED_PECA_ID, new Estoque(SEED_PECA_ID, 10, 0, seedTime));
    }

    public synchronized Servico criarServico(String nome, String descricao, BigDecimal valorBase) {
        var agora = agora();
        var servico = new Servico(UUID.randomUUID(), nome, descricao, valorBase, agora);
        servicos.put(servico.servicoId(), servico);
        return servico;
    }

    public synchronized List<Servico> listarServicos() {
        return servicos.values().stream()
                .sorted(Comparator.comparing(Servico::criadoEm))
                .toList();
    }

    public synchronized Servico buscarServico(UUID servicoId) {
        var servico = servicos.get(servicoId);
        if (servico == null) {
            throw new NotFoundException("Servico nao encontrado: " + servicoId);
        }
        return servico;
    }

    public synchronized Servico atualizarServico(UUID servicoId, String nome, String descricao, BigDecimal valorBase) {
        var servico = buscarServico(servicoId);
        servico.atualizar(nome, descricao, valorBase, agora());
        return servico;
    }

    public synchronized Peca criarPeca(String nome, String codigo, BigDecimal valorUnitario) {
        var agora = agora();
        var codigoNormalizado = normalizarCodigo(codigo);
        exigirCodigoDisponivel(codigoNormalizado, null);
        var peca = new Peca(UUID.randomUUID(), nome, codigoNormalizado, valorUnitario, agora);
        pecas.put(peca.pecaId(), peca);
        pecaIdsPorCodigo.put(peca.codigo(), peca.pecaId());
        saldos.put(peca.pecaId(), new Estoque(peca.pecaId(), 0, 0, agora));
        return peca;
    }

    public synchronized List<Peca> listarPecas() {
        return pecas.values().stream()
                .sorted(Comparator.comparing(Peca::criadoEm))
                .toList();
    }

    public synchronized Peca buscarPeca(UUID pecaId) {
        var peca = pecas.get(pecaId);
        if (peca == null) {
            throw new NotFoundException("Peca nao encontrada: " + pecaId);
        }
        return peca;
    }

    public synchronized Peca atualizarPeca(UUID pecaId, String nome, String codigo, BigDecimal valorUnitario) {
        var peca = buscarPeca(pecaId);
        var codigoNormalizado = normalizarCodigo(codigo);
        exigirCodigoDisponivel(codigoNormalizado, pecaId);
        pecaIdsPorCodigo.remove(peca.codigo());
        peca.atualizar(nome, codigoNormalizado, valorUnitario, agora());
        pecaIdsPorCodigo.put(peca.codigo(), peca.pecaId());
        return peca;
    }

    public synchronized Estoque buscarSaldo(UUID pecaId) {
        buscarPeca(pecaId);
        return saldos.computeIfAbsent(pecaId, id -> new Estoque(id, 0, 0, agora()));
    }

    public synchronized List<MovimentoEstoque> listarMovimentos(UUID pecaId, UUID ordemServicoId) {
        return movimentos.stream()
                .filter(movimento -> pecaId == null || movimento.pecaId().equals(pecaId))
                .filter(movimento -> ordemServicoId == null || ordemServicoId.equals(movimento.ordemServicoId()))
                .sorted(Comparator.comparing(MovimentoEstoque::criadoEm))
                .toList();
    }

    public synchronized MovimentoEstoque registrarMovimento(TipoMovimentoEstoque tipo, UUID pecaId, UUID ordemServicoId, int quantidade, String motivo) {
        var saldo = buscarSaldo(pecaId);
        var movimento = saldo.registrar(tipo, quantidade, ordemServicoId, motivo, agora());
        movimentos.add(movimento);
        return movimento;
    }

    private void exigirCodigoDisponivel(String codigo, UUID pecaAtualId) {
        var pecaIdExistente = pecaIdsPorCodigo.get(codigo);
        if (pecaIdExistente != null && !pecaIdExistente.equals(pecaAtualId)) {
            throw new WebApplicationException("Codigo de peca ja cadastrado: " + codigo, Response.Status.CONFLICT);
        }
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Codigo da peca e obrigatorio.");
        }
        return codigo.trim().toUpperCase();
    }

    private OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
