
package br.com.criare3d.dto;

import br.com.criare3d.models.CalculoHistorico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
public class CalculoRequest {

    @NotNull(message = "A peça é obrigatória")
    private UUID pecaId;

    @Valid
    @NotEmpty(message = "Adicione pelo menos um filamento")
    private List<ItemCalculoRequest> filamentos = new ArrayList<>();

    private BigDecimal quantidadeFilamentoGramas;

    @NotNull(message = "A impressora é obrigatória")
    private UUID impressoraId;

    @NotNull(message = "O tempo de impressão é obrigatório")
    @Positive(message = "O tempo de impressão deve ser maior que zero")
    private Long tempoImpressaoMinutos;

    private UUID embalagemId;

    @NotNull(message = "Selecione o markup do cálculo")
    @DecimalMin(value = "0.0", message = "O markup não pode ser negativo")
    private BigDecimal markupPercentual;

    public static CalculoHistorico toEntity(@Valid CalculoRequest request) {
        return CalculoHistorico.builder()
                .quantidadeFilamentoGramas(request.getQuantidadeFilamentoGramas())
                .tempoImpressaoMinutos(request.getTempoImpressaoMinutos())
                .markupPercentual(request.getMarkupPercentual())
                .build();
    }
}

