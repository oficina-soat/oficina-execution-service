package br.com.oficina.execution.framework.dynamodb;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.core.entities.catalogo.Servico;
import br.com.oficina.execution.core.entities.estoque.Estoque;
import br.com.oficina.execution.core.entities.estoque.MovimentoEstoque;
import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus;
import br.com.oficina.execution.framework.dynamodb.OutboxEventRecord.OutboxStatus;
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
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class DynamoDbExecutionStore {
    public static final UUID SEED_PECA_ID = UUID.fromString("19fdd9ab-cf1f-4074-a96b-80ae86fba7b0");
    public static final UUID SEED_PNEU_ID = UUID.fromString("9fc69d25-1ed0-40dd-a02f-4c37e41f0bd6");
    public static final UUID SEED_TAPETE_ID = UUID.fromString("e522d846-12fb-4c42-8a68-914f4cb5a044");
    public static final UUID SEED_SERVICO_ID = UUID.fromString("b96e7e7f-b1f7-4c55-b42a-61c53ab06caa");
    public static final UUID SEED_ORDEM_SERVICO_ID = UUID.fromString("d290f1ee-6c54-4b01-90e6-d701748f0851");

    private final DynamoDbTableNames tableNames;
    private final LinkedHashMap<UUID, Peca> pecas = new LinkedHashMap<>();
    private final LinkedHashMap<String, UUID> pecaIdsPorCodigo = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, Servico> servicos = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, Estoque> saldos = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, Execucao> execucoes = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, UUID> execucaoIdPorOrdemServico = new LinkedHashMap<>();
    private final List<MovimentoEstoque> movimentos = new ArrayList<>();
    private final LinkedHashMap<String, DynamoDbItem> catalogoItems = new LinkedHashMap<>();
    private final LinkedHashMap<String, DynamoDbItem> estoqueItems = new LinkedHashMap<>();
    private final LinkedHashMap<String, DynamoDbItem> execucaoItems = new LinkedHashMap<>();
    private final LinkedHashMap<String, DynamoDbItem> outboxItems = new LinkedHashMap<>();
    private final LinkedHashMap<String, DynamoDbItem> idempotenciaItems = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, OutboxEventRecord> outbox = new LinkedHashMap<>();
    private final LinkedHashMap<String, IdempotencyRecord> idempotencias = new LinkedHashMap<>();

    public DynamoDbExecutionStore(DynamoDbTableNames tableNames) {
        this.tableNames = tableNames;
        aplicarSeedLimpo();
    }

    public synchronized void aplicarSeedLimpo() {
        for (var peca : ExecutionDynamoDbSeedData.pecas()) {
            salvarPeca(new Peca(peca.pecaId(), peca.nome(), peca.codigo(), peca.valorUnitario(), ExecutionDynamoDbSeedData.SEED_TIME));
        }
        for (var servico : ExecutionDynamoDbSeedData.servicos()) {
            salvarServico(new Servico(servico.servicoId(), servico.nome(), servico.descricao(), servico.valorBase(), ExecutionDynamoDbSeedData.SEED_TIME));
        }
        for (var saldo : ExecutionDynamoDbSeedData.saldos()) {
            salvarSaldo(new Estoque(saldo.pecaId(), saldo.quantidadeDisponivel(), saldo.quantidadeReservada(), ExecutionDynamoDbSeedData.SEED_TIME));
        }
    }

    public synchronized Servico criarServico(String nome, String descricao, BigDecimal valorBase) {
        var agora = agora();
        var servico = new Servico(UUID.randomUUID(), nome, descricao, valorBase, agora);
        salvarServico(servico);
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
        salvarServico(servico);
        return servico;
    }

    public synchronized Peca criarPeca(String nome, String codigo, BigDecimal valorUnitario) {
        var agora = agora();
        var codigoNormalizado = normalizarCodigo(codigo);
        exigirCodigoDisponivel(codigoNormalizado, null);
        var peca = new Peca(UUID.randomUUID(), nome, codigoNormalizado, valorUnitario, agora);
        salvarPeca(peca);
        salvarSaldo(new Estoque(peca.pecaId(), 0, 0, agora));
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
        salvarPeca(peca);
        return peca;
    }

    public synchronized Estoque buscarSaldo(UUID pecaId) {
        buscarPeca(pecaId);
        return saldos.computeIfAbsent(pecaId, id -> {
            var saldo = new Estoque(id, 0, 0, agora());
            salvarSaldo(saldo);
            return saldo;
        });
    }

    public synchronized List<MovimentoEstoque> listarMovimentos(UUID pecaId, UUID ordemServicoId) {
        return movimentos.stream()
                .filter(movimento -> pecaId == null || movimento.pecaId().equals(pecaId))
                .filter(movimento -> ordemServicoId == null || ordemServicoId.equals(movimento.ordemServicoId()))
                .sorted(Comparator.comparing(MovimentoEstoque::criadoEm))
                .toList();
    }

    public synchronized MovimentoEstoque registrarMovimento(TipoMovimentoEstoque tipo, UUID pecaId, UUID ordemServicoId, int quantidade, String motivo) {
        return registrarMovimento(tipo, pecaId, ordemServicoId, quantidade, motivo, "local");
    }

    public synchronized MovimentoEstoque registrarMovimento(
            TipoMovimentoEstoque tipo,
            UUID pecaId,
            UUID ordemServicoId,
            int quantidade,
            String motivo,
            String correlationId) {
        var saldo = buscarSaldo(pecaId);
        var movimento = saldo.registrar(tipo, quantidade, ordemServicoId, motivo, agora());
        movimentos.add(movimento);
        salvarSaldo(saldo);
        salvarMovimento(movimento);
        registrarOutboxEstoque(movimento, correlationId);
        return movimento;
    }

    public synchronized Execucao criarExecucao(UUID ordemServicoId) {
        return criarExecucao(ordemServicoId, Execucao.PRIORIDADE_PADRAO);
    }

    public synchronized Execucao criarExecucao(UUID ordemServicoId, Integer prioridade) {
        if (ordemServicoId == null) {
            throw new IllegalArgumentException("ordemServicoId e obrigatorio.");
        }
        if (execucaoIdPorOrdemServico.containsKey(ordemServicoId)) {
            throw new WebApplicationException("Ja existe execucao para a ordem de servico: " + ordemServicoId, Response.Status.CONFLICT);
        }
        var agora = agora();
        var execucao = new Execucao(UUID.randomUUID(), ordemServicoId, prioridadeOuPadrao(prioridade), agora);
        salvarExecucao(execucao);
        salvarHistorico(execucao, null, StatusExecucao.CRIADA, "Execucao criada", null, agora);
        return execucao;
    }

    public synchronized Execucao criarExecucaoSeAusente(UUID ordemServicoId) {
        var execucaoId = execucaoIdPorOrdemServico.get(ordemServicoId);
        return execucaoId == null ? criarExecucao(ordemServicoId) : buscarExecucao(execucaoId);
    }

    public synchronized List<Execucao> listarExecucoes(StatusExecucao status) {
        return execucoes.values().stream()
                .filter(execucao -> status == null || execucao.status() == status)
                .sorted(Comparator.comparing(Execucao::criadoEm))
                .toList();
    }

    public synchronized List<Execucao> listarFilaExecucao(StatusExecucao status) {
        return execucoes.values().stream()
                .filter(execucao -> status == null ? statusEmFila(execucao.status()) : execucao.status() == status && statusEmFila(status))
                .sorted(ordenacaoFila())
                .toList();
    }

    public synchronized Execucao buscarExecucao(UUID execucaoId) {
        var execucao = execucoes.get(execucaoId);
        if (execucao == null) {
            throw new NotFoundException("Execucao nao encontrada: " + execucaoId);
        }
        return execucao;
    }

    public synchronized Execucao buscarExecucaoDaOrdemServico(UUID ordemServicoId) {
        var execucaoId = execucaoIdPorOrdemServico.get(ordemServicoId);
        if (execucaoId == null) {
            throw new NotFoundException("Execucao nao encontrada para a ordem de servico: " + ordemServicoId);
        }
        return buscarExecucao(execucaoId);
    }

    public synchronized Execucao iniciarDiagnostico(UUID execucaoId, String correlationId) {
        var execucao = buscarExecucao(execucaoId);
        var statusAnterior = execucao.status();
        execucao.iniciarDiagnostico(agora());
        salvarExecucaoEEvento(execucao, statusAnterior, "Diagnostico iniciado", "diagnosticoIniciado", "oficina.execution.diagnostico-iniciado", payloadDiagnosticoIniciado(execucao), correlationId);
        return execucao;
    }

    public synchronized Execucao concluirDiagnostico(UUID execucaoId, String diagnostico, String correlationId) {
        var execucao = buscarExecucao(execucaoId);
        var statusAnterior = execucao.status();
        execucao.concluirDiagnostico(diagnostico, agora());
        salvarExecucaoEEvento(execucao, statusAnterior, "Diagnostico concluido", "diagnosticoFinalizado", "oficina.execution.diagnostico-finalizado", payloadDiagnosticoFinalizado(execucao), correlationId);
        return execucao;
    }

    public synchronized Execucao iniciarReparo(UUID execucaoId, String correlationId) {
        var execucao = buscarExecucao(execucaoId);
        var statusAnterior = execucao.status();
        execucao.iniciarReparo(agora());
        salvarExecucaoEEvento(execucao, statusAnterior, "Reparo iniciado", "execucaoIniciada", "oficina.execution.execucao-iniciada", payloadExecucaoIniciada(execucao), correlationId);
        return execucao;
    }

    public synchronized Execucao concluirReparo(UUID execucaoId, String observacoes, String correlationId) {
        var execucao = buscarExecucao(execucaoId);
        var statusAnterior = execucao.status();
        execucao.concluirReparo(observacoes, agora());
        salvarExecucaoEEvento(execucao, statusAnterior, "Reparo concluido", "execucaoFinalizada", "oficina.execution.execucao-finalizada", payloadExecucaoFinalizada(execucao), correlationId);
        return execucao;
    }

    public synchronized Execucao cancelarExecucao(UUID execucaoId, String motivo) {
        var execucao = buscarExecucao(execucaoId);
        var statusAnterior = execucao.status();
        execucao.cancelar(motivo, agora());
        salvarExecucao(execucao);
        salvarHistorico(execucao, statusAnterior, execucao.status(), "Execucao cancelada", null, execucao.atualizadoEm());
        return execucao;
    }

    public synchronized OutboxEventRecord registrarOutbox(
            String eventType,
            String topic,
            String aggregateId,
            Map<String, Object> payload,
            String correlationId) {
        var agora = agora();
        var event = new OutboxEventRecord(
                UUID.randomUUID(),
                eventType,
                1,
                topic,
                "oficina-execution-service",
                aggregateId,
                payload,
                OutboxStatus.PENDING,
                0,
                agora,
                null,
                null,
                correlationId,
                agora,
                agora);
        outbox.put(event.eventId(), event);
        put(outboxItems, toItem(event));
        return event;
    }

    public synchronized IdempotencyRecord registrarIdempotencia(
            String scope,
            String key,
            String requestHash,
            Integer responseStatus,
            String responseBody,
            ProcessingStatus status) {
        var agora = agora();
        var record = new IdempotencyRecord(scope, key, requestHash, responseStatus, responseBody, status, agora, agora, agora.plusDays(1));
        idempotencias.put(scope + "#" + key, record);
        put(idempotenciaItems, toItem(record));
        return record;
    }

    public synchronized boolean idempotenciaExiste(String scope, String key) {
        return idempotencias.containsKey(scope + "#" + key);
    }

    public synchronized List<DynamoDbItem> catalogoItems() {
        return List.copyOf(catalogoItems.values());
    }

    public synchronized List<DynamoDbItem> estoqueItems() {
        return List.copyOf(estoqueItems.values());
    }

    public synchronized List<DynamoDbItem> execucaoItems() {
        return List.copyOf(execucaoItems.values());
    }

    public synchronized List<DynamoDbItem> outboxItems() {
        return List.copyOf(outboxItems.values());
    }

    public synchronized List<DynamoDbItem> idempotenciaItems() {
        return List.copyOf(idempotenciaItems.values());
    }

    public synchronized List<OutboxEventRecord> outboxEvents() {
        return List.copyOf(outbox.values());
    }

    private void salvarServico(Servico servico) {
        servicos.put(servico.servicoId(), servico);
        put(catalogoItems, toItem(servico));
    }

    private void salvarPeca(Peca peca) {
        pecas.put(peca.pecaId(), peca);
        pecaIdsPorCodigo.put(peca.codigo(), peca.pecaId());
        put(catalogoItems, toItem(peca));
    }

    private void salvarSaldo(Estoque saldo) {
        saldos.put(saldo.pecaId(), saldo);
        put(estoqueItems, toItem(saldo));
    }

    private void salvarMovimento(MovimentoEstoque movimento) {
        put(estoqueItems, toItem(movimento));
    }

    private void salvarExecucaoEEvento(
            Execucao execucao,
            StatusExecucao statusAnterior,
            String descricao,
            String eventType,
            String topic,
            Map<String, Object> payload,
            String correlationId) {
        salvarExecucao(execucao);
        salvarHistorico(execucao, statusAnterior, execucao.status(), descricao, null, execucao.atualizadoEm());
        registrarOutbox(eventType, topic, execucao.ordemServicoId().toString(), payload, correlationId(correlationId));
    }

    private void salvarExecucao(Execucao execucao) {
        execucoes.put(execucao.execucaoId(), execucao);
        execucaoIdPorOrdemServico.put(execucao.ordemServicoId(), execucao.execucaoId());
        put(execucaoItems, toItem(execucao));
    }

    private void salvarHistorico(
            Execucao execucao,
            StatusExecucao statusAnterior,
            StatusExecucao statusNovo,
            String descricao,
            String sourceEventId,
            OffsetDateTime criadoEm) {
        var historicoId = UUID.randomUUID();
        put(execucaoItems, new DynamoDbItem(
                tableNames.execucoes(),
                "EXECUCAO#" + execucao.execucaoId(),
                "HISTORICO#" + criadoEm + "#" + historicoId,
                "EXECUCAO_HISTORICO",
                attributes(
                        "historicoId", historicoId,
                        "execucaoId", execucao.execucaoId(),
                        "ordemServicoId", execucao.ordemServicoId(),
                        "statusAnterior", statusAnterior,
                        "statusNovo", statusNovo,
                        "descricao", descricao,
                        "createdAt", criadoEm,
                        "sourceEventId", sourceEventId)));
    }

    private void registrarOutboxEstoque(MovimentoEstoque movimento, String correlationId) {
        if (movimento.tipo() == TipoMovimentoEstoque.ENTRADA) {
            registrarOutbox(
                    "estoqueAcrescentado",
                    "oficina.execution.estoque-acrescentado",
                    movimento.pecaId().toString(),
                    payloadEstoque(movimento),
                    correlationId(correlationId));
            return;
        }
        if (movimento.tipo() == TipoMovimentoEstoque.RESERVA || movimento.tipo() == TipoMovimentoEstoque.CONSUMO) {
            registrarOutbox(
                    "estoqueBaixado",
                    "oficina.execution.estoque-baixado",
                    movimento.pecaId().toString(),
                    payloadEstoque(movimento),
                    correlationId(correlationId));
        }
    }

    private Map<String, Object> payloadDiagnosticoIniciado(Execucao execucao) {
        return attributes(
                "execucaoId", execucao.execucaoId(),
                "ordemServicoId", execucao.ordemServicoId(),
                "statusExecucao", execucao.status(),
                "iniciadoEm", execucao.atualizadoEm());
    }

    private Map<String, Object> payloadDiagnosticoFinalizado(Execucao execucao) {
        return attributes(
                "execucaoId", execucao.execucaoId(),
                "ordemServicoId", execucao.ordemServicoId(),
                "statusExecucao", execucao.status(),
                "diagnostico", execucao.diagnostico(),
                "servicos", List.of(),
                "pecas", List.of(),
                "finalizadoEm", execucao.atualizadoEm());
    }

    private Map<String, Object> payloadExecucaoIniciada(Execucao execucao) {
        return attributes(
                "execucaoId", execucao.execucaoId(),
                "ordemServicoId", execucao.ordemServicoId(),
                "statusExecucao", execucao.status(),
                "iniciadaEm", execucao.atualizadoEm());
    }

    private Map<String, Object> payloadExecucaoFinalizada(Execucao execucao) {
        return attributes(
                "execucaoId", execucao.execucaoId(),
                "ordemServicoId", execucao.ordemServicoId(),
                "statusExecucao", execucao.status(),
                "observacoes", execucao.observacoesReparo(),
                "finalizadaEm", execucao.atualizadoEm());
    }

    private Map<String, Object> payloadEstoque(MovimentoEstoque movimento) {
        return attributes(
                "movimentoId", movimento.movimentoId(),
                "pecaId", movimento.pecaId(),
                "ordemServicoId", movimento.ordemServicoId(),
                "tipo", movimento.tipo(),
                "quantidade", movimento.quantidade(),
                "observacao", movimento.motivo(),
                "movimentadoEm", movimento.criadoEm());
    }

    private DynamoDbItem toItem(Execucao execucao) {
        return new DynamoDbItem(
                tableNames.execucoes(),
                "EXECUCAO#" + execucao.execucaoId(),
                "METADATA",
                "EXECUCAO",
                attributes(
                        "execucaoId", execucao.execucaoId(),
                        "ordemServicoId", execucao.ordemServicoId(),
                        "status", execucao.status(),
                        "prioridade", execucao.prioridade(),
                        "diagnostico", execucao.diagnostico(),
                        "observacoesReparo", execucao.observacoesReparo(),
                        "createdAt", execucao.criadoEm(),
                        "updatedAt", execucao.atualizadoEm(),
                        "correlationId", "local",
                        "filaStatus", filaStatus(execucao),
                        "prioridadeCriadoEm", prioridadeCriadoEm(execucao)));
    }

    private DynamoDbItem toItem(Servico servico) {
        return new DynamoDbItem(
                tableNames.catalogo(),
                "SERVICO#" + servico.servicoId(),
                "METADATA",
                "SERVICO",
                attributes(
                        "servicoId", servico.servicoId(),
                        "nome", servico.nome(),
                        "nomeNormalizado", servico.nome().toUpperCase(),
                        "descricao", servico.descricao(),
                        "valorBase", servico.valorBase(),
                        "ativo", servico.ativo(),
                        "createdAt", servico.criadoEm(),
                        "updatedAt", servico.atualizadoEm()));
    }

    private DynamoDbItem toItem(Peca peca) {
        return new DynamoDbItem(
                tableNames.catalogo(),
                "PECA#" + peca.pecaId(),
                "METADATA",
                "PECA",
                attributes(
                        "pecaId", peca.pecaId(),
                        "nome", peca.nome(),
                        "nomeNormalizado", peca.nome().toUpperCase(),
                        "codigo", peca.codigo(),
                        "valorUnitario", peca.valorUnitario(),
                        "ativo", peca.ativo(),
                        "createdAt", peca.criadoEm(),
                        "updatedAt", peca.atualizadoEm()));
    }

    private DynamoDbItem toItem(Estoque saldo) {
        return new DynamoDbItem(
                tableNames.estoque(),
                "PECA#" + saldo.pecaId(),
                "SALDO",
                "ESTOQUE_SALDO",
                attributes(
                        "pecaId", saldo.pecaId(),
                        "quantidadeDisponivel", saldo.quantidadeDisponivel(),
                        "quantidadeReservada", saldo.quantidadeReservada(),
                        "updatedAt", saldo.atualizadoEm()));
    }

    private DynamoDbItem toItem(MovimentoEstoque movimento) {
        return new DynamoDbItem(
                tableNames.estoque(),
                "PECA#" + movimento.pecaId(),
                "MOVIMENTO#" + movimento.criadoEm() + "#" + movimento.movimentoId(),
                "ESTOQUE_MOVIMENTO",
                attributes(
                        "movimentoId", movimento.movimentoId(),
                        "pecaId", movimento.pecaId(),
                        "ordemServicoId", movimento.ordemServicoId(),
                        "tipo", movimento.tipo(),
                        "quantidade", movimento.quantidade(),
                        "motivo", movimento.motivo(),
                        "createdAt", movimento.criadoEm()));
    }

    private DynamoDbItem toItem(OutboxEventRecord event) {
        return new DynamoDbItem(
                tableNames.outbox(),
                "OUTBOX#" + event.eventId(),
                "EVENT",
                "OUTBOX_EVENT",
                attributes(
                        "eventId", event.eventId(),
                        "eventType", event.eventType(),
                        "eventVersion", event.eventVersion(),
                        "topic", event.topic(),
                        "producer", event.producer(),
                        "aggregateId", event.aggregateId(),
                        "payload", event.payload(),
                        "status", event.status(),
                        "attempts", event.attempts(),
                        "nextAttemptAt", event.nextAttemptAt(),
                        "correlationId", event.correlationId(),
                        "createdAt", event.createdAt(),
                        "updatedAt", event.updatedAt()));
    }

    private DynamoDbItem toItem(IdempotencyRecord record) {
        return new DynamoDbItem(
                tableNames.idempotencia(),
                "IDEMPOTENCY#" + record.scope() + "#" + record.key(),
                "REQUEST",
                "IDEMPOTENCY",
                attributes(
                        "scope", record.scope(),
                        "key", record.key(),
                        "requestHash", record.requestHash(),
                        "responseStatus", record.responseStatus(),
                        "responseBody", record.responseBody(),
                        "processingStatus", record.processingStatus(),
                        "createdAt", record.createdAt(),
                        "updatedAt", record.updatedAt(),
                        "expiresAt", record.expiresAt()));
    }

    private Map<String, Object> attributes(Object... entries) {
        var attributes = new LinkedHashMap<String, Object>();
        for (var i = 0; i < entries.length; i += 2) {
            var value = entries[i + 1];
            if (value != null) {
                attributes.put((String) entries[i], value);
            }
        }
        return attributes;
    }

    private void put(LinkedHashMap<String, DynamoDbItem> items, DynamoDbItem item) {
        items.put(item.key(), item);
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

    private int prioridadeOuPadrao(Integer prioridade) {
        return prioridade == null ? Execucao.PRIORIDADE_PADRAO : prioridade;
    }

    private Comparator<Execucao> ordenacaoFila() {
        return Comparator.comparingInt(Execucao::prioridade)
                .thenComparing(Execucao::criadoEm)
                .thenComparing(Execucao::execucaoId);
    }

    private boolean statusEmFila(StatusExecucao status) {
        return status == StatusExecucao.CRIADA || status == StatusExecucao.DIAGNOSTICO_CONCLUIDO;
    }

    private String filaStatus(Execucao execucao) {
        return statusEmFila(execucao.status()) ? execucao.status().name() : null;
    }

    private String prioridadeCriadoEm(Execucao execucao) {
        return statusEmFila(execucao.status())
                ? "%010d#%s#%s".formatted(execucao.prioridade(), execucao.criadoEm(), execucao.execucaoId())
                : null;
    }

    private String correlationId(String correlationId) {
        return correlationId == null || correlationId.isBlank() ? "local-" + UUID.randomUUID() : correlationId.trim();
    }

    private OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
