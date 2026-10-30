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
}