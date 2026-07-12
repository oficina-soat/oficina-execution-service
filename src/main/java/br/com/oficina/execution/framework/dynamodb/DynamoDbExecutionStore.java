package br.com.oficina.execution.framework.dynamodb;

import br.com.oficina.execution.core.entities.catalogo.Peca;
import br.com.oficina.execution.core.entities.catalogo.Servico;
import br.com.oficina.execution.core.entities.estoque.Estoque;
import br.com.oficina.execution.core.entities.estoque.MovimentoEstoque;
import br.com.oficina.execution.core.entities.estoque.TipoMovimentoEstoque;
import br.com.oficina.execution.core.entities.execucao.Execucao;
import br.com.oficina.execution.core.entities.execucao.StatusExecucao;
import br.com.oficina.execution.core.exceptions.BusinessConflictException;
import br.com.oficina.execution.core.exceptions.ResourceNotFoundException;
import br.com.oficina.execution.framework.dynamodb.IdempotencyRecord.ProcessingStatus;
import br.com.oficina.execution.framework.dynamodb.OutboxEventRecord.OutboxStatus;
import br.com.oficina.execution.framework.observability.StructuredLog;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

@ApplicationScoped
public class DynamoDbExecutionStore {
    private static final Logger LOG = Logger.getLogger(DynamoDbExecutionStore.class);
    private static final String PRODUCER = "oficina-execution-service";
    public static final UUID SEED_PECA_ID = UUID.fromString("19fdd9ab-cf1f-4074-a96b-80ae86fba7b0");
    public static final UUID SEED_PNEU_ID = UUID.fromString("9fc69d25-1ed0-40dd-a02f-4c37e41f0bd6");
    public static final UUID SEED_TAPETE_ID = UUID.fromString("e522d846-12fb-4c42-8a68-914f4cb5a044");
    public static final UUID SEED_SERVICO_ID = UUID.fromString("b96e7e7f-b1f7-4c55-b42a-61c53ab06caa");
    public static final UUID SEED_ORDEM_SERVICO_ID = UUID.fromString("d290f1ee-6c54-4b01-90e6-d701748f0851");
    private static final String ATTR_EXECUCAO_ID = "execucaoId";
    private static final String ATTR_ORDEM_SERVICO_ID = "ordemServicoId";
    private static final String ATTR_STATUS_EXECUCAO = "statusExecucao";
    private static final String ATTR_PECA_ID = "pecaId";
    private static final String ATTR_CREATED_AT = "createdAt";
    private static final String ATTR_UPDATED_AT = "updatedAt";
    private static final String ATTR_CORRELATION_ID = "correlationId";
    private static final String ATTR_PK = "PK";
    private static final String ATTR_SK = "SK";
    private static final String ATTR_ENTITY_TYPE = "entityType";
    private static final String SORT_KEY_METADATA = "METADATA";
    private static final String KEY_PREFIX_PECA = "PECA#";
    private static final String KEY_PREFIX_SERVICO = "SERVICO#";
    private static final String KEY_PREFIX_EXECUCAO = "EXECUCAO#";

    private final DynamoDbTableNames tableNames;
    private final DynamoDbClient dynamoDbClient;

    public DynamoDbExecutionStore(DynamoDbTableNames tableNames, DynamoDbClient dynamoDbClient) {
        this.tableNames = tableNames;
        this.dynamoDbClient = dynamoDbClient;
        aplicarSeedLimpo();
    }

