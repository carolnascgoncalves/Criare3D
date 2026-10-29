package br.com.criare3d.dto;

import br.com.criare3d.models.Configuracao;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;


@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfiguracaoRequest {

    @NotNull(message = "A tarifa de energia da configuração é obrigatória")
    private @Getter @Setter BigDecimal tarifaEnergiaKwh;

    @NotNull(message = "A opção de embalagem fixa é obrigatória")
    private @Getter @Setter Boolean embalagemFixa;

    private @Getter @Setter BigDecimal custoEmbalagemFixa;

    public static ConfiguracaoRequest toDto(Configuracao configuracao) {
        return ConfiguracaoRequest.builder()
                .tarifaEnergiaKwh(configuracao.getTarifaEnergiaKwh())
                .embalagemFixa(configuracao.getEmbalagemFixa())
                .custoEmbalagemFixa(configuracao.getCustoEmbalagemFixa())
                .build();
    }

    public static Configuracao toEntity(ConfiguracaoRequest dto) {
        return Configuracao.builder()
                .tarifaEnergiaKwh(dto.getTarifaEnergiaKwh())
                .embalagemFixa(dto.getEmbalagemFixa())
                .custoEmbalagemFixa(dto.getCustoEmbalagemFixa())
                .build();
    }
}
