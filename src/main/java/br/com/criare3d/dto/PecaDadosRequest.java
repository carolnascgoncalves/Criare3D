package br.com.criare3d.dto;

import br.com.criare3d.models.Peca;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PecaDadosRequest {
    @NotBlank(message="O nome da peca é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da peca deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_peca", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @NotNull(message = "A quantidade de filamento utilizado na peca é obrigatorio")
    @Column(name="qntFil_peca",nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal quantidadeFilamentoGramas;

    @NotNull(message = "O Tempo de impressão da peca é obrigatorio")
    @Column(name= "tmp_peca",nullable = false)
    private @Getter @Setter Long tempoImpressaoMinutos;

    public static PecaDadosRequest toDto(final Peca peca){
        return PecaDadosRequest.builder()
                .nome(peca.getNome())
                .quantidadeFilamentoGramas(peca.getQuantidadeFilamentoGramas())
                .tempoImpressaoMinutos(peca.getTempoImpressaoMinutos())
                .build();
    }

    public static Peca toEntity(final PecaDadosRequest dto){
        return Peca.builder()
                .nome(dto.getNome())
                .quantidadeFilamentoGramas(dto.getQuantidadeFilamentoGramas())
                .tempoImpressaoMinutos(dto.getTempoImpressaoMinutos())
                .build();
    }
}