    public void aplicarSeedLimpo() {
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

    public Servico criarServico(String nome, String descricao, BigDecimal valorBase) {
        var agora = agora();
        var servico = new Servico(UUID.randomUUID(), nome, descricao, valorBase, agora);
        salvarServico(servico);
        return servico;
    }

    public List<Servico> listarServicos() {
        return catalogoItems().stream()
                .filter(item -> item.entityType().equals("SERVICO"))
                .map(this::toServico)
                .sorted(Comparator.comparing(Servico::criadoEm))
                .toList();
    }

    public Servico buscarServico(UUID servicoId) {
        return getItem(tableNames.catalogo(), KEY_PREFIX_SERVICO + servicoId, SORT_KEY_METADATA)
                .filter(item -> item.entityType().equals("SERVICO"))
                .map(this::toServico)
                .orElseThrow(() -> new ResourceNotFoundException("Servico nao encontrado: " + servicoId));
    }

    public Servico atualizarServico(UUID servicoId, String nome, String descricao, BigDecimal valorBase) {
        var servico = buscarServico(servicoId);
        servico.atualizar(nome, descricao, valorBase, agora());
        salvarServico(servico);
        return servico;
    }

    public Peca criarPeca(String nome, String codigo, BigDecimal valorUnitario) {
        var agora = agora();
        var codigoNormalizado = normalizarCodigo(codigo);
        exigirCodigoDisponivel(codigoNormalizado, null);
        var peca = new Peca(UUID.randomUUID(), nome, codigoNormalizado, valorUnitario, agora);
        var saldo = new Estoque(peca.pecaId(), 0, 0, agora);
        transactPut(toItem(peca), toItem(saldo));
        return peca;
    }

    public List<Peca> listarPecas() {
        return catalogoItems().stream()
                .filter(item -> item.entityType().equals("PECA"))
                .map(this::toPeca)
                .sorted(Comparator.comparing(Peca::criadoEm))
                .toList();
    }

    public Peca buscarPeca(UUID pecaId) {
        return getItem(tableNames.catalogo(), KEY_PREFIX_PECA + pecaId, SORT_KEY_METADATA)
                .filter(item -> item.entityType().equals("PECA"))
                .map(this::toPeca)
                .orElseThrow(() -> new ResourceNotFoundException("Peca nao encontrada: " + pecaId));
    }

    public Peca atualizarPeca(UUID pecaId, String nome, String codigo, BigDecimal valorUnitario) {
        var peca = buscarPeca(pecaId);
        var codigoNormalizado = normalizarCodigo(codigo);
        exigirCodigoDisponivel(codigoNormalizado, pecaId);
        peca.atualizar(nome, codigoNormalizado, valorUnitario, agora());
        salvarPeca(peca);
        return peca;
    }

    public Estoque buscarSaldo(UUID pecaId) {
        buscarPeca(pecaId);
        return getItem(tableNames.estoque(), KEY_PREFIX_PECA + pecaId, "SALDO")
                .filter(item -> item.entityType().equals("ESTOQUE_SALDO"))
                .map(this::toEstoque)
                .orElseGet(() -> {
                    var saldo = new Estoque(pecaId, 0, 0, agora());
                    salvarSaldo(saldo);
                    return saldo;
                });
    }

    public List<MovimentoEstoque> listarMovimentos(UUID pecaId, UUID ordemServicoId) {
        return estoqueItems().stream()
                .filter(item -> item.entityType().equals("ESTOQUE_MOVIMENTO"))
                .map(this::toMovimento)
                .filter(movimento -> pecaId == null || movimento.pecaId().equals(pecaId))
                .filter(movimento -> ordemServicoId == null || ordemServicoId.equals(movimento.ordemServicoId()))
                .sorted(Comparator.comparing(MovimentoEstoque::criadoEm))
                .toList();
    }

    public MovimentoEstoque registrarMovimento(TipoMovimentoEstoque tipo, UUID pecaId, UUID ordemServicoId, int quantidade, String motivo) {
        return registrarMovimento(tipo, pecaId, ordemServicoId, quantidade, motivo, "local");
    }

    public MovimentoEstoque registrarMovimento(
            TipoMovimentoEstoque tipo,
            UUID pecaId,
            UUID ordemServicoId,
            int quantidade,
            String motivo,
            String correlationId) {
        var saldo = buscarSaldo(pecaId);
        var movimento = saldo.registrar(tipo, quantidade, ordemServicoId, motivo, agora());
        var outbox = outboxEstoque(movimento, correlationId);
        if (outbox == null) {
            transactPut(toItem(saldo), toItem(movimento));
        } else {
            transactPut(toItem(saldo), toItem(movimento), toItem(outbox));
            logEvent("outbox event registered", outbox, "PENDING");
        }
        return movimento;
    }

    public Execucao criarExecucao(UUID ordemServicoId) {
        return criarExecucao(ordemServicoId, Execucao.PRIORIDADE_PADRAO);
    }

    public Execucao criarExecucao(UUID ordemServicoId, Integer prioridade) {
        if (ordemServicoId == null) {
            throw new IllegalArgumentException("ordemServicoId e obrigatorio.");
        }
        if (findExecucaoDaOrdemServico(ordemServicoId) != null) {
            throw new BusinessConflictException("Ja existe execucao para a ordem de servico: " + ordemServicoId);
        }
        var agora = agora();
        var execucao = new Execucao(UUID.randomUUID(), ordemServicoId, prioridadeOuPadrao(prioridade), agora);
        var historico = historico(execucao, null, StatusExecucao.CRIADA, "Execucao criada", null, agora);
        transactPut(toItem(execucao), historico);
        return execucao;
    }

    public Execucao criarExecucaoSeAusente(UUID ordemServicoId) {
        var execucao = findExecucaoDaOrdemServico(ordemServicoId);
        return execucao == null ? criarExecucao(ordemServicoId) : execucao;
    }

    public List<Execucao> listarExecucoes(StatusExecucao status) {
        return execucaoItems().stream()
                .filter(item -> item.entityType().equals("EXECUCAO"))
                .map(this::toExecucao)
                .filter(execucao -> status == null || execucao.status() == status)
                .sorted(Comparator.comparing(Execucao::criadoEm))
                .toList();
    }

    public List<Execucao> listarFilaExecucao(StatusExecucao status) {
        return execucaoItems().stream()
                .filter(item -> item.entityType().equals("EXECUCAO"))
                .map(this::toExecucao)
                .filter(execucao -> status == null ? statusEmFila(execucao.status()) : execucao.status() == status && statusEmFila(status))
                .sorted(ordenacaoFila())
                .toList();
    }

    public Execucao buscarExecucao(UUID execucaoId) {
        return getItem(tableNames.execucoes(), KEY_PREFIX_EXECUCAO + execucaoId, SORT_KEY_METADATA)
                .filter(item -> item.entityType().equals("EXECUCAO"))
                .map(this::toExecucao)
                .orElseThrow(() -> new ResourceNotFoundException("Execucao nao encontrada: " + execucaoId));
    }

    public Execucao buscarExecucaoDaOrdemServico(UUID ordemServicoId) {
        var execucao = findExecucaoDaOrdemServico(ordemServicoId);
        if (execucao == null) {
            throw new ResourceNotFoundException("Execucao nao encontrada para a ordem de servico: " + ordemServicoId);
        }
        return execucao;
    }

    public Execucao iniciarDiagnostico(UUID execucaoId, String correlationId) {
        var execucao = buscarExecucao(execucaoId);
        var statusAnterior = execucao.status();
        execucao.iniciarDiagnostico(agora());
        salvarExecucaoEEvento(execucao, statusAnterior, "Diagnostico iniciado", "diagnosticoIniciado", "oficina.execution.diagnostico-iniciado", payloadDiagnosticoIniciado(execucao), correlationId);
        return execucao;
    }

    public Execucao concluirDiagnostico(UUID execucaoId, String diagnostico, String correlationId) {
        var execucao = buscarExecucao(execucaoId);
        var statusAnterior = execucao.status();
        execucao.concluirDiagnostico(diagnostico, agora());
        salvarExecucaoEEvento(execucao, statusAnterior, "Diagnostico concluido", "diagnosticoFinalizado", "oficina.execution.diagnostico-finalizado", payloadDiagnosticoFinalizado(execucao), correlationId);
        return execucao;
    }

    public Execucao iniciarReparo(UUID execucaoId, String correlationId) {
        var execucao = buscarExecucao(execucaoId);
        var statusAnterior = execucao.status();
        execucao.iniciarReparo(agora());
        salvarExecucaoEEvento(execucao, statusAnterior, "Reparo iniciado", "execucaoIniciada", "oficina.execution.execucao-iniciada", payloadExecucaoIniciada(execucao), correlationId);
        return execucao;
    }

    public Execucao concluirReparo(UUID execucaoId, String observacoes, String correlationId) {
        var execucao = buscarExecucao(execucaoId);
        var statusAnterior = execucao.status();
        execucao.concluirReparo(observacoes, agora());
        salvarExecucaoEEvento(execucao, statusAnterior, "Reparo concluido", "execucaoFinalizada", "oficina.execution.execucao-finalizada", payloadExecucaoFinalizada(execucao), correlationId);
        return execucao;
    }

    public Execucao cancelarExecucao(UUID execucaoId, String motivo) {
        var execucao = buscarExecucao(execucaoId);
        var statusAnterior = execucao.status();
        execucao.cancelar(motivo, agora());
        var historico = historico(execucao, statusAnterior, execucao.status(), "Execucao cancelada", null, execucao.atualizadoEm());
        transactPut(toItem(execucao), historico);
        return execucao;
    }

    public OutboxEventRecord registrarOutbox(
            String eventType,
            String topic,
            String aggregateId,
            Map<String, Object> payload,
            String correlationId) {
        var event = outboxRecord(eventType, topic, aggregateId, payload, correlationId);
        put(toItem(event));
        logEvent("outbox event registered", event, "PENDING");
        return event;
    }

    public IdempotencyRecord registrarIdempotencia(
            String scope,
            String key,
            String requestHash,
            Integer responseStatus,
            String responseBody,
            ProcessingStatus status) {
        return registrarIdempotencia(
                scope,
                key,
                requestHash,
                responseStatus,
                responseBody,
                status,
                correlationId(null),
                null,
                agora().plusDays(1));
    }

    public IdempotencyRecord registrarIdempotencia(
            String scope,
            String key,
            String requestHash,
            Integer responseStatus,
            String responseBody,
            ProcessingStatus status,
            String correlationId,
            String requestId,
            OffsetDateTime expiresAt) {
        var agora = agora();
        var idempotencyRecord = new IdempotencyRecord(
                scope,
                key,
                requestHash,
                responseStatus,
                responseBody,
                status,
                correlationId,
                requestId,
                agora,
                agora,
                expiresAt);
        put(toItem(idempotencyRecord));
        return idempotencyRecord;
    }

    public Optional<IdempotencyRecord> buscarIdempotencia(String scope, String key) {
        return getItem(tableNames.idempotencia(), "IDEMPOTENCY#" + scope + "#" + key, "REQUEST")
                .map(this::toIdempotencyRecord);
    }

    public boolean idempotenciaExiste(String scope, String key) {
        return getItem(tableNames.idempotencia(), "IDEMPOTENCY#" + scope + "#" + key, "REQUEST").isPresent();
    }

    public void concluirIdempotencia(
            String scope,
            String key,
            ProcessingStatus status,
            int responseStatus,
            String responseBody) {
        var current = buscarIdempotencia(scope, key)
                .orElseThrow(() -> new IllegalStateException("Registro de idempotencia nao encontrado: " + scope + "/" + key));
        put(toItem(new IdempotencyRecord(
                current.scope(),
                current.key(),
                current.requestHash(),
                responseStatus,
                responseBody,
                status,
                current.correlationId(),
                current.requestId(),
                current.createdAt(),
                agora(),
                current.expiresAt())));
    }

    public List<DynamoDbItem> catalogoItems() {
        return scan(tableNames.catalogo());
    }

    public List<DynamoDbItem> estoqueItems() {
        return scan(tableNames.estoque());
    }

    public List<DynamoDbItem> execucaoItems() {
        return scan(tableNames.execucoes());
    }

    public List<DynamoDbItem> outboxItems() {
        return scan(tableNames.outbox());
    }

    public List<DynamoDbItem> idempotenciaItems() {
        return scan(tableNames.idempotencia());
    }

    public List<OutboxEventRecord> outboxEvents() {
        return outboxItems().stream()
                .filter(item -> item.entityType().equals("OUTBOX_EVENT"))
                .map(this::toOutboxEvent)
                .sorted(Comparator.comparing(OutboxEventRecord::createdAt))
                .toList();
    }

    public List<OutboxEventRecord> listarOutboxPendenteParaPublicacao(int limit) {
        var now = agora();
        return outboxEvents().stream()
                .filter(event -> event.status() == OutboxStatus.PENDING)
                .filter(event -> event.nextAttemptAt() == null
                        || event.nextAttemptAt().isBefore(now)
                        || event.nextAttemptAt().isEqual(now))
                .limit(Math.max(1, limit))
                .toList();
    }

    public OutboxEventRecord marcarOutboxPublicado(UUID eventId) {
        var current = buscarOutbox(eventId);
        var now = agora();
        var updated = new OutboxEventRecord(
                current.eventId(),
                current.eventType(),
                current.eventVersion(),
                current.topic(),
                current.producer(),
                current.aggregateId(),
                current.payload(),
                OutboxStatus.PUBLISHED,
                current.attempts() + 1,
                null,
                now,
                now.plusDays(7),
                null,
                current.correlationId(),
                current.createdAt(),
                now);
        put(toItem(updated));
        logEvent("outbox event published", updated, "PUBLISHED");
        return updated;
    }

    public OutboxEventRecord marcarFalhaPublicacao(
            UUID eventId,
            String lastError,
            OffsetDateTime nextAttemptAt,
            boolean failed) {
        var current = buscarOutbox(eventId);
        var now = agora();
        var status = failed ? OutboxStatus.FAILED : OutboxStatus.PENDING;
        var updated = new OutboxEventRecord(
                current.eventId(),
                current.eventType(),
                current.eventVersion(),
                current.topic(),
                current.producer(),
                current.aggregateId(),
                current.payload(),
                status,
                current.attempts() + 1,
                failed ? null : nextAttemptAt,
                null,
                null,
                lastError,
                current.correlationId(),
                current.createdAt(),
                now);
        put(toItem(updated));
        logEvent("outbox event publication failed", updated, status.name());
        return updated;
    }

    private OutboxEventRecord buscarOutbox(UUID eventId) {
        return getItem(tableNames.outbox(), "OUTBOX#" + eventId, "EVENT")
                .map(this::toOutboxEvent)
                .orElseThrow(() -> new IllegalStateException("Evento de Outbox nao encontrado: " + eventId));
    }

    private void salvarServico(Servico servico) {
        put(toItem(servico));
    }

    private void salvarPeca(Peca peca) {
        put(toItem(peca));
    }

    private void salvarSaldo(Estoque saldo) {
        put(toItem(saldo));
    }

    private void salvarExecucaoEEvento(
            Execucao execucao,
            StatusExecucao statusAnterior,
            String descricao,
            String eventType,
            String topic,
            Map<String, Object> payload,
            String correlationId) {
        var historico = historico(execucao, statusAnterior, execucao.status(), descricao, null, execucao.atualizadoEm());
        var outbox = outboxRecord(eventType, topic, execucao.ordemServicoId().toString(), payload, correlationId(correlationId));
        transactPut(toItem(execucao), historico, toItem(outbox));
        logEvent("outbox event registered", outbox, "PENDING");
    }

    private OutboxEventRecord outboxEstoque(MovimentoEstoque movimento, String correlationId) {
        if (movimento.tipo() == TipoMovimentoEstoque.ENTRADA) {
            return outboxRecord(
                    "estoqueAcrescentado",
                    "oficina.execution.estoque-acrescentado",
                    movimento.pecaId().toString(),
                    payloadEstoque(movimento),
                    correlationId(correlationId));
        }
        if (movimento.tipo() == TipoMovimentoEstoque.RESERVA || movimento.tipo() == TipoMovimentoEstoque.CONSUMO) {
            return outboxRecord(
                    "estoqueBaixado",
                    "oficina.execution.estoque-baixado",
                    movimento.pecaId().toString(),
                    payloadEstoque(movimento),
                    correlationId(correlationId));
        }
        return null;
    }

    private OutboxEventRecord outboxRecord(
            String eventType,
            String topic,
            String aggregateId,
            Map<String, Object> payload,
            String correlationId) {
        var agora = agora();
        return new OutboxEventRecord(
                UUID.randomUUID(),
                eventType,
                1,
                topic,
                PRODUCER,
                aggregateId,
                payload,
                OutboxStatus.PENDING,
                0,
                agora,
                null,
                null,
                null,
                correlationId(correlationId),
                agora,
                agora);
    }

    private DynamoDbItem historico(
            Execucao execucao,
            StatusExecucao statusAnterior,
            StatusExecucao statusNovo,
            String descricao,
            String sourceEventId,
            OffsetDateTime criadoEm) {
        var historicoId = UUID.randomUUID();
        return new DynamoDbItem(
                tableNames.execucoes(),
                KEY_PREFIX_EXECUCAO + execucao.execucaoId(),
                "HISTORICO#" + criadoEm + "#" + historicoId,
                "EXECUCAO_HISTORICO",
                attributes(
                        "historicoId", historicoId,
                        ATTR_EXECUCAO_ID, execucao.execucaoId(),
                        ATTR_ORDEM_SERVICO_ID, execucao.ordemServicoId(),
                        "statusAnterior", statusAnterior,
                        "statusNovo", statusNovo,
                        "descricao", descricao,
                        ATTR_CREATED_AT, criadoEm,
                        "sourceEventId", sourceEventId));
    }

    private Execucao findExecucaoDaOrdemServico(UUID ordemServicoId) {
        return execucaoItems().stream()
                .filter(item -> item.entityType().equals("EXECUCAO"))
                .map(this::toExecucao)
                .filter(execucao -> execucao.ordemServicoId().equals(ordemServicoId))
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> payloadDiagnosticoIniciado(Execucao execucao) {
        return attributes(
                ATTR_EXECUCAO_ID, execucao.execucaoId(),
                ATTR_ORDEM_SERVICO_ID, execucao.ordemServicoId(),
                ATTR_STATUS_EXECUCAO, execucao.status(),
                "iniciadoEm", execucao.atualizadoEm());
    }

    private Map<String, Object> payloadDiagnosticoFinalizado(Execucao execucao) {
        return attributes(
                ATTR_EXECUCAO_ID, execucao.execucaoId(),
                ATTR_ORDEM_SERVICO_ID, execucao.ordemServicoId(),
                ATTR_STATUS_EXECUCAO, execucao.status(),
                "diagnostico", execucao.diagnostico(),
                "servicos", List.of(),
                "pecas", List.of(),
                "finalizadoEm", execucao.atualizadoEm());
    }

    private Map<String, Object> payloadExecucaoIniciada(Execucao execucao) {
        return attributes(
                ATTR_EXECUCAO_ID, execucao.execucaoId(),
                ATTR_ORDEM_SERVICO_ID, execucao.ordemServicoId(),
                ATTR_STATUS_EXECUCAO, execucao.status(),
                "iniciadaEm", execucao.atualizadoEm());
    }

    private Map<String, Object> payloadExecucaoFinalizada(Execucao execucao) {
        return attributes(
                ATTR_EXECUCAO_ID, execucao.execucaoId(),
                ATTR_ORDEM_SERVICO_ID, execucao.ordemServicoId(),
                ATTR_STATUS_EXECUCAO, execucao.status(),
                "observacoes", execucao.observacoesReparo(),
                "finalizadaEm", execucao.atualizadoEm());
    }

    private Map<String, Object> payloadEstoque(MovimentoEstoque movimento) {
        return attributes(
                "movimentoId", movimento.movimentoId(),
                ATTR_PECA_ID, movimento.pecaId(),
                ATTR_ORDEM_SERVICO_ID, movimento.ordemServicoId(),
                "tipo", movimento.tipo(),
                "quantidade", movimento.quantidade(),
                "observacao", movimento.motivo(),
                "movimentadoEm", movimento.criadoEm());
    }

    private DynamoDbItem toItem(Execucao execucao) {
        return new DynamoDbItem(
                tableNames.execucoes(),
                KEY_PREFIX_EXECUCAO + execucao.execucaoId(),
                SORT_KEY_METADATA,
                "EXECUCAO",
                attributes(
                        ATTR_EXECUCAO_ID, execucao.execucaoId(),
                        ATTR_ORDEM_SERVICO_ID, execucao.ordemServicoId(),
                        "status", execucao.status(),
                        "prioridade", execucao.prioridade(),
                        "diagnostico", execucao.diagnostico(),
                        "observacoesReparo", execucao.observacoesReparo(),
                        ATTR_CREATED_AT, execucao.criadoEm(),
                        ATTR_UPDATED_AT, execucao.atualizadoEm(),
                        ATTR_CORRELATION_ID, "local",
                        "filaStatus", filaStatus(execucao),
                        "prioridadeCriadoEm", prioridadeCriadoEm(execucao)));
    }

    private DynamoDbItem toItem(Servico servico) {
        return new DynamoDbItem(
                tableNames.catalogo(),
                KEY_PREFIX_SERVICO + servico.servicoId(),
                SORT_KEY_METADATA,
                "SERVICO",
                attributes(
                        "servicoId", servico.servicoId(),
                        "nome", servico.nome(),
                        "nomeNormalizado", servico.nome().toUpperCase(),
                        "descricao", servico.descricao(),
                        "valorBase", servico.valorBase(),
                        "ativo", servico.ativo(),
                        ATTR_CREATED_AT, servico.criadoEm(),
                        ATTR_UPDATED_AT, servico.atualizadoEm()));
    }

    private DynamoDbItem toItem(Peca peca) {
        return new DynamoDbItem(
                tableNames.catalogo(),
                KEY_PREFIX_PECA + peca.pecaId(),
                SORT_KEY_METADATA,
                "PECA",
                attributes(
                        ATTR_PECA_ID, peca.pecaId(),
                        "nome", peca.nome(),
                        "nomeNormalizado", peca.nome().toUpperCase(),
                        "codigo", peca.codigo(),
                        "valorUnitario", peca.valorUnitario(),
                        "ativo", peca.ativo(),
                        ATTR_CREATED_AT, peca.criadoEm(),
                        ATTR_UPDATED_AT, peca.atualizadoEm()));
    }

    private DynamoDbItem toItem(Estoque saldo) {
        return new DynamoDbItem(
                tableNames.estoque(),
                KEY_PREFIX_PECA + saldo.pecaId(),
                "SALDO",
                "ESTOQUE_SALDO",
                attributes(
                        ATTR_PECA_ID, saldo.pecaId(),
                        "quantidadeDisponivel", saldo.quantidadeDisponivel(),
                        "quantidadeReservada", saldo.quantidadeReservada(),
                        ATTR_UPDATED_AT, saldo.atualizadoEm()));
    }

    private DynamoDbItem toItem(MovimentoEstoque movimento) {
        return new DynamoDbItem(
                tableNames.estoque(),
                KEY_PREFIX_PECA + movimento.pecaId(),
                "MOVIMENTO#" + movimento.criadoEm() + "#" + movimento.movimentoId(),
                "ESTOQUE_MOVIMENTO",
                attributes(
                        "movimentoId", movimento.movimentoId(),
                        ATTR_PECA_ID, movimento.pecaId(),
                        ATTR_ORDEM_SERVICO_ID, movimento.ordemServicoId(),
                        "tipo", movimento.tipo(),
                        "quantidade", movimento.quantidade(),
                        "motivo", movimento.motivo(),
                        ATTR_CORRELATION_ID, correlationId(null),
                        ATTR_CREATED_AT, movimento.criadoEm()));
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
                        "publishedAt", event.publishedAt(),
                        "expiresAt", event.expiresAt(),
                        "lastError", event.lastError(),
                        ATTR_CORRELATION_ID, event.correlationId(),
                        ATTR_CREATED_AT, event.createdAt(),
                        ATTR_UPDATED_AT, event.updatedAt()));
    }

