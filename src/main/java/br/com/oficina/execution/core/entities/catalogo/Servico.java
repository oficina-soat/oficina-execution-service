package br.com.oficina.execution.core.entities.catalogo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class Servico {
    private final UUID servicoId;
    private final OffsetDateTime criadoEm;
    private String nome;
    private String descricao;
    private BigDecimal valorBase;
    private boolean ativo;
    private OffsetDateTime atualizadoEm;

    public Servico(UUID servicoId, String nome, String descricao, BigDecimal valorBase, OffsetDateTime criadoEm) {
        this.servicoId = servicoId;
        this.criadoEm = criadoEm;
        this.ativo = true;
        atualizar(nome, descricao, valorBase, criadoEm);
    }

    public void atualizar(String nome, String descricao, BigDecimal valorBase, OffsetDateTime atualizadoEm) {
        this.nome = validarTexto(nome, "Nome do servico e obrigatorio.");
        this.descricao = descricao == null || descricao.isBlank() ? null : descricao.trim();
        this.valorBase = validarValor(valorBase, "Valor base do servico deve ser maior ou igual a zero.");
        this.atualizadoEm = atualizadoEm;
    }

    public UUID servicoId() {
        return servicoId;
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public BigDecimal valorBase() {
        return valorBase;
    }

    public boolean ativo() {
        return ativo;
    }

    public OffsetDateTime criadoEm() {
        return criadoEm;
    }

    public OffsetDateTime atualizadoEm() {
        return atualizadoEm;
    }

    private static String validarTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor.trim();
    }

    private static BigDecimal validarValor(BigDecimal valor, String mensagem) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException(mensagem);
        }
        return valor;
    }
}
