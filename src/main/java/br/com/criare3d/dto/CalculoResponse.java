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
    private Long tempoTrabalhoMinutos;
    private BigDecimal tarifaEnergiaKwh;

    private BigDecimal custoFilamento;
    private BigDecimal custoEnergia;
    private BigDecimal custoMaoObra;
    private BigDecimal custoDepreciacao;
    private BigDecimal custoManutencao;
    private BigDecimal custoFalha;
    private Boolean maoObraAtivada;
    private Boolean depreciacaoAtivada;
    private Boolean taxaFalhaAtivada;
    private BigDecimal valorMaoObraHora;
    private BigDecimal percentualFalha;
    private BigDecimal valorCompraImpressora;
    private BigDecimal vidaUtilImpressoraHoras;
    private BigDecimal manutencaoHoraImpressora;
    private String nomeEmbalagem;
    private BigDecimal custoEmbalagem;
    private BigDecimal custoTotal;

    private BigDecimal markupPercentual;
    private BigDecimal precoSugerido;
    private LocalDateTime dataCalculo;

    private String fotoPecaBase64;
    private String fotoPecaContentType;

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
    private Boolean formularioAvancado;

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
                .tempoTrabalhoMinutos(calculo.getTempoTrabalhoMinutos())
                .tarifaEnergiaKwh(calculo.getTarifaEnergiaKwh())
                .custoFilamento(calculo.getCustoFilamento())
                .custoEnergia(calculo.getCustoEnergia())
                .custoMaoObra(calculo.getCustoMaoObra())
                .custoDepreciacao(calculo.getCustoDepreciacao())
                .custoManutencao(calculo.getCustoManutencao())
                .custoFalha(calculo.getCustoFalha())
                .maoObraAtivada(calculo.getMaoObraAtivada())
                .depreciacaoAtivada(calculo.getDepreciacaoAtivada())
                .taxaFalhaAtivada(calculo.getTaxaFalhaAtivada())
                .valorMaoObraHora(calculo.getValorMaoObraHora())
                .percentualFalha(calculo.getPercentualFalha())
                .valorCompraImpressora(calculo.getValorCompraImpressora())
                .vidaUtilImpressoraHoras(calculo.getVidaUtilImpressoraHoras())
                .manutencaoHoraImpressora(calculo.getManutencaoHoraImpressora())
                .nomeEmbalagem(calculo.getNomeEmbalagem())
                .custoEmbalagem(calculo.getCustoEmbalagem())
                .custoTotal(calculo.getCustoTotal())
                .markupPercentual(calculo.getMarkupPercentual())
                .precoSugerido(calculo.getPrecoSugerido())
                .dataCalculo(calculo.getDataCalculo())
                .itensFilamento(itens)
                .lucroEstimado(calculo.getPrecoSugerido().subtract(calculo.getCustoTotal()))
                .fotoPecaBase64(calculo.getFotoPecaBase64())
                .fotoPecaContentType(calculo.getFotoPecaContentType())
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
                .formularioAvancado(calculo.getFormularioAvancado())
                .precoVendaPlataforma(calculo.getPrecoVendaPlataforma())
                .taxasPlataforma(calculo.getTaxasPlataforma())
                .lucroLiquido(calculo.getLucroLiquido())
                .build();
    }
}