    private DynamoDbItem toItem(IdempotencyRecord idempotencyRecord) {
        return new DynamoDbItem(
                tableNames.idempotencia(),
                "IDEMPOTENCY#" + idempotencyRecord.scope() + "#" + idempotencyRecord.key(),
                "REQUEST",
                "IDEMPOTENCY",
                attributes(
                        "scope", idempotencyRecord.scope(),
                        "key", idempotencyRecord.key(),
                        "requestHash", idempotencyRecord.requestHash(),
                        "responseStatus", idempotencyRecord.responseStatus(),
                        "responseBody", idempotencyRecord.responseBody(),
                        "processingStatus", idempotencyRecord.processingStatus(),
                        ATTR_CORRELATION_ID, idempotencyRecord.correlationId(),
                        "requestId", idempotencyRecord.requestId(),
                        ATTR_CREATED_AT, idempotencyRecord.createdAt(),
                        ATTR_UPDATED_AT, idempotencyRecord.updatedAt(),
                        "expiresAt", idempotencyRecord.expiresAt()));
    }

    private Peca toPeca(DynamoDbItem item) {
        var peca = new Peca(
                uuid(item, ATTR_PECA_ID),
                string(item, "nome"),
                string(item, "codigo"),
                decimal(item, "valorUnitario"),
                offsetDateTime(item, ATTR_CREATED_AT));
        peca.atualizar(string(item, "nome"), string(item, "codigo"), decimal(item, "valorUnitario"), offsetDateTime(item, ATTR_UPDATED_AT));
        return peca;
    }

