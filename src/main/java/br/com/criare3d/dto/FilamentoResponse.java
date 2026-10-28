package br.com.criare3d.dto;

import br.com.criare3d.models.Filamento;
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
public class FilamentoResponse {
    private @Getter @Setter UUID id;

    private @Getter @Setter String nome;

    private @Getter @Setter String cor;

    private @Getter @Setter String codigoCor;

    private @Getter @Setter String marca;

    private @Getter @Setter BigDecimal pesoRoloGramas;

    private @Getter @Setter BigDecimal precoRolo;

    public static FilamentoResponse toDto(Filamento filamento){
        return FilamentoResponse.builder()
                .id(filamento.getId())
                .nome(filamento.getNome())
                .cor(filamento.getCor())
                .codigoCor(filamento.getCodigoCor())
                .marca(filamento.getMarca())
                .pesoRoloGramas(filamento.getPesoRoloGramas())
                .precoRolo(filamento.getPrecoRolo())
                .build();
    }

}
