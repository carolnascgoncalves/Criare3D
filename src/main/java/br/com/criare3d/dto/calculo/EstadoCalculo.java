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
public class EstadoCalculo {
    private UUID id;
    private String nomePeca;
    private String categoriaPeca;
    @Builder.Default
    private Boolean temFoto = false;
    private String nomeFilamento;
    private BigDecimal precoRoloFilamento;
    private BigDecimal pesoRoloGramas;
    private BigDecimal quantidadeFilamentoGramas;
    private BigDecimal gramasTotaisLote;
    private String nomeImpressora;
    private BigDecimal potenciaWatts;
    private Long tempoImpressaoMinutos;
    private Long minutosMaquinaLote;
    private Integer pecasPorImpressao;
    private Integer tempoAquecimentoMinutos;
    private BigDecimal purgaGramasPorImpressao;
    private BigDecimal tarifaEnergiaKwh;
    private BigDecimal custoFilamento;
    private BigDecimal custoEnergia;
    private BigDecimal custoMaoObra;
    private BigDecimal custoDepreciacao;
    private BigDecimal custoManutencao;
    private BigDecimal custoFalha;
    private BigDecimal custoPurga;
    private Boolean maoObraAtivada;
    private BigDecimal valorMaoObraHora;
    private Boolean depreciacaoAtivada;
    private Long tempoTrabalhoMinutos;
    private BigDecimal valorCompraImpressora;
    private BigDecimal vidaUtilImpressoraHoras;
    private BigDecimal depreciacaoHoraImpressora;
    private String depreciacaoOrigemImpressora;
    private Integer anunciosNovosDepreciacao;
    private Integer anunciosUsadosDepreciacao;
    private BigDecimal manutencaoHoraImpressora;
    private Boolean taxaFalhaAtivada;
    private BigDecimal percentualFalha;
    private String nomeEmbalagem;
    private BigDecimal custoEmbalagem;
    @Builder.Default
    private BigDecimal custoLoja = BigDecimal.ZERO;
    private BigDecimal custoTotal;
    private BigDecimal markupPercentual;
    private BigDecimal precoSugerido;
    private LocalDateTime dataCalculo;
    private Integer unidades;
    private String plataformaVenda;
    private String tipoAnuncio;
    private BigDecimal percentualComissaoPlataforma;
    private BigDecimal custoAdicionalPlataforma;
    private BigDecimal percentualImposto;
    private BigDecimal freteVendedor;
    private Boolean taxaCpfShopee;
    private Boolean freteGratisTikTok;
    private BigDecimal percentualAfiliado;
    private BigDecimal precoVendaPlataforma;
    private BigDecimal taxasPlataforma;
    private BigDecimal lucroLiquido;
    private String tipoPessoaShopee;
    private Boolean mais450PedidosShopee;
    private Boolean isencaoComissaoTikTok;
    private String faixaPesoPacote;
    private Boolean formularioAvancado;
    @Builder.Default
    private Integer impressoes = 1;
    private UUID pecaId;
    private UUID impressoraId;
    private UUID embalagemId;
    private UUID filamentoId;
    @Builder.Default
    private List<ItemMotor> itensFilamento = new ArrayList<>();
    @Builder.Default
    private List<RateioMotor> rateiosCustos = new ArrayList<>();
}