    private Servico toServico(DynamoDbItem item) {
        var servico = new Servico(
                uuid(item, "servicoId"),
                string(item, "nome"),
                optionalString(item, "descricao"),
                decimal(item, "valorBase"),
                offsetDateTime(item, ATTR_CREATED_AT));
        servico.atualizar(string(item, "nome"), optionalString(item, "descricao"), decimal(item, "valorBase"), offsetDateTime(item, ATTR_UPDATED_AT));
        return servico;
    }

    private Estoque toEstoque(DynamoDbItem item) {
        return new Estoque(
                uuid(item, ATTR_PECA_ID),
                integer(item, "quantidadeDisponivel"),
                integer(item, "quantidadeReservada"),
                offsetDateTime(item, ATTR_UPDATED_AT));
    }

    private MovimentoEstoque toMovimento(DynamoDbItem item) {
        return new MovimentoEstoque(
                uuid(item, "movimentoId"),
                uuid(item, ATTR_PECA_ID),
                optionalUuid(item, ATTR_ORDEM_SERVICO_ID),
                TipoMovimentoEstoque.valueOf(string(item, "tipo")),
                integer(item, "quantidade"),
                optionalString(item, "motivo"),
                offsetDateTime(item, ATTR_CREATED_AT));
    }

    private Execucao toExecucao(DynamoDbItem item) {
        return Execucao.reconstituir(
                uuid(item, ATTR_EXECUCAO_ID),
                uuid(item, ATTR_ORDEM_SERVICO_ID),
                integer(item, "prioridade"),
                StatusExecucao.valueOf(string(item, "status")),
                optionalString(item, "diagnostico"),
                optionalString(item, "observacoesReparo"),
                offsetDateTime(item, ATTR_CREATED_AT),
                offsetDateTime(item, ATTR_UPDATED_AT));
    }

