package br.com.oficina.execution.core.entities.catalogo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class Peca {
    private final UUID pecaId;
    private final OffsetDateTime criadoEm;
    private String nome;
    private String codigo;
    private BigDecimal valorUnitario;
    private boolean ativo;
    private OffsetDateTime atualizadoEm;

    public Peca(UUID pecaId, String nome, String codigo, BigDecimal valorUnitario, OffsetDateTime criadoEm) {
        this.pecaId = pecaId;
        this.criadoEm = criadoEm;
        this.ativo = true;
        atualizar(nome, codigo, valorUnitario, criadoEm);
    }

    public void atualizar(String nome, String codigo, BigDecimal valorUnitario, OffsetDateTime atualizadoEm) {
        this.nome = validarTexto(nome, "Nome da peca e obrigatorio.");
        this.codigo = validarTexto(codigo, "Codigo da peca e obrigatorio.").toUpperCase();
        this.valorUnitario = validarValor(valorUnitario, "Valor unitario da peca deve ser maior ou igual a zero.");
        this.atualizadoEm = atualizadoEm;
    }

    public UUID pecaId() {
        return pecaId;
    }

    public String nome() {
        return nome;
    }

    public String codigo() {
        return codigo;
    }

    public BigDecimal valorUnitario() {
        return valorUnitario;
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
