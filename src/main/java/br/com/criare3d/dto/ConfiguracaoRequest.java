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

    @DecimalMin(value = "0.0", message = "O valor não pode ser negativo.")
    private BigDecimal markupPercentual;

    private Boolean markupFixo = false;
    @AssertTrue(message = "Informe o lucro sobre o custo quando o percentual fixo estiver ativado.")
    public boolean isMarkupFixoValido() {
        return !Boolean.TRUE.equals(markupFixo) || markupPercentual != null && markupPercentual.signum() >= 0;
    }


    @NotNull(message = "Informe o primeiro atalho de markup")
    @DecimalMin(value = "0.0", message = "O atalho não pode ser negativo.")
    private BigDecimal markupOpcao1;

    @NotNull(message = "Informe o segundo atalho de markup")
    @DecimalMin(value = "0.0", message = "O atalho não pode ser negativo.")
    private BigDecimal markupOpcao2;

    @NotNull(message = "Informe o terceiro atalho de markup")
    @DecimalMin(value = "0.0", message = "O atalho não pode ser negativo.")
    private BigDecimal markupOpcao3;

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
    private Long tempoMaoObraFixoMinutos;
    private Boolean depreciacaoAtivada = false;
    private Boolean taxaFalhaAtivada = false;
    private BigDecimal percentualFalha;

    @DecimalMin(value = "0.0", message = "A purga não pode ser negativa.")
    private BigDecimal purgaGramasPorImpressao;

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

        dto.setMarkupPercentual(numeroSemZerosFinais(configuracao.getMarkupPercentual()));
        dto.setMarkupFixo(Boolean.TRUE.equals(configuracao.getMarkupFixo()));
        dto.setMarkupOpcao1(numeroSemZerosFinais(configuracao.getMarkupOpcao1() != null
                ? configuracao.getMarkupOpcao1() : new BigDecimal("30.00")));
        dto.setMarkupOpcao2(numeroSemZerosFinais(configuracao.getMarkupOpcao2() != null
                ? configuracao.getMarkupOpcao2() : new BigDecimal("40.00")));
        dto.setMarkupOpcao3(numeroSemZerosFinais(configuracao.getMarkupOpcao3() != null
                ? configuracao.getMarkupOpcao3() : new BigDecimal("50.00")));
        dto.setTarifaEnergiaKwh(numeroSemZerosFinais(configuracao.getTarifaEnergiaKwh()));
        dto.setEmbalagemFixa(configuracao.getEmbalagemFixa());
        dto.setCustoEmbalagemFixa(numeroSemZerosFinais(configuracao.getCustoEmbalagemFixa()));
        dto.setFormularioAvancado(
                Boolean.TRUE.equals(configuracao.getFormularioAvancado())
        );
        dto.setMaoObraAtivada(Boolean.TRUE.equals(configuracao.getMaoObraAtivada()));
        dto.setValorMaoObraHora(numeroSemZerosFinais(configuracao.getValorMaoObraHora()));
        dto.setMaoObraTempoFixo(Boolean.TRUE.equals(configuracao.getMaoObraTempoFixo()));
        dto.setTempoMaoObraFixoMinutos(configuracao.getTempoMaoObraFixoMinutos() != null
                ? configuracao.getTempoMaoObraFixoMinutos() : 0L);
        dto.setDepreciacaoAtivada(Boolean.TRUE.equals(configuracao.getDepreciacaoAtivada()));
        dto.setTaxaFalhaAtivada(Boolean.TRUE.equals(configuracao.getTaxaFalhaAtivada()));
        dto.setPercentualFalha(numeroSemZerosFinais(configuracao.getPercentualFalha() != null
                ? configuracao.getPercentualFalha() : new BigDecimal("5.00")));
        dto.setPurgaGramasPorImpressao(numeroSemZerosFinais(
                configuracao.getPurgaGramasPorImpressao() != null
                        ? configuracao.getPurgaGramasPorImpressao() : BigDecimal.ZERO));

        return dto;
    }

    private static BigDecimal numeroSemZerosFinais(BigDecimal valor) {
        if (valor == null) return null;
        BigDecimal normalizado = valor.stripTrailingZeros();
        if (normalizado.signum() == 0) return BigDecimal.ZERO;
        return normalizado.scale() < 0 ? normalizado.setScale(0) : normalizado;
    }

    public static Configuracao toEntity(ConfiguracaoRequest request) {
        return Configuracao.builder()
                .markupPercentual(request.getMarkupPercentual())
                .markupFixo(Boolean.TRUE.equals(request.getMarkupFixo()))
                .markupOpcao1(request.getMarkupOpcao1())
                .markupOpcao2(request.getMarkupOpcao2())
                .markupOpcao3(request.getMarkupOpcao3())
                .tarifaEnergiaKwh(request.getTarifaEnergiaKwh())
                .embalagemFixa(request.getEmbalagemFixa())
                .custoEmbalagemFixa(request.getCustoEmbalagemFixa())
                .formularioAvancado(
                        Boolean.TRUE.equals(request.getFormularioAvancado())
                )
                .maoObraAtivada(Boolean.TRUE.equals(request.getMaoObraAtivada()))
                .valorMaoObraHora(request.getValorMaoObraHora())
                .maoObraTempoFixo(Boolean.TRUE.equals(request.getMaoObraTempoFixo()))
                .tempoMaoObraFixoMinutos(request.getTempoMaoObraFixoMinutos())
                .depreciacaoAtivada(Boolean.TRUE.equals(request.getDepreciacaoAtivada()))
                .taxaFalhaAtivada(Boolean.TRUE.equals(request.getTaxaFalhaAtivada()))
                .percentualFalha(request.getPercentualFalha())
                .purgaGramasPorImpressao(request.getPurgaGramasPorImpressao())
                .build();
    }
}
