package br.com.criare3d.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "configuracao")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Configuracao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private @Getter @Setter UUID id;

    @NotNull(message = "A porcentagem de markup da configuração é obrigatória")
    @Column(name = "markup_conf", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal markupPercentual;

    @Column(name = "markup_fixo_conf", nullable = false)
    @Builder.Default
    private @Getter @Setter Boolean markupFixo = false;

    @NotNull(message = "A tarifa de energia da configuração é obrigatória")
    @Column(name = "qntFil_peca", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal tarifaEnergiaKwh;

    @NotNull(message = "A opção de embalagem fixa é obrigatória")
    @Column(name = "embFixa_conf", nullable = false)
    private @Getter @Setter Boolean embalagemFixa;

    @Column(name = "custoEmbFixa_conf", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoEmbalagemFixa;

    @Column(name = "formulario_avancado_conf", nullable = false)
    @Builder.Default
    private @Getter @Setter Boolean formularioAvancado = false;

    @Column(name = "mao_obra_ativada", nullable = false)
    @Builder.Default
    private @Getter @Setter Boolean maoObraAtivada = false;

    @Column(name = "valor_mao_obra_hora", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal valorMaoObraHora;

    @Column(name = "mao_obra_tempo_fixo", nullable = false)
    @Builder.Default
    private @Getter @Setter Boolean maoObraTempoFixo = false;

    @Column(name = "tempo_mao_obra_fixo_minutos", nullable = false)
    @Builder.Default
    private @Getter @Setter Long tempoMaoObraFixoMinutos = 0L;

    @Column(name = "depreciacao_ativada", nullable = false)
    @Builder.Default
    private @Getter @Setter Boolean depreciacaoAtivada = false;

    @Column(name = "taxa_falha_ativada", nullable = false)
    @Builder.Default
    private @Getter @Setter Boolean taxaFalhaAtivada = false;

    @Column(name = "percentual_falha", precision = 5, scale = 2)
    private @Getter @Setter BigDecimal percentualFalha;

    @Column(name = "purga_gramas_por_impressao", nullable = false, precision = 12, scale = 6)
    @Builder.Default
    private @Getter @Setter BigDecimal purgaGramasPorImpressao = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private @Getter @Setter Usuario usuario;
}
