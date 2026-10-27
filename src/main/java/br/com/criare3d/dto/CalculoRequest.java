
package br.com.criare3d.dto;

import br.com.criare3d.models.CalculoHistorico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CalculoRequest {

    @NotNull(message = "A peça é obrigatória")
    private UUID pecaId;

    @NotNull(message = "O filamento é obrigatório")
    private UUID filamentoId;

    @NotNull(message = "A quantidade de filamento é obrigatória")
    @Positive(message = "A quantidade de filamento deve ser maior que zero")
    private BigDecimal quantidadeFilamentoGramas;

    @NotNull(message = "A impressora é obrigatória")
    private UUID impressoraId;

    @NotNull(message = "O tempo de impressão é obrigatório")
    @Positive(message = "O tempo de impressão deve ser maior que zero")
    private Long tempoImpressaoMinutos;

    private UUID embalagemId;


    public static CalculoHistorico toEntity(@Valid CalculoRequest request) {
        return CalculoHistorico.builder()
                .quantidadeFilamentoGramas(request.getQuantidadeFilamentoGramas())
                .tempoImpressaoMinutos(request.getTempoImpressaoMinutos())
                .build();
    }

}
