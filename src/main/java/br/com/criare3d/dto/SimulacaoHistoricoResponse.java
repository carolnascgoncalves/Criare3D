package br.com.criare3d.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SimulacaoHistoricoResponse(
        UUID id,
        String nomePeca,
        LocalDateTime dataCalculo,
        String nomeImpressora,
        Integer unidades,
        Integer pecasPorImpressao,
        Long tempoPorUnidadeMinutos,
        Integer aquecimentoMinutos,
        List<SimulacaoFilamentoResponse> filamentos
) { }
