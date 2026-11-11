package br.com.criare3d.dto;

import br.com.criare3d.models.Peca;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PecaRequest {
    @NotBlank(message="O nome da peca é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da peca deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_peca", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres.")
    private @Getter @Setter String categoria;

    @NotNull(message = "A quantidade de filamento utilizado na peca é obrigatorio")
    @Column(name="qntFil_peca",nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "A quantidade não pode ser negativa.")
    private @Getter @Setter BigDecimal quantidadeFilamentoGramas;

    @NotNull(message = "O Tempo de impressão da peca é obrigatorio")
    @Column(name= "tmp_peca",nullable = false)
    @PositiveOrZero(message = "O tempo deve ser maior ou igual a zero.")
    private @Getter @Setter Long tempoImpressaoMinutos;

    private @Getter @Setter String dadosArquivo3mf;


    public static PecaRequest toDto(final Peca peca){
        return PecaRequest.builder()
                .nome(peca.getNome())
                .categoria(peca.getCategoria())
                .quantidadeFilamentoGramas(peca.getQuantidadeFilamentoGramas())
                .tempoImpressaoMinutos(peca.getTempoImpressaoMinutos())
                .dadosArquivo3mf(peca.getDadosArquivo3mf())
                .build();
    }

    public static Peca toEntity(final PecaRequest dto){
        return Peca.builder()
                .nome(dto.getNome())
                .categoria(dto.getCategoria())
                .quantidadeFilamentoGramas(dto.getQuantidadeFilamentoGramas())
                .tempoImpressaoMinutos(dto.getTempoImpressaoMinutos())
                .dadosArquivo3mf(dto.getDadosArquivo3mf())
                .build();
    }
}
