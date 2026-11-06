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

    public boolean isDadosDepreciacaoValidos() {
        boolean nenhumDado = valorCompra == null && vidaUtilHoras == null;
        return nenhumDado || (valorCompra != null && vidaUtilHoras != null
                && vidaUtilHoras.signum() > 0);
    }

    public static ImpressoraResponse toDto(final Impressora impressora){
        return ImpressoraResponse.builder()
                .id(impressora.getId())
                .nome(impressora.getNome())
                .modelo(impressora.getModelo())
                .potenciaWatts(impressora.getPotenciaWatts())
                .valorCompra(impressora.getValorCompra())
                .vidaUtilHoras(impressora.getVidaUtilHoras())
                .build();
    }
}