    @SuppressWarnings("unchecked")
    private OutboxEventRecord toOutboxEvent(DynamoDbItem item) {
        return new OutboxEventRecord(
                uuid(item, "eventId"),
                string(item, "eventType"),
                integer(item, "eventVersion"),
                string(item, "topic"),
                string(item, "producer"),
                string(item, "aggregateId"),
                (Map<String, Object>) item.attributes().getOrDefault("payload", Map.of()),
                OutboxStatus.valueOf(string(item, "status")),
                integer(item, "attempts"),
                optionalOffsetDateTime(item, "nextAttemptAt"),
                optionalOffsetDateTime(item, "publishedAt"),
                optionalOffsetDateTime(item, "expiresAt"),
                optionalString(item, "lastError"),
                string(item, ATTR_CORRELATION_ID),
                offsetDateTime(item, ATTR_CREATED_AT),
                offsetDateTime(item, ATTR_UPDATED_AT));
    }

    private IdempotencyRecord toIdempotencyRecord(DynamoDbItem item) {
        return new IdempotencyRecord(
                string(item, "scope"),
                string(item, "key"),
                string(item, "requestHash"),
                optionalInteger(item, "responseStatus"),
                optionalString(item, "responseBody"),
                ProcessingStatus.valueOf(string(item, "processingStatus")),
                idempotencyCorrelationId(item),
                optionalString(item, "requestId"),
                offsetDateTime(item, ATTR_CREATED_AT),
                offsetDateTime(item, ATTR_UPDATED_AT),
                offsetDateTime(item, "expiresAt"));
    }

