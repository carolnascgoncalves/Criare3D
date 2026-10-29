package br.com.criare3d.dto;

import br.com.criare3d.models.CalculoHistorico;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CalculoResponse {
    private @Getter @Setter UUID id;
    private @Getter @Setter String nomePeca;
    private @Getter @Setter String nomeFilamento;
    private @Getter @Setter BigDecimal precoRoloFilamento;
    private @Getter @Setter BigDecimal pesoRoloGramas;
    private @Getter @Setter BigDecimal quantidadeFilamentoGramas;
    private @Getter @Setter String nomeImpressora;
    private @Getter @Setter BigDecimal potenciaWatts;
    private @Getter @Setter Long tempoImpressaoMinutos;
    private @Getter @Setter BigDecimal tarifaEnergiaKwh;
    private @Getter @Setter BigDecimal custoFilamento;
    private @Getter @Setter BigDecimal custoEnergia;
    private @Getter @Setter String nomeEmbalagem;
    private @Getter @Setter BigDecimal custoEmbalagem;
    private @Getter @Setter BigDecimal custoTotal;
    private @Getter @Setter BigDecimal markupPercentual;
    private @Getter @Setter BigDecimal precoSugerido;
    private @Getter @Setter LocalDateTime dataCalculo;
    private @Getter @Setter List<ItemCalculoResponse> itensFilamento;
    private @Getter @Setter BigDecimal lucroEstimado;

    public static CalculoResponse toDto(final CalculoHistorico calculo) {
        return CalculoResponse.builder()
                .id(calculo.getId())
                .nomePeca(calculo.getNomePeca())
                .nomeFilamento(calculo.getNomeFilamento())
                .precoRoloFilamento(calculo.getPrecoRoloFilamento())
                .pesoRoloGramas(calculo.getPesoRoloGramas())
                .quantidadeFilamentoGramas(calculo.getQuantidadeFilamentoGramas())
                .nomeImpressora(calculo.getNomeImpressora())
                .potenciaWatts(calculo.getPotenciaWatts())
                .tempoImpressaoMinutos(calculo.getTempoImpressaoMinutos())
                .tarifaEnergiaKwh(calculo.getTarifaEnergiaKwh())
                .custoFilamento(calculo.getCustoFilamento())
                .custoEnergia(calculo.getCustoEnergia())
                .nomeEmbalagem(calculo.getNomeEmbalagem())
                .custoEmbalagem(calculo.getCustoEmbalagem())
                .custoTotal(calculo.getCustoTotal())
                .markupPercentual(calculo.getMarkupPercentual())
                .precoSugerido(calculo.getPrecoSugerido())
                .dataCalculo(calculo.getDataCalculo())
                .itensFilamento(calculo.getItensFilamento().stream()
                        .map(ItemCalculoResponse::toDto)
                        .collect(Collectors.toList()))
                .lucroEstimado(calculo.getPrecoSugerido().subtract(calculo.getCustoTotal()))
                .build();
    }
}
