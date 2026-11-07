package br.com.criare3d.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class FilamentoHistoricoUsoResponse {

    private final UUID historicoId;
    private final String nomePeca;
    private final LocalDateTime dataCalculo;
    private final BigDecimal quantidadeGramas;
    private final String corFilamento;
    private final String codigoCor;
    private final BigDecimal pesoRoloGramas;
    private final Boolean vendido;

    public String getCorHexadecimal() {
        return codigoCor != null && codigoCor.matches("#[0-9A-Fa-f]{6}")
                ? codigoCor : "#b18e7b";
    }

    public int getPercentualDoRolo() {
        if (quantidadeGramas == null || pesoRoloGramas == null || pesoRoloGramas.signum() <= 0) {
            return 0;
        }
        return quantidadeGramas.multiply(BigDecimal.valueOf(100))
                .divide(pesoRoloGramas, 0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100))
                .max(BigDecimal.ZERO)
                .intValue();
    }
}
