package br.com.criare3d.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "calculoHistorico")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Getter @Setter UUID id;

    @NotBlank(message = "O nome da peça é obrigatório")
    @Size(min = 2, max = 150)
    @Column(name = "nomePeca_hist", nullable = false, length = 150)
    private @Getter @Setter String nomePeca;

    @NotBlank(message = "O nome do filamento é obrigatório")
    @Size(min = 2, max = 150)
    @Column(name = "nomeFil_hist", nullable = false, length = 150)
    private @Getter @Setter String nomeFilamento;

    @NotNull(message = "O preço do rolo é obrigatório")
    @Column(name = "precoRolo_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal precoRoloFilamento;

    @NotNull(message = "O peso do rolo é obrigatório")
    @Column(name = "pesoRolo_fil", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal pesoRoloGramas;

    @NotNull(message = "A quantidade de filamento é obrigatória")
    @Column(name = "qntdFil_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal quantidadeFilamentoGramas;

    @NotBlank(message = "O nome da impressora é obrigatório")
    @Size(min = 2, max = 150)
    @Column(name = "nomeImp_hist", nullable = false, length = 150)
    private @Getter @Setter String nomeImpressora;

    @NotNull(message = "A potência é obrigatória")
    @Column(name = "pot_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal potenciaWatts;

    @NotNull(message = "O tempo da impressão é obrigatório")
    @Column(name = "temp_hist", nullable = false)
    private @Getter @Setter Long tempoImpressaoMinutos;

    @NotNull(message = "A tarifa de energia é obrigatória")
    @Column(name = "tarifa_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal tarifaEnergiaKwh;

    @NotNull(message = "O custo do filamento é obrigatório")
    @Column(name = "custoFil_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoFilamento;

    @NotNull(message = "O custo da energia é obrigatório")
    @Column(name = "custoEnergia_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoEnergia;

    @NotBlank(message = "O nome da embalagem é obrigatório")
    @Size(min = 2, max = 150)
    @Column(name = "nomeEmb_hist", nullable = false, length = 150)
    private @Getter @Setter String nomeEmbalagem;

    @NotNull(message = "O custo da embalagem é obrigatório")
    @Column(name = "custoEmb_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoEmbalagem;

    @NotNull(message = "O custo total é obrigatório")
    @Column(name = "custoTotal_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoTotal;

    @NotNull(message = "O markup é obrigatório")
    @Column(name = "markup_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal markupPercentual;

    @NotNull(message = "O preço sugerido é obrigatório")
    @Column(name = "precoSug_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal precoSugerido;

    @NotNull(message = "A data é obrigatória")
    @Column(name = "data_hist", nullable = false)
    private @Getter @Setter LocalDateTime dataCalculo;

    // Dados da plataforma: snapshots do momento do cálculo

    @Column(name = "plataformaVenda_hist", length = 30)
    private @Getter @Setter String plataformaVenda;

    @Column(name = "tipoAnuncio_hist", length = 30)
    private @Getter @Setter String tipoAnuncio;

    @Column(name = "percentualComissao_hist", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal percentualComissaoPlataforma;

    @Column(name = "custoAdicionalPlataforma_hist", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoAdicionalPlataforma;

    @Column(name = "percentualImposto_hist", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal percentualImposto;

    @Column(name = "freteVendedor_hist", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal freteVendedor;

    @Column(name = "taxaCpfShopee_hist")
    private @Getter @Setter Boolean taxaCpfShopee;

    @Column(name = "freteGratisTikTok_hist")
    private @Getter @Setter Boolean freteGratisTikTok;

    @Column(name = "percentualAfiliado_hist", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal percentualAfiliado;

    @Column(name = "precoVendaPlataforma_hist", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal precoVendaPlataforma;

    @Column(name = "taxasPlataforma_hist", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal taxasPlataforma;

    @Column(name = "lucroLiquido_hist", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal lucroLiquido;

    @Column(name = "tipoPessoaShopee_hist", length = 10)
    private @Getter @Setter String tipoPessoaShopee;

    @Column(name = "mais450PedidosShopee_hist")
    private @Getter @Setter Boolean mais450PedidosShopee;

    @Column(name = "isencaoComissaoTikTok_hist")
    private @Getter @Setter Boolean isencaoComissaoTikTok;

    @Column(name = "faixaPesoPacote_hist", length = 30)
    private @Getter @Setter String faixaPesoPacote;

    @Column(name = "formularioAvancado_hist")
    private @Getter @Setter Boolean formularioAvancado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_filamento_fk")
    private @Getter @Setter Filamento filamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_impressora_fk")
    private @Getter @Setter Impressora impressora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_embalagem_fk")
    private @Getter @Setter Embalagem embalagem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_peca_fk")
    private @Getter @Setter Peca peca;

    @OneToMany(
            mappedBy = "calculoHistorico",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private @Getter @Setter List<ItemCalculoHistorico> itensFilamento = new ArrayList<>();
}