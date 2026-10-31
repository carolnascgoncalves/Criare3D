package br.com.criare3d.dto;

import br.com.criare3d.models.CalculoHistorico;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculoResponse {

    private java.util.UUID id;

    private String nomePeca;
    private String nomeFilamento;
    private BigDecimal precoRoloFilamento;
    private BigDecimal pesoRoloGramas;
    private BigDecimal quantidadeFilamentoGramas;

    private String nomeImpressora;
    private BigDecimal potenciaWatts;
    private Long tempoImpressaoMinutos;
    private BigDecimal tarifaEnergiaKwh;

    private BigDecimal custoFilamento;
    private BigDecimal custoEnergia;
    private String nomeEmbalagem;
    private BigDecimal custoEmbalagem;
    private BigDecimal custoTotal;

    private BigDecimal markupPercentual;
    private BigDecimal precoSugerido;
    private LocalDateTime dataCalculo;

    private List<ItemCalculoResponse> itensFilamento;

    private BigDecimal lucroEstimado;

    // Dados da venda pela plataforma
    private String plataformaVenda;
    private String tipoAnuncio;
    private BigDecimal percentualComissaoPlataforma;
    private BigDecimal custoAdicionalPlataforma;
    private BigDecimal percentualImposto;
    private BigDecimal freteVendedor;
    private Boolean taxaCpfShopee;
    private Boolean freteGratisTikTok;
    private BigDecimal percentualAfiliado;
    private String faixaPesoPacote;

    private BigDecimal precoVendaPlataforma;
    private BigDecimal taxasPlataforma;
    private BigDecimal lucroLiquido;

    public static CalculoResponse toDto(CalculoHistorico calculo) {

        List<ItemCalculoResponse> itens = calculo.getItensFilamento() == null
                ? new ArrayList<>()
                : calculo.getItensFilamento()
                .stream()
                .map(ItemCalculoResponse::toDto)
                .collect(Collectors.toList());

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
                .itensFilamento(itens)
                .lucroEstimado(calculo.getPrecoSugerido()
                        .subtract(calculo.getCustoTotal()))

                .plataformaVenda(calculo.getPlataformaVenda())
                .tipoAnuncio(calculo.getTipoAnuncio())
                .percentualComissaoPlataforma(calculo.getPercentualComissaoPlataforma())
                .custoAdicionalPlataforma(calculo.getCustoAdicionalPlataforma())
                .percentualImposto(calculo.getPercentualImposto())
                .freteVendedor(calculo.getFreteVendedor())
                .taxaCpfShopee(calculo.getTaxaCpfShopee())
                .freteGratisTikTok(calculo.getFreteGratisTikTok())
                .percentualAfiliado(calculo.getPercentualAfiliado())
                .faixaPesoPacote(calculo.getFaixaPesoPacote())
                .precoVendaPlataforma(calculo.getPrecoVendaPlataforma())
                .taxasPlataforma(calculo.getTaxasPlataforma())
                .lucroLiquido(calculo.getLucroLiquido())
                .build();
    }
}