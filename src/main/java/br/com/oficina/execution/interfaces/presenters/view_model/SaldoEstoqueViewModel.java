package br.com.oficina.execution.interfaces.presenters.view_model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SaldoEstoqueViewModel(
        UUID pecaId,
        int quantidadeDisponivel,
        int quantidadeReservada,
        OffsetDateTime atualizadoEm,
        List<String> acoesPermitidas) {
}
