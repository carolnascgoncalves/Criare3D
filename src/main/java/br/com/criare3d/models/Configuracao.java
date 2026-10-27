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

    @NotNull(message = "A porcentagem de markup da configuracao é obrigatoria")
    @Column(name="markup_conf",nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal markupPercentual;

    @NotNull(message = "A tarifa de energia da configuracao é obrigatoria")
    @Column(name="qntFil_peca",nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal tarifaEnergiaKwh;

    @NotNull(message = "A embalagemFixa é obrigatoria")
    @Column(name="embFixa_conf", nullable = false)
    private @Getter @Setter Boolean embalagemFixa;

    @Column(name="custoEmbFixa_conf", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoEmbalagemFixa;
}
