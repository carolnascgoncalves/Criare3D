package br.com.criare3d.dto;

import br.com.criare3d.models.Configuracao;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfiguracaoDadosRequest {
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

    public static ConfiguracaoDadosRequest toDto(Configuracao configuracao){
        return ConfiguracaoDadosRequest.builder()
                .markupPercentual(configuracao.getMarkupPercentual())
                .tarifaEnergiaKwh(configuracao.getTarifaEnergiaKwh())
                .embalagemFixa(configuracao.getEmbalagemFixa())
                .custoEmbalagemFixa(configuracao.getCustoEmbalagemFixa())
                .build();
    }

    public static Configuracao toEntity(ConfiguracaoDadosRequest dto){
        return Configuracao.builder()
                .markupPercentual(dto.getMarkupPercentual())
                .tarifaEnergiaKwh(dto.getTarifaEnergiaKwh())
                .embalagemFixa(dto.getEmbalagemFixa())
                .custoEmbalagemFixa(dto.getCustoEmbalagemFixa())
                .build();
    }
}
