package br.com.criare3d.dto;

import br.com.criare3d.models.Impressora;
import br.com.criare3d.models.Peca;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImpressoraResponse {
    private @Getter @Setter UUID id;

    private @Getter @Setter String nome;

    private @Getter @Setter String modelo;

    private @Getter @Setter BigDecimal potenciaWatts;

    private @Getter @Setter BigDecimal valorCompra;
    private @Getter @Setter BigDecimal vidaUtilHoras;
    private @Getter @Setter BigDecimal depreciacaoPorHora;
    private @Getter @Setter String depreciacaoOrigem;
    private @Getter @Setter Integer anunciosNovos;
    private @Getter @Setter Integer anunciosUsados;
    private @Getter @Setter Boolean depreciacaoManual;
    private @Getter @Setter BigDecimal horasMensaisUso;
    private @Getter @Setter Boolean horasMensaisManual;
    private @Getter @Setter Integer tempoAquecimentoMinutos;

    public boolean isDadosDepreciacaoValidos() {
        return depreciacaoPorHora != null || valorCompra == null
                || (vidaUtilHoras != null && vidaUtilHoras.signum() > 0);
    }

    public static ImpressoraResponse toDto(final Impressora impressora){
        return ImpressoraResponse.builder()
                .id(impressora.getId())
                .nome(impressora.getNome())
                .modelo(impressora.getModelo())
                .potenciaWatts(impressora.getPotenciaWatts())
                .valorCompra(impressora.getValorCompra())
                .vidaUtilHoras(impressora.getVidaUtilHoras())
                .depreciacaoPorHora(impressora.getDepreciacaoPorHora())
                .depreciacaoOrigem(impressora.getDepreciacaoOrigem())
                .anunciosNovos(impressora.getAnunciosNovos())
                .anunciosUsados(impressora.getAnunciosUsados())
                .depreciacaoManual(impressora.getDepreciacaoManual())
                .horasMensaisUso(impressora.getHorasMensaisUso())
                .horasMensaisManual(impressora.getHorasMensaisManual())
                .tempoAquecimentoMinutos(impressora.getTempoAquecimentoMinutos())
                .build();
    }
}
