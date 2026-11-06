package br.com.criare3d.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class FilamentoHistoricoUsoResponse {

    private final UUID historicoId;
    private final String nomePeca;
    private final LocalDateTime dataCalculo;
    private final BigDecimal quantidadeGramas;
    private final Boolean vendido;
}