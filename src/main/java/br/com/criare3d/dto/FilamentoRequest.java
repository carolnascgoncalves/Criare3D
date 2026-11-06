package br.com.criare3d.dto;

import br.com.criare3d.models.Filamento;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FilamentoRequest {
    @NotBlank(message="O nome do filamento é obrigatório")
    @Size(min = 2, max = 150, message = "O nome do filamento deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_fil", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @NotBlank(message="A cor do filamento é obrigatória")
    @Size(min = 2, max = 80, message = "A cor do filamento deve ter entre 2 à 80 caracteres")
    @Column(name= "cor_fil", nullable = false, length = 80)
    private @Getter @Setter String cor;

    @NotBlank(message = "Selecione uma cor.")
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Selecione uma cor válida.")
    @Column(name = "codigo_cor_fil", length = 7)
    private @Getter @Setter String codigoCor;

    @Size(max = 150, message = "A marca deve ter no máximo 150 caracteres.")
    @Column(name= "marca_fil", length = 150)
    private @Getter @Setter String marca;

    @NotNull(message = "O peso do rolo comprado do filamento é obrigatorio")
    @Column(name="pesoRolo_fil", precision = 10, scale = 2)
    @DecimalMin(value = "0.01", message = "O peso deve ser maior que zero.")
    private @Getter @Setter BigDecimal pesoRoloGramas;

    @DecimalMin(value = "0.00", message = "O estoque não pode ser negativo.")
    @Column(name = "estoque_gramas", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal estoqueGramas;

    @NotNull(message = "O preco do filamento do filamento é obrigatorio")
    @Column(name="precoRolo_fil", precision = 10, scale = 2)
    @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
    private @Getter @Setter BigDecimal precoRolo;

    public static FilamentoRequest toDto(Filamento filamento){
        return FilamentoRequest.builder()
                .nome(filamento.getNome())
                .cor(filamento.getCor())
                .codigoCor(filamento.getCodigoCor())
                .marca(filamento.getMarca())
                .pesoRoloGramas(filamento.getPesoRoloGramas())
                .estoqueGramas(filamento.getEstoqueGramas())
                .precoRolo(filamento.getPrecoRolo())
                .build();
    }

    public static Filamento toEntity(FilamentoRequest dto){
        return Filamento.builder()
                .nome(dto.getNome())
                .cor(dto.getCor())
                .codigoCor(dto.getCodigoCor())
                .marca(dto.getMarca())
                .pesoRoloGramas(dto.getPesoRoloGramas())
                .estoqueGramas(dto.getEstoqueGramas())
                .precoRolo(dto.getPrecoRolo())
                .build();
    }
}
