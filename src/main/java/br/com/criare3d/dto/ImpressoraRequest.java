package br.com.criare3d.dto;


import br.com.criare3d.models.Impressora;
import br.com.criare3d.models.Peca;
import jakarta.persistence.Column;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImpressoraRequest {
    @NotBlank(message="O nome da impressora é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da impressora deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_imp", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @NotBlank(message="O modelo da impressora é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da impressora deve ter entre 2 à 150 caracteres")
    @Column(name="modelo_imp", nullable = false, length = 150)
    private @Getter @Setter String modelo;

    @NotNull(message = "A potencia da impressora é obrigatória")
    @Column(name="potencia_imp", nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "A potência não pode ser negativa.")
    private @Getter @Setter BigDecimal potenciaWatts;

    @DecimalMin(value = "0.0", message = "O valor de compra não pode ser negativo")
    private @Getter @Setter BigDecimal valorCompra;

    @DecimalMin(value = "0.0", message = "A vida útil não pode ser negativa")
    private @Getter @Setter BigDecimal vidaUtilHoras;

    @DecimalMin(value = "0.0", message = "A depreciação por hora não pode ser negativa")
    private @Getter @Setter BigDecimal depreciacaoPorHora;
    private @Getter @Setter String depreciacaoOrigem;
    private @Getter @Setter Integer anunciosNovos;
    private @Getter @Setter Integer anunciosUsados;
    @Builder.Default
    private @Getter @Setter Boolean depreciacaoManual = false;

    @DecimalMin(value = "0.0", message = "As horas mensais não podem ser negativas")
    private @Getter @Setter BigDecimal horasMensaisUso;
    @Builder.Default
    private @Getter @Setter Boolean horasMensaisManual = false;

    @jakarta.validation.constraints.Min(value = 0, message = "O aquecimento não pode ser negativo.")
    @jakarta.validation.constraints.Max(value = 10000, message = "O aquecimento deve ser de até 10.000 minutos.")
    @Builder.Default
    private @Getter @Setter Integer tempoAquecimentoMinutos = 0;

    @AssertTrue(message = "Para usar o cálculo antigo, informe também as horas estimadas da impressora.")
    public boolean isDadosDepreciacaoValidos() {
        boolean taxaValida = depreciacaoPorHora == null || depreciacaoPorHora.signum() >= 0;
        boolean dadosLegadosValidos = depreciacaoPorHora != null || valorCompra == null
                || (vidaUtilHoras != null && vidaUtilHoras.signum() > 0);
        return taxaValida && dadosLegadosValidos;
    }

    public static ImpressoraRequest toDto(final Impressora impressora){
        return ImpressoraRequest.builder()
                .nome(impressora.getNome())
                .modelo(impressora.getModelo())
                .potenciaWatts(impressora.getPotenciaWatts())
                .valorCompra(impressora.getValorCompra())
                .vidaUtilHoras(impressora.getVidaUtilHoras())
                .tempoAquecimentoMinutos(impressora.getTempoAquecimentoMinutos())
                .depreciacaoPorHora(impressora.getDepreciacaoPorHora())
                .depreciacaoOrigem(impressora.getDepreciacaoOrigem())
                .anunciosNovos(impressora.getAnunciosNovos())
                .anunciosUsados(impressora.getAnunciosUsados())
                .depreciacaoManual(impressora.getDepreciacaoManual())
                .horasMensaisUso(impressora.getHorasMensaisUso())
                .horasMensaisManual(impressora.getHorasMensaisManual())
                .build();
    }

    public static Impressora toEntity(final ImpressoraRequest dto){
        return Impressora.builder()
                .nome(dto.getNome())
                .modelo(dto.getModelo())
                .potenciaWatts(dto.getPotenciaWatts())
                .valorCompra(dto.getValorCompra())
                .vidaUtilHoras(dto.getVidaUtilHoras())
                .depreciacaoPorHora(dto.getDepreciacaoPorHora())
                .depreciacaoOrigem(dto.getDepreciacaoOrigem())
                .anunciosNovos(dto.getAnunciosNovos())
                .anunciosUsados(dto.getAnunciosUsados())
                .depreciacaoManual(Boolean.TRUE.equals(dto.getDepreciacaoManual()))
                .horasMensaisUso(dto.getHorasMensaisUso())
                .horasMensaisManual(Boolean.TRUE.equals(dto.getHorasMensaisManual()))
                .tempoAquecimentoMinutos(dto.getTempoAquecimentoMinutos() == null ? 0 : dto.getTempoAquecimentoMinutos())
                .build();
    }
}
