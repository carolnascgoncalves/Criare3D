package br.com.criare3d.dto;

import br.com.criare3d.models.CalculoHistorico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import br.com.criare3d.models.enums.PlataformaVenda;

@Getter
@Setter
public class CalculoRequest {

    @NotNull(message = "A peça é obrigatória")
    private UUID pecaId;

    @Valid
    @NotEmpty(message = "Adicione pelo menos um filamento")
    private List<ItemCalculoRequest> filamentos = new ArrayList<>();

    private BigDecimal quantidadeFilamentoGramas;

    @NotNull(message = "Informe a quantidade de unidades.")
    @Min(value = 1, message = "A quantidade mínima é 1 unidade.")
    @Max(value = 10000, message = "A quantidade máxima é 10.000 unidades.")
    private Integer unidades = 1;

    @NotNull(message = "Informe quantas fornadas de impressão serão necessárias.")
    @Min(value = 1, message = "Deve haver pelo menos 1 fornada.")
    @Max(value = 10000, message = "O máximo é 10.000 fornadas.")
    private Integer fornadas = 1;

    private byte[] fotoPecaBytes;

    private String fotoPecaContentType;
    private boolean fotoRemovida;

    @NotNull(message = "A impressora é obrigatória")
    private UUID impressoraId;

    @NotNull(message = "O tempo de impressão é obrigatório")
    @Positive(message = "O tempo de impressão deve ser maior que zero")
    private Long tempoImpressaoMinutos;

    @PositiveOrZero(message = "O tempo de trabalho não pode ser negativo")
    private Long tempoTrabalhoMinutos;

    private UUID embalagemId;

    @NotNull(message = "Selecione o markup do cálculo")
    @DecimalMin(value = "0.0", message = "O markup não pode ser negativo")
    private BigDecimal markupPercentual;

    private PlataformaVenda plataformaVenda = PlataformaVenda.NENHUMA;

    private String tipoAnuncio = "CLASSICO";

    @DecimalMin(value = "0.0", message = "A comissão não pode ser negativa.")
    private BigDecimal percentualComissaoPlataforma = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "O custo adicional não pode ser negativo.")
    private BigDecimal custoAdicionalPlataforma = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "O imposto não pode ser negativo.")
    private BigDecimal percentualImposto = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "O frete não pode ser negativo.")
    private BigDecimal freteVendedor = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "O percentual do afiliado não pode ser negativo.")
    private BigDecimal percentualAfiliado = BigDecimal.ZERO;

    private boolean taxaCpfShopee;
    private String tipoPessoaShopee = "CPF";
    private boolean mais450PedidosShopee;

    private boolean freteGratisTikTok;
    private boolean isencaoComissaoTikTok;

    private String faixaPesoPacote;

    @DecimalMin(value = "0.0", message = "O custo de envio não pode ser negativo.")
    private BigDecimal freteMercadoLivreManual;

    private boolean freteMercadoLivreManualNecessario;

    public static CalculoHistorico toEntity(CalculoRequest request) {
        return CalculoHistorico.builder()
                .quantidadeFilamentoGramas(request.getQuantidadeFilamentoGramas())
                .tempoImpressaoMinutos(request.getTempoImpressaoMinutos())
                .unidades(request.getUnidades())
                .fornadas(request.getFornadas())
                .markupPercentual(request.getMarkupPercentual())
                .build();
    }
}