    private String idempotencyCorrelationId(DynamoDbItem item) {
        var correlationId = optionalString(item, ATTR_CORRELATION_ID);
        return correlationId == null || correlationId.isBlank() ? correlationId(null) : correlationId;
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

    private void put(DynamoDbItem item) {
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(item.tableName())
                .item(toAttributeMap(item))
                .build());
    }

    private void transactPut(DynamoDbItem... items) {
        var transactionItems = new ArrayList<TransactWriteItem>();
        for (var item : items) {
            transactionItems.add(TransactWriteItem.builder()
                    .put(Put.builder()
                            .tableName(item.tableName())
                            .item(toAttributeMap(item))
                            .build())
                    .build());
        }
        dynamoDbClient.transactWriteItems(TransactWriteItemsRequest.builder()
                .transactItems(transactionItems)
                .build());
    }

    private java.util.Optional<DynamoDbItem> getItem(String tableName, String pk, String sk) {
        var response = dynamoDbClient.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        ATTR_PK, AttributeValue.fromS(pk),
                        ATTR_SK, AttributeValue.fromS(sk)))
                .build());
        if (!response.hasItem() || response.item().isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(toDynamoDbItem(tableName, response.item()));
    }

    private List<DynamoDbItem> scan(String tableName) {
        var items = new ArrayList<DynamoDbItem>();
        Map<String, AttributeValue> startKey = null;
        do {
            var request = ScanRequest.builder()
                    .tableName(tableName)
                    .exclusiveStartKey(startKey)
                    .build();
            var response = dynamoDbClient.scan(request);
            response.items().forEach(item -> items.add(toDynamoDbItem(tableName, item)));
            startKey = response.lastEvaluatedKey();
        } while (startKey != null && !startKey.isEmpty());
        return List.copyOf(items);
    }

    private Map<String, AttributeValue> toAttributeMap(DynamoDbItem item) {
        var attributes = new LinkedHashMap<String, AttributeValue>();
        attributes.put(ATTR_PK, AttributeValue.fromS(item.pk()));
        attributes.put(ATTR_SK, AttributeValue.fromS(item.sk()));
        attributes.put(ATTR_ENTITY_TYPE, AttributeValue.fromS(item.entityType()));
        item.attributes().forEach((key, value) -> attributes.put(key, toAttributeValue(value)));
        return attributes;
    }

    private AttributeValue toAttributeValue(Object value) {
        return switch (value) {
            case String string -> AttributeValue.fromS(string);
            case UUID uuid -> AttributeValue.fromS(uuid.toString());
            case OffsetDateTime offsetDateTime -> AttributeValue.fromS(offsetDateTime.toString());
            case BigDecimal decimal -> AttributeValue.fromN(decimal.toPlainString());
            case Integer integer -> AttributeValue.fromN(integer.toString());
            case Long longValue -> AttributeValue.fromN(longValue.toString());
            case Boolean bool -> AttributeValue.fromBool(bool);
            case Enum<?> enumValue -> AttributeValue.fromS(enumValue.name());
            case Map<?, ?> map -> AttributeValue.fromM(mapToAttributeValues(map));
            case List<?> list -> AttributeValue.fromL(list.stream().map(this::toAttributeValue).toList());
            default -> AttributeValue.fromS(value.toString());
        };
    }

    private Map<String, AttributeValue> mapToAttributeValues(Map<?, ?> map) {
        var attributes = new LinkedHashMap<String, AttributeValue>();
        map.forEach((key, value) -> {
            if (value != null) {
                attributes.put(key.toString(), toAttributeValue(value));
            }
        });
        return attributes;
    }

    private DynamoDbItem toDynamoDbItem(String tableName, Map<String, AttributeValue> item) {
        var attributes = new LinkedHashMap<String, Object>();
        item.forEach((key, value) -> {
            if (!ATTR_PK.equals(key) && !ATTR_SK.equals(key) && !ATTR_ENTITY_TYPE.equals(key)) {
                attributes.put(key, fromAttributeValue(value));
            }
        });
        return new DynamoDbItem(
                tableName,
                item.get(ATTR_PK).s(),
                item.get(ATTR_SK).s(),
                item.get(ATTR_ENTITY_TYPE).s(),
                attributes);
    }

    private Object fromAttributeValue(AttributeValue value) {
        if (value.s() != null) {
            return value.s();
        }
        if (value.n() != null) {
            return number(value.n());
        }
        if (value.bool() != null) {
            return value.bool();
        }
        if (value.hasM()) {
            var map = new LinkedHashMap<String, Object>();
            value.m().forEach((key, nested) -> map.put(key, fromAttributeValue(nested)));
            return map;
        }
        if (value.hasL()) {
            return value.l().stream().map(this::fromAttributeValue).toList();
        }
        return null;
    }

    private Object number(String value) {
        var decimal = new BigDecimal(value);
        if (decimal.scale() <= 0) {
            try {
                return decimal.intValueExact();
            } catch (ArithmeticException _) {
                return decimal.longValueExact();
            }
        }
        return decimal;
    }

    private UUID uuid(DynamoDbItem item, String name) {
        return UUID.fromString(string(item, name));
    }

    private UUID optionalUuid(DynamoDbItem item, String name) {
        var value = optionalString(item, name);
        return value == null ? null : UUID.fromString(value);
    }

    private String string(DynamoDbItem item, String name) {
        var value = item.attributes().get(name);
        if (value == null) {
            throw new IllegalStateException("Atributo DynamoDB obrigatorio ausente: " + name);
        }
        return value.toString();
    }

    private String optionalString(DynamoDbItem item, String name) {
        var value = item.attributes().get(name);
        return value == null ? null : value.toString();
    }

    private BigDecimal decimal(DynamoDbItem item, String name) {
        var value = item.attributes().get(name);
        BigDecimal decimal;
        if (value instanceof BigDecimal monetaryValue) {
            return monetaryValue.scale() < 2 ? monetaryValue.setScale(2) : monetaryValue;
        }
        if (value instanceof Number number) {
            decimal = BigDecimal.valueOf(number.longValue());
        } else {
            decimal = new BigDecimal(value.toString());
        }
        return decimal.scale() < 2 ? decimal.setScale(2) : decimal;
    }

    private int integer(DynamoDbItem item, String name) {
        var value = item.attributes().get(name);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private Integer optionalInteger(DynamoDbItem item, String name) {
        var value = item.attributes().get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private OffsetDateTime offsetDateTime(DynamoDbItem item, String name) {
        return OffsetDateTime.parse(string(item, name));
    }

    private OffsetDateTime optionalOffsetDateTime(DynamoDbItem item, String name) {
        var value = optionalString(item, name);
        return value == null ? null : OffsetDateTime.parse(value);
    }

    private void exigirCodigoDisponivel(String codigo, UUID pecaAtualId) {
        var codigoIndisponivel = listarPecas().stream()
                .anyMatch(peca -> peca.codigo().equals(codigo) && !peca.pecaId().equals(pecaAtualId));
        if (codigoIndisponivel) {
            throw new BusinessConflictException("Codigo de peca ja cadastrado: " + codigo);
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
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId.trim();
        }
        var mdcCorrelationId = MDC.get(ATTR_CORRELATION_ID);
        if (mdcCorrelationId != null && !mdcCorrelationId.toString().isBlank()) {
            return mdcCorrelationId.toString();
        }
        return "local-" + UUID.randomUUID();
    }

    private void logEvent(String message, OutboxEventRecord event, String messageStatus) {
        StructuredLog.info(LOG, message, Map.of(
                ATTR_CORRELATION_ID, event.correlationId(),
                "eventId", event.eventId().toString(),
                "eventType", event.eventType(),
                "eventVersion", event.eventVersion(),
                "topic", event.topic(),
                "producer", event.producer(),
                "aggregateId", event.aggregateId(),
                "messageStatus", messageStatus));
    }

    private OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
