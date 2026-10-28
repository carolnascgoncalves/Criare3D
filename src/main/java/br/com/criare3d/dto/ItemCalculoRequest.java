
package br.com.criare3d.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ItemCalculoRequest {

    @NotNull(message = "Selecione um filamento")
    private UUID filamentoId;

    @NotNull(message = "A quantidade de filamento é obrigatória")
    @Positive(message = "A quantidade deve ser maior que zero")
    private BigDecimal quantidadeFilamentoGramas;
}
