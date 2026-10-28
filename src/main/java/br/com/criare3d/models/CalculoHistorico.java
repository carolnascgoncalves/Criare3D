package br.com.criare3d.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "calculoHistorico")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculoHistorico {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Getter @Setter UUID id;

    @NotBlank(message="O nome da peca é obrigatória")
    @Size(min = 2, max = 150, message = "O nome da peca deve ter entre 2 à 150 caracteres")
    @Column(name= "nomePeca_hist", nullable = false, length = 150)
    private @Getter @Setter String nomePeca;

    @NotBlank(message="O nome do filamento é obrigatório")
    @Size(min = 2, max = 150, message = "O nome do filamento deve ter entre 2 à 150 caracteres")
    @Column(name= "nomeFil_hist", nullable = false, length = 150)
    private @Getter @Setter String nomeFilamento;

    @NotNull(message = "O preco do rolo é obrigatorio")
    @Column(name="precoRolo_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal precoRoloFilamento;

    @NotNull(message = "O peso do rolo  é obrigatorio")
    @Column(name="pesoRolo_fil", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal pesoRoloGramas;

    @NotNull(message = "A quantidade de filamento é obrigatoria")
    @Column(name="qntdFil_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal quantidadeFilamentoGramas;

    @NotBlank(message="O nome da impressora é obrigatória")
    @Size(min = 2, max = 150, message = "O nome da impressora deve ter entre 2 à 150 caracteres")
    @Column(name= "nomeImp_hist", nullable = false, length = 150)
    private @Getter @Setter String nomeImpressora;

    @NotNull(message = "A potencia é obrigatoria")
    @Column(name="pot_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal potenciaWatts;

    @NotNull(message = "O tempo da impressao é obrigatorio")
    @Column(name="temp_hist", nullable = false)
    private @Getter @Setter Long tempoImpressaoMinutos;

    @NotNull(message = "A tarifa de energia é obrigatoria")
    @Column(name="tarifa_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal tarifaEnergiaKwh;

    @NotNull(message = "O custo do filamento é obrigatorio")
    @Column(name="custoFil_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoFilamento;

    @NotNull(message = "O custo da energia é obrigatoria")
    @Column(name="custoEnergia_hist",nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoEnergia;

    @NotBlank(message="O nome da embalagem é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da embalagem deve ter entre 2 à 150 caracteres")
    @Column(name= "nomeEmb_hist", nullable = false, length = 150)
    private @Getter @Setter String nomeEmbalagem;

    @NotNull(message = "O custo da embalagem é obrigatoria")
    @Column(name="custoEmb_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoEmbalagem;

    @NotNull(message = "O custo total é obrigatorio")
    @Column(name="custoTotal_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoTotal;

    @NotNull(message = "O markup é obrigatorio")
    @Column(name="markup_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal markupPercentual;

    @NotNull(message = "O preco sugerido é obrigatorio")
    @Column(name="precoSug_hist", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal precoSugerido;

    @NotNull(message = "A data é obrigatoria")
    @Column(name = "data_hist", nullable = false)
    private @Getter @Setter LocalDateTime dataCalculo;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="id_filamento_fk")
    private @Getter @Setter Filamento filamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="id_impressora_fk")
    private @Getter @Setter Impressora impressora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="id_embalagem_fk")
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
