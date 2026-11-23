package br.com.criare3d.api;

import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.Peca;
import br.com.criare3d.models.enums.PlataformaVenda;
import br.com.criare3d.dto.Modelo3mfFilamentoResponse;
import br.com.criare3d.service.Modelo3mfParser;
import br.com.criare3d.service.FotoCalculoService;
import br.com.criare3d.service.PecaArquivoService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Optional;
import java.util.Base64;
import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.dto.HistoricoListaResponse;
import br.com.criare3d.service.CalculoService;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.ConfiguracaoPadrao;
import br.com.criare3d.service.HistoricoCalculoService;
import br.com.criare3d.exception.RegraNegocioException;
import org.springframework.http.HttpStatus;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calculos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ApiCalculosController {
    private final CalculoService calculos;
    private final CatalogoBuscaService catalogo;
    private final HistoricoCalculoService historico;
    private final ConfiguracaoService configuracoes;

    private final Modelo3mfParser parser;
    private final FotoCalculoService fotos;
    private final PecaArquivoService arquivos;

    public record VendaCalculoEntrada(PlataformaVenda plataformaVenda, String tipoAnuncio,
            @PositiveOrZero BigDecimal percentualComissaoPlataforma, @PositiveOrZero BigDecimal custoAdicionalPlataforma,
            @PositiveOrZero BigDecimal percentualImposto, @PositiveOrZero BigDecimal freteVendedor,
            @PositiveOrZero BigDecimal percentualAfiliado, String tipoPessoaShopee, Boolean mais450PedidosShopee,
            Boolean freteGratisTikTok, Boolean isencaoComissaoTikTok, String faixaPesoPacote,
            @PositiveOrZero BigDecimal freteMercadoLivreManual) {}

    public record ArquivoConsulta(String nome, int placas, Double segundos, Double gramas,
            List<Modelo3mfFilamentoResponse> materiais, boolean completo) {}

    @PostMapping(value="/arquivo", consumes="multipart/form-data")
    public ArquivoConsulta arquivo(@RequestParam("arquivo3mf") MultipartFile arquivo) throws IOException {
        var m = parser.analisarConsulta(arquivo);
        return new ArquivoConsulta(m.nome(), m.numeroPlacas(), m.tempoTotalSegundos(), m.pesoTotalGramas(), m.filamentosUsados(), m.estatisticasCompletas());
    }

    @GetMapping("/{id}/foto")
    @Transactional
    public ResponseEntity<ApiPainelController.FotoCatalogo> foto(@PathVariable UUID id) {
        var foto = fotos.buscarConteudoFoto(id, true);
        if (foto.isPresent()) return ResponseEntity.ok(new ApiPainelController.FotoCatalogo(foto.get().tipo(),
                Base64.getEncoder().encodeToString(foto.get().bytes())));
        var imagem = historico.fetchById(id).map(CalculoHistorico::getPeca).flatMap(this::imagemPeca);
        return imagem.map(i -> ResponseEntity.ok(new ApiPainelController.FotoCatalogo(i.mimeType(),
                Base64.getEncoder().encodeToString(i.bytes())))).orElseGet(() -> ResponseEntity.noContent().build());
    }

    private Optional<PecaArquivoService.ImagemPeca> imagemPeca(Peca peca) {
        return arquivos.buscarResumo(peca)
                .flatMap(a -> a.placas().stream().filter(placa -> placa.temImagem()).findFirst())
                .flatMap(placa -> arquivos.buscarImagem(peca.getId(), placa.numero()));
    }

    public record EntradaCalculo(UUID pecaId, @NotNull UUID impressoraId,
            @NotEmpty @Size(max=100) List<@NotNull @Valid ItemCalculoRequest> filamentos,
            UUID embalagemId, @Min(1) @Max(10000) int unidades,
            @Min(1) @Max(1000000) long minutos, @Min(0) @Max(1000000) Long trabalho,
            @DecimalMin("0") @DecimalMax("99999") BigDecimal markup, @Valid VendaCalculoEntrada venda, @Min(1) @Max(100) Integer placas, @Size(max=150) String nomeConsulta) {}

    public record CustosCalculo(BigDecimal custoFilamento, BigDecimal custoEnergia, BigDecimal custoDepreciacao, BigDecimal custoManutencao, BigDecimal custoEmbalagem, BigDecimal custoMaoObra, BigDecimal custoFalha, BigDecimal custoPurga, BigDecimal custoLoja, BigDecimal taxasPlataforma, BigDecimal precoVendaPlataforma, BigDecimal lucroLiquido) {}

    public record ValorCalculo(UUID id, String nome, BigDecimal custoUnitario, BigDecimal precoUnitario, Integer unidades, CustosCalculo custos) {}

    public record ConfirmacaoCalculo(@NotNull @Valid EntradaCalculo entrada,
            @NotNull BigDecimal custoEsperado, @NotNull BigDecimal precoEsperado, BigDecimal precoPlataformaEsperado,
            @Size(min = 1, max = 3 * 1024 * 1024) byte[] foto) {}

    public record PadroesCalculo(boolean embalagemFixa, boolean markupFixo, BigDecimal markup, boolean avancado) {}

    @GetMapping("/padroes")
    public PadroesCalculo padroes() {
        var config = configuracoes.buscarConfiguracao().orElseGet(ConfiguracaoPadrao::criar);
        return new PadroesCalculo(Boolean.TRUE.equals(config.getEmbalagemFixa()),
                Boolean.TRUE.equals(config.getMarkupFixo()), config.getMarkupPercentual(), Boolean.TRUE.equals(config.getFormularioAvancado()));
    }

    public record CalculoSalvo(ValorCalculo valores, String impressora, String filamento, String embalagem,
            Long minutos, String situacao, java.time.LocalDate data) {}

    @GetMapping("/{id}")
    public CalculoSalvo detalhe(@PathVariable UUID id) {
        var h = historico.fetchById(id).orElseThrow();
        return new CalculoSalvo(valores(h),
                h.getNomeImpressora(), h.getNomeFilamento(), h.getNomeEmbalagem(), h.getTempoImpressaoMinutos(),
                Boolean.TRUE.equals(h.getVendido()) ? "Vendido" : Boolean.TRUE.equals(h.getProduzido()) ? "Produzido" : "Só cálculo",
                h.getDataCalculo() == null ? null : h.getDataCalculo().toLocalDate());
    }

    private ValorCalculo valores(CalculoHistorico h) {
        return new ValorCalculo(h.getId(), h.getNomePeca(), h.getCustoTotal(), h.getPrecoSugerido(), h.getUnidades(),
                new CustosCalculo(h.getCustoFilamento(), h.getCustoEnergia(), h.getCustoDepreciacao(),
                        h.getCustoManutencao(), h.getCustoEmbalagem(), h.getCustoMaoObra(), h.getCustoFalha(),
                        h.getCustoPurga(), h.getCustoLoja(), h.getTaxasPlataforma(),
                        h.getPrecoVendaPlataforma(), h.getLucroLiquido()));
    }

    private CalculoRequest preparar(EntradaCalculo entrada) {
        var config = configuracoes.buscarConfiguracao().orElseGet(ConfiguracaoPadrao::criar);
        if (!Boolean.TRUE.equals(config.getEmbalagemFixa()) && entrada.embalagemId() == null)
            throw new RegraNegocioException("embalagem_obrigatoria", "Selecione uma embalagem.", HttpStatus.UNPROCESSABLE_ENTITY);
        var r = new CalculoRequest();
        r.setPecaId(entrada.pecaId());
        r.setImpressoraId(entrada.impressoraId());
        r.setFilamentos(entrada.filamentos());
        r.setUnidades(entrada.unidades());
        r.setTempoImpressaoMinutos(entrada.minutos());
        r.setTempoTrabalhoMinutos(entrada.trabalho() == null ? 0L : entrada.trabalho());
        r.setEmbalagemId(entrada.embalagemId());
        r.setMarkupPercentual(entrada.markup() == null || Boolean.TRUE.equals(config.getMarkupFixo())
                ? config.getMarkupPercentual() : entrada.markup());
        var venda = entrada.venda();
        if (entrada.pecaId() != null && venda != null && venda.plataformaVenda() != null) {
            r.setPlataformaVenda(venda.plataformaVenda());
            if (venda.tipoAnuncio() != null) r.setTipoAnuncio(venda.tipoAnuncio());
            r.setPercentualComissaoPlataforma(venda.percentualComissaoPlataforma());
            r.setCustoAdicionalPlataforma(venda.custoAdicionalPlataforma());
            r.setPercentualImposto(venda.percentualImposto());
            r.setFreteVendedor(venda.freteVendedor());
            r.setPercentualAfiliado(venda.percentualAfiliado());
            r.setTipoPessoaShopee(venda.tipoPessoaShopee());
            r.setMais450PedidosShopee(Boolean.TRUE.equals(venda.mais450PedidosShopee()));
            r.setFreteGratisTikTok(Boolean.TRUE.equals(venda.freteGratisTikTok()));
            r.setIsencaoComissaoTikTok(Boolean.TRUE.equals(venda.isencaoComissaoTikTok()));
            r.setFaixaPesoPacote(venda.faixaPesoPacote());
            r.setFreteMercadoLivreManual(venda.freteMercadoLivreManual());
            r.setFreteMercadoLivreManualNecessario(venda.freteMercadoLivreManual() != null);
        }
        return r;
    }

    @PostMapping("/previa")
    public ValorCalculo consultar(@Valid @RequestBody EntradaCalculo entrada) {
        var r = preparar(entrada);
        int placasPorArquivo = entrada.placas() == null ? 1 : entrada.placas();
        var resultado = entrada.pecaId() == null
                ? calculos.preverAvulsoTotais(r, entrada.nomeConsulta(), entrada.unidades() * placasPorArquivo)
                : calculos.prever(r);
        var custos = new CustosCalculo(resultado.custoFilamento(), resultado.custoEnergia(), resultado.custoDepreciacao(),
                resultado.custoManutencao(), resultado.custoEmbalagem(), resultado.custoMaoObra(), resultado.custoFalha(),
                resultado.custoPurga(), resultado.custoLoja(), resultado.taxasPlataforma(),
                resultado.precoVendaPlataforma(), resultado.lucroLiquido());
        return new ValorCalculo(null, resultado.nomePeca(), resultado.custoTotal(), resultado.precoSugerido(),
                resultado.unidades(), custos);
    }

    @PostMapping
    @Transactional
    public ValorCalculo salvar(@Valid @RequestBody ConfirmacaoCalculo confirmacao) {
        var h = calculos.calcular(preparar(confirmacao.entrada()));
        boolean precoCanalAlterado = confirmacao.precoPlataformaEsperado() != null
                && (h.getPrecoVendaPlataforma() == null
                    || h.getPrecoVendaPlataforma().compareTo(confirmacao.precoPlataformaEsperado()) != 0);
        if (h.getCustoTotal().compareTo(confirmacao.custoEsperado()) != 0
                || h.getPrecoSugerido().compareTo(confirmacao.precoEsperado()) != 0 || precoCanalAlterado)
            throw new RegraNegocioException("preco_alterado", "Os custos mudaram. Calcule novamente para conferir o preço atualizado.", HttpStatus.CONFLICT);
        if (confirmacao.foto() != null) {
            String tipo = br.com.criare3d.dto.FotoConteudo.detectarTipoImagem(confirmacao.foto());
            if (tipo == null)
                throw new RegraNegocioException("foto_invalida", "Escolha uma imagem JPG, PNG ou WEBP.", HttpStatus.UNPROCESSABLE_ENTITY);
            return valores(historico.create(h, tipo, confirmacao.foto()));
        }
        h.setTemFoto(false);
        var imagem = Optional.ofNullable(h.getPeca()).flatMap(this::imagemPeca);
        var salvo = imagem.map(i -> historico.create(h, i.mimeType(), i.bytes()))
                .orElseGet(() -> historico.create(h));
        return valores(salvo);
    }

    @GetMapping
    public PaginaResponse<HistoricoListaResponse> listar(@RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="") String busca) {
        var ordem = Sort.by(Sort.Order.desc("dataCalculo"), Sort.Order.asc("id"));
        return PaginaResponse.de(catalogo.buscarHistorico(busca.trim(), null, "", "TODOS", "TODOS",
                PaginaResponse.pedido(pagina, 12, ordem)));
    }
}
