package br.com.criare3d.dto;

import br.com.criare3d.models.Embalagem;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmbalagemRequest {
    @NotBlank(message="O nome da embalagem é obrigatório")
    @Size(min = 2, max = 150, message = "O nome da embalagem deve ter entre 2 à 150 caracteres")
    @Column(name= "nome_emb", nullable = false, length = 150)
    private @Getter @Setter String nome;

    @NotNull(message="O custo unitário da embalagem é obrigatorio")
    @Column(name="custoUni_emb", nullable = false, precision = 10, scale = 2)
    private @Getter @Setter BigDecimal custoUnitario;

    public static EmbalagemRequest toDto(Embalagem embalagem){
        return EmbalagemRequest.builder()
                .nome(embalagem.getNome())
                .custoUnitario(embalagem.getCustoUnitario())
                .build();
    }

    public static Embalagem toEntity(EmbalagemRequest dto){
        return Embalagem.builder()
                .nome(dto.getNome())
                .custoUnitario(dto.getCustoUnitario())
                .build();
    }
}
