package br.com.criare3d.dto;


import br.com.criare3d.models.Impressora;
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
    private @Getter @Setter BigDecimal potenciaWatts;

    public static ImpressoraRequest toDto(final Impressora impressora){
        return ImpressoraRequest.builder()
                .nome(impressora.getNome())
                .modelo(impressora.getModelo())
                .potenciaWatts(impressora.getPotenciaWatts())
                .build();
    }

    public static Impressora toEntity(final ImpressoraRequest dto){
        return Impressora.builder()
                .nome(dto.getNome())
                .modelo(dto.getModelo())
                .potenciaWatts(dto.getPotenciaWatts())
                .build();
    }
}
