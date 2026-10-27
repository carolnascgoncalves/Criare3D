package br.com.criare3d.dto;

import br.com.criare3d.models.Configuracao;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfiguracaoResponse {
    private @Getter @Setter UUID id;

    private @Getter @Setter BigDecimal markupPercentual;

    private @Getter @Setter BigDecimal tarifaEnergiaKwh;

    private @Getter @Setter Boolean embalagemFixa;

    private @Getter @Setter BigDecimal custoEmbalagemFixa;

    public static ConfiguracaoResponse toDto(Configuracao configuracao){
        return ConfiguracaoResponse.builder()
                .id(configuracao.getId())
                .markupPercentual(configuracao.getMarkupPercentual())
                .tarifaEnergiaKwh(configuracao.getTarifaEnergiaKwh())
                .embalagemFixa(configuracao.getEmbalagemFixa())
                .custoEmbalagemFixa(configuracao.getCustoEmbalagemFixa())
                .build();
    }
}