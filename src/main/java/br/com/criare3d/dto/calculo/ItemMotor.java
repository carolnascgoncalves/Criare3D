package br.com.criare3d.dto.calculo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemMotor {
    private UUID id;
    private String nomeFilamento;
    private String corFilamento;
    private String codigoCor;
    private BigDecimal precoRoloFilamento;
    private BigDecimal pesoRoloGramas;
    private BigDecimal quantidadeFilamentoGramas;
    @Builder.Default
    private BigDecimal purgaGramas = BigDecimal.ZERO;
    private BigDecimal custoFilamento;
    private UUID filamentoId;
}
