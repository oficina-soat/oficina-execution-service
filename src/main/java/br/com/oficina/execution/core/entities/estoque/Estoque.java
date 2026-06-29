package br.com.oficina.execution.core.entities.estoque;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class Estoque {
    private final UUID pecaId;
    private int quantidadeDisponivel;
    private int quantidadeReservada;
    private OffsetDateTime atualizadoEm;

    public Estoque(UUID pecaId, int quantidadeDisponivel, int quantidadeReservada, OffsetDateTime atualizadoEm) {
        if (pecaId == null) {
            throw new IllegalArgumentException("Identificador da peca e obrigatorio.");
        }
        if (quantidadeDisponivel < 0 || quantidadeReservada < 0) {
            throw new IllegalArgumentException("Quantidades de estoque nao podem ser negativas.");
        }
        this.pecaId = pecaId;
        this.quantidadeDisponivel = quantidadeDisponivel;
        this.quantidadeReservada = quantidadeReservada;
        this.atualizadoEm = atualizadoEm;
    }

    public MovimentoEstoque registrar(TipoMovimentoEstoque tipo, int quantidade, UUID ordemServicoId, String motivo, OffsetDateTime agora) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo do movimento de estoque e obrigatorio.");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        switch (tipo) {
            case ENTRADA -> quantidadeDisponivel += quantidade;
            case RESERVA -> {
                exigirOrdemServico(ordemServicoId, tipo);
                exigirDisponivel(quantidade);
                quantidadeDisponivel -= quantidade;
                quantidadeReservada += quantidade;
            }
            case CONSUMO -> {
                exigirOrdemServico(ordemServicoId, tipo);
                exigirReservado(quantidade);
                quantidadeReservada -= quantidade;
            }
            case ESTORNO -> {
                exigirOrdemServico(ordemServicoId, tipo);
                exigirReservado(quantidade);
                quantidadeReservada -= quantidade;
                quantidadeDisponivel += quantidade;
            }
        }

        atualizadoEm = agora;
        return new MovimentoEstoque(UUID.randomUUID(), pecaId, ordemServicoId, tipo, quantidade, motivo, agora);
    }

    public UUID pecaId() {
        return pecaId;
    }

    public int quantidadeDisponivel() {
        return quantidadeDisponivel;
    }

    public int quantidadeReservada() {
        return quantidadeReservada;
    }

    public OffsetDateTime atualizadoEm() {
        return atualizadoEm;
    }

    private void exigirDisponivel(int quantidade) {
        if (quantidadeDisponivel < quantidade) {
            throw new WebApplicationException("Saldo de estoque insuficiente para a peca: " + pecaId, Response.Status.CONFLICT);
        }
    }

    private void exigirReservado(int quantidade) {
        if (quantidadeReservada < quantidade) {
            throw new WebApplicationException("Saldo reservado insuficiente para a peca: " + pecaId, Response.Status.CONFLICT);
        }
    }

    private void exigirOrdemServico(UUID ordemServicoId, TipoMovimentoEstoque tipo) {
        if (ordemServicoId == null) {
            throw new IllegalArgumentException("ordemServicoId e obrigatorio para movimento " + tipo + ".");
        }
    }
}
