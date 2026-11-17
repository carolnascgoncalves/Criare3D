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
public class RateioMotor {
    private UUID custoLojaId;
    private String descricao;
    private BigDecimal valorUnitario;
}
