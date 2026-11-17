package br.com.criare3d.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Estado de um cálculo identificado por UUID. Não depende de sessão HTTP. */
public class RascunhoCalculo {
    @Getter
    private final UUID id;
    @Getter
    private final Dados dados;
    private final BiConsumer<String, Object> persistir;

    @Getter
    @Setter
    public static class Dados {
        private CalculoRequest requestCalculo;
        private CalculoResponse resultadoCalculo;
        private CalculoRequest calculoParaEditar;
        private UUID fotoCalculoId;
        private String fotoCalculoTipo;
        private UUID orcamentoDestino;
        private UUID pecaReferencia;
        private BigDecimal custoReferencia;
        private RascunhoFormulario formulario;
    }

    public RascunhoCalculo() {
        this(UUID.randomUUID(), new Dados(), (chave, valor) -> {});
    }

    public RascunhoCalculo(UUID id, Dados dados, BiConsumer<String, Object> persistir) {
        this.id = id;
        this.dados = dados;
        this.persistir = persistir;
    }

    public Object getAttribute(String chave) {
        return switch (chave) {
            case "requestCalculo" -> dados.requestCalculo;
            case "resultadoCalculo" -> dados.resultadoCalculo;
            case "calculoParaEditar" -> dados.calculoParaEditar;
            case "fotoCalculoId" -> dados.fotoCalculoId;
            case "fotoCalculoTipo" -> dados.fotoCalculoTipo;
            case "orcamentoDestino" -> dados.orcamentoDestino;
            case "pecaReferencia" -> dados.pecaReferencia;
            case "custoReferencia" -> dados.custoReferencia;
            case "formulario" -> dados.formulario;
            default -> null;
        };
    }

    public void setAttribute(String chave, Object valor) {
        if (valor instanceof CalculoRequest entrada) {
            entrada.setFotoPecaBytes(null);
            entrada.setFotoPecaContentType(null);
        }
        // Cada alteração é atômica no banco, sem sobrescrever a foto ou outros campos de outra requisição.
        persistir.accept(chave, valor);
        switch (chave) {
            case "requestCalculo" -> dados.requestCalculo = (CalculoRequest) valor;
            case "resultadoCalculo" -> dados.resultadoCalculo = (CalculoResponse) valor;
            case "calculoParaEditar" -> dados.calculoParaEditar = (CalculoRequest) valor;
            case "fotoCalculoId" -> dados.fotoCalculoId = (UUID) valor;
            case "fotoCalculoTipo" -> dados.fotoCalculoTipo = (String) valor;
            case "orcamentoDestino" -> dados.orcamentoDestino = (UUID) valor;
            case "pecaReferencia" -> dados.pecaReferencia = (UUID) valor;
            case "custoReferencia" -> dados.custoReferencia = (BigDecimal) valor;
            case "formulario" -> dados.formulario = (RascunhoFormulario) valor;
            default -> throw new IllegalArgumentException("Campo de rascunho inválido.");
        }
    }

    public void removeAttribute(String chave) {
        if (getAttribute(chave) != null) setAttribute(chave, null);
    }
}
