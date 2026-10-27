package br.com.criare3d.dto;

import br.com.criare3d.models.Peca;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PecaResponse {
    private @Getter @Setter UUID id;

    private @Getter @Setter String nome;

    private @Getter @Setter BigDecimal quantidadeFilamentoGramas;

    private @Getter @Setter Long tempoImpressaoMinutos;

    public static PecaResponse toDto(final Peca peca){
        return PecaResponse.builder()
                .id(peca.getId())
                .nome(peca.getNome())
                .quantidadeFilamentoGramas(peca.getQuantidadeFilamentoGramas())
                .tempoImpressaoMinutos(peca.getTempoImpressaoMinutos())
                .build();
    }
}
