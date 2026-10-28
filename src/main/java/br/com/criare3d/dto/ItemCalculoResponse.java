
package br.com.criare3d.dto;

import br.com.criare3d.models.ItemCalculoHistorico;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCalculoResponse {

    private UUID id;
    private String nomeFilamento;
    private String corFilamento;
    private String codigoCor;
    private BigDecimal precoRoloFilamento;
    private BigDecimal pesoRoloGramas;
    private BigDecimal quantidadeFilamentoGramas;
    private BigDecimal custoFilamento;

    public static ItemCalculoResponse toDto(ItemCalculoHistorico item) {
        return ItemCalculoResponse.builder()
                .id(item.getId())
                .nomeFilamento(item.getNomeFilamento())
                .corFilamento(item.getCorFilamento())
                .codigoCor(item.getCodigoCor())
                .precoRoloFilamento(item.getPrecoRoloFilamento())
                .pesoRoloGramas(item.getPesoRoloGramas())
                .quantidadeFilamentoGramas(item.getQuantidadeFilamentoGramas())
                .custoFilamento(item.getCustoFilamento())
                .build();
    }
}
