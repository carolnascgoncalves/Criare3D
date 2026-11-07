package br.com.criare3d.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Dados mínimos do histórico necessários para atualizar o estoque e o status. */
public record AjusteEstoqueCalculoResponse(
        UUID calculoId,
        Boolean produzido,
        Boolean vendido,
        UUID filamentoItemId,
        BigDecimal quantidadeItem,
        UUID filamentoPrincipalId,
        BigDecimal quantidadePrincipal
) {}
