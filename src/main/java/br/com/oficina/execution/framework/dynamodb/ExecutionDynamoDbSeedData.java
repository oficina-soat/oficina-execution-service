package br.com.oficina.execution.framework.dynamodb;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public final class ExecutionDynamoDbSeedData {
    public static final OffsetDateTime SEED_TIME = OffsetDateTime.of(2026, 6, 23, 15, 30, 0, 0, ZoneOffset.UTC);

    private ExecutionDynamoDbSeedData() {
    }

    public static List<PecaSeed> pecas() {
        return List.of(
                new PecaSeed(DynamoDbExecutionStore.SEED_PECA_ID, "Volante", "VOL-001", new BigDecimal("50.00")),
                new PecaSeed(DynamoDbExecutionStore.SEED_PNEU_ID, "Pneu", "PNE-001", BigDecimal.ZERO),
                new PecaSeed(DynamoDbExecutionStore.SEED_TAPETE_ID, "Tapete", "TAP-001", BigDecimal.ZERO));
    }

    public static List<ServicoSeed> servicos() {
        return List.of(new ServicoSeed(
                DynamoDbExecutionStore.SEED_SERVICO_ID,
                "Troca de oleo",
                "Servico reaproveitado do import.sql do oficina-app",
                new BigDecimal("150.00")));
    }

    public static List<SaldoSeed> saldos() {
        return List.of(new SaldoSeed(DynamoDbExecutionStore.SEED_PECA_ID, 50, 0));
    }

    public record PecaSeed(UUID pecaId, String nome, String codigo, BigDecimal valorUnitario) {
    }

    public record ServicoSeed(UUID servicoId, String nome, String descricao, BigDecimal valorBase) {
    }

    public record SaldoSeed(UUID pecaId, int quantidadeDisponivel, int quantidadeReservada) {
    }
}
