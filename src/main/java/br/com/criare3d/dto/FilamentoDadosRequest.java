package br.com.criare3d.dto;

import br.com.criare3d.models.Filamento;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FilamentoDadosRequest {
    @NotBlank(message="O nome do filamento é obrigatório")
    @Size(min = 2, max = 150, message = "O nome do filamento deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_fil", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @NotBlank(message="A cor do filamento é obrigatória")
    @Size(min = 2, max = 80, message = "A cor do filamento deve ter entre 2 à 80 caracteres")
    @Column(name= "cor_fil", nullable = false, length = 80)
    private @Getter @Setter String cor;

    @Size(min = 0, max = 150, message = "A marca do filamento deve ter entre 2 à 150 caracteres")
    @Column(name= "marca_fil", length = 150)
    private @Getter @Setter String marca;

    @NotNull(message = "O peso do rolo comprado do filamento é obrigatorio")
    @Column(name="pesoRolo_fil", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal pesoRoloGramas;

    @NotNull(message = "O preco do filamento do filamento é obrigatorio")
    @Column(name="precoRolo_fil", precision = 10, scale = 2)
    private @Getter @Setter BigDecimal precoRolo;

    public static FilamentoDadosRequest toDto(Filamento filamento){
        return FilamentoDadosRequest.builder()
                .nome(filamento.getNome())
                .cor(filamento.getCor())
                .marca(filamento.getMarca())
                .pesoRoloGramas(filamento.getPesoRoloGramas())
                .precoRolo(filamento.getPrecoRolo())
                .build();
    }

    public static Filamento toEntity(FilamentoDadosRequest dto){
        return Filamento.builder()
                .nome(dto.getNome())
                .cor(dto.getCor())
                .marca((dto.getMarca()))
                .pesoRoloGramas(dto.getPesoRoloGramas())
                .precoRolo(dto.getPrecoRolo())
                .build();
    }
}
