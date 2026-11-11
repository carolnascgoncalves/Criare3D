package br.com.criare3d.dto;

import java.math.BigDecimal;

public record SimulacaoFilamentoResponse(
        String nome,
        String cor,
        String codigoCor,
        BigDecimal gramasPorUnidade
) { }
