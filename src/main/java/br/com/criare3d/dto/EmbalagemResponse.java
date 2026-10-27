package br.com.criare3d.dto;

import br.com.criare3d.models.Embalagem;
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
public class EmbalagemResponse {
    private @Getter @Setter UUID id;

    private @Getter @Setter String nome;

    private @Getter @Setter BigDecimal custoUnitario;

    public static EmbalagemResponse toDto(Embalagem embalagem){
        return EmbalagemResponse.builder()
                .id(embalagem.getId())
                .nome(embalagem.getNome())
                .custoUnitario(embalagem.getCustoUnitario())
                .build();
    }
}
