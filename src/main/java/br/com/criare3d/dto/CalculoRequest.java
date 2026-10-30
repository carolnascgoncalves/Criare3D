package br.com.criare3d.dto;

import br.com.criare3d.models.CalculoHistorico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CalculoRequest {

    @NotNull(message = "A peça é obrigatória")
    private UUID pecaId;

    @Valid
    @NotEmpty(message = "Adicione pelo menos um filamento")
    private List<ItemCalculoRequest> filamentos = new ArrayList<>();

    private BigDecimal quantidadeFilamentoGramas;

    @NotNull(message = "A impressora é obrigatória")
    private UUID impressoraId;

    @NotNull(message = "O tempo de impressão é obrigatório")
    @Positive(message = "O tempo de impressão deve ser maior que zero")
    private Long tempoImpressaoMinutos;

    private UUID embalagemId;

    @NotNull(message = "Selecione o markup do cálculo")
    @DecimalMin(value = "0.0", message = "O markup não pode ser negativo")
    private BigDecimal markupPercentual;

    private String plataformaVenda = "NENHUMA";

    private String tipoAnuncio = "CLASSICO";

    private BigDecimal percentualComissaoPlataforma = BigDecimal.ZERO;

    private BigDecimal custoAdicionalPlataforma = BigDecimal.ZERO;

    private BigDecimal percentualImposto = BigDecimal.ZERO;

    private BigDecimal freteVendedor = BigDecimal.ZERO;

    // Shopee
    private boolean taxaCpfShopee;
    private String tipoPessoaShopee = "CPF";
    private boolean mais450PedidosShopee;

    // TikTok Shop
    private boolean freteGratisTikTok;
    private BigDecimal percentualAfiliado = BigDecimal.ZERO;
    private boolean isencaoComissaoTikTok;

    // Mercado Livre: ATE_300G, DE_300G_500G, DE_500G_1KG,
    // DE_1KG_1_5KG, DE_1_5KG_2KG, DE_2KG_3KG,
    // DE_3KG_4KG, DE_4KG_5KG
    private String faixaPesoPacote;

    public static CalculoHistorico toEntity(CalculoRequest request) {
        return CalculoHistorico.builder()
                .quantidadeFilamentoGramas(request.getQuantidadeFilamentoGramas())
                .tempoImpressaoMinutos(request.getTempoImpressaoMinutos())
                .markupPercentual(request.getMarkupPercentual())
                .build();
    }
}