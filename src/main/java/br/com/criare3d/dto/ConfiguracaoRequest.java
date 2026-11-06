package br.com.criare3d.dto;

import br.com.criare3d.models.Configuracao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ConfiguracaoRequest {

    @NotNull(message = "O markup é obrigatório")
    @DecimalMin(value = "0.0", message = "O valor não pode ser negativo.")
    private BigDecimal markupPercentual;

    private Boolean markupFixo = false;

    @NotNull(message = "A tarifa de energia é obrigatória")
    @DecimalMin(value = "0.0", message = "O valor não pode ser negativo.")
    private BigDecimal tarifaEnergiaKwh;

    @NotNull(message = "Informe se utilizará embalagem fixa")
    private Boolean embalagemFixa;

    @DecimalMin(value = "0.0", message = "O custo não pode ser negativo.")
    private BigDecimal custoEmbalagemFixa;

    private Boolean formularioAvancado = false;

    private Boolean maoObraAtivada = false;
    private BigDecimal valorMaoObraHora;
    private Boolean maoObraTempoFixo = false;
    private Long tempoMaoObraFixoMinutos = 0L;
    private Boolean depreciacaoAtivada = false;
    private Boolean taxaFalhaAtivada = false;
    private BigDecimal percentualFalha = new BigDecimal("5.00");

    @AssertTrue(message = "Informe um custo por hora igual ou maior que zero para a mão de obra.")
    public boolean isMaoObraValida() {
        return !Boolean.TRUE.equals(maoObraAtivada)
                || (valorMaoObraHora != null && valorMaoObraHora.signum() >= 0);
    }

    @AssertTrue(message = "Informe um tempo fixo de mão de obra igual ou maior que zero.")
    public boolean isTempoMaoObraFixoValido() {
        return !Boolean.TRUE.equals(maoObraAtivada)
                || !Boolean.TRUE.equals(maoObraTempoFixo)
                || (tempoMaoObraFixoMinutos != null && tempoMaoObraFixoMinutos >= 0);
    }

    @AssertTrue(message = "Informe um percentual de falhas entre zero e 100.")
    public boolean isPercentualFalhaValido() {
        return !Boolean.TRUE.equals(taxaFalhaAtivada)
                || (percentualFalha != null && percentualFalha.signum() >= 0
                && percentualFalha.compareTo(new BigDecimal("100")) <= 0);
    }

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
        dto.setMaoObraAtivada(Boolean.TRUE.equals(configuracao.getMaoObraAtivada()));
        dto.setValorMaoObraHora(configuracao.getValorMaoObraHora());
        dto.setMaoObraTempoFixo(Boolean.TRUE.equals(configuracao.getMaoObraTempoFixo()));
        dto.setTempoMaoObraFixoMinutos(configuracao.getTempoMaoObraFixoMinutos() != null
                ? configuracao.getTempoMaoObraFixoMinutos() : 0L);
        dto.setDepreciacaoAtivada(Boolean.TRUE.equals(configuracao.getDepreciacaoAtivada()));
        dto.setTaxaFalhaAtivada(Boolean.TRUE.equals(configuracao.getTaxaFalhaAtivada()));
        dto.setPercentualFalha(configuracao.getPercentualFalha() != null
                ? configuracao.getPercentualFalha() : new BigDecimal("5.00"));

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
                .maoObraAtivada(Boolean.TRUE.equals(request.getMaoObraAtivada()))
                .valorMaoObraHora(request.getValorMaoObraHora())
                .maoObraTempoFixo(Boolean.TRUE.equals(request.getMaoObraTempoFixo()))
                .tempoMaoObraFixoMinutos(request.getTempoMaoObraFixoMinutos() != null
                        ? request.getTempoMaoObraFixoMinutos() : 0L)
                .depreciacaoAtivada(Boolean.TRUE.equals(request.getDepreciacaoAtivada()))
                .taxaFalhaAtivada(Boolean.TRUE.equals(request.getTaxaFalhaAtivada()))
                .percentualFalha(request.getPercentualFalha())
                .build();
    }
}
