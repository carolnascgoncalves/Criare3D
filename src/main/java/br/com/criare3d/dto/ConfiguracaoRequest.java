package br.com.criare3d.dto;

import br.com.criare3d.models.Configuracao;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ConfiguracaoRequest {

    @NotNull(message = "O markup é obrigatório")
    private BigDecimal markupPercentual;

    private Boolean markupFixo = false;

    @NotNull(message = "A tarifa de energia é obrigatória")
    private BigDecimal tarifaEnergiaKwh;

    @NotNull(message = "Informe se utilizará embalagem fixa")
    private Boolean embalagemFixa;

    private BigDecimal custoEmbalagemFixa;

    private Boolean formularioAvancado = false;

    public static ConfiguracaoRequest toDto(Configuracao configuracao) {
        ConfiguracaoRequest dto = new ConfiguracaoRequest();

        dto.setMarkupPercentual(configuracao.getMarkupPercentual());
        dto.setMarkupFixo(Boolean.TRUE.equals(configuracao.getMarkupFixo()));
        dto.setTarifaEnergiaKwh(configuracao.getTarifaEnergiaKwh());
        dto.setEmbalagemFixa(configuracao.getEmbalagemFixa());
        dto.setCustoEmbalagemFixa(configuracao.getCustoEmbalagemFixa());
        dto.setFormularioAvancado(
                Boolean.TRUE.equals(configuracao.getFormularioAvancado())
        );

        return dto;
    }

    public static Configuracao toEntity(ConfiguracaoRequest request) {
        return Configuracao.builder()
                .markupPercentual(request.getMarkupPercentual())
                .markupFixo(Boolean.TRUE.equals(request.getMarkupFixo()))
                .tarifaEnergiaKwh(request.getTarifaEnergiaKwh())
                .embalagemFixa(request.getEmbalagemFixa())
                .custoEmbalagemFixa(request.getCustoEmbalagemFixa())
                .formularioAvancado(
                        Boolean.TRUE.equals(request.getFormularioAvancado())
                )
                .build();
    }
}