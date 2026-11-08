
package br.com.criare3d.controller;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.CalculoResponse;
import br.com.criare3d.dto.HistoricoListaResponse;
import br.com.criare3d.dto.ComparacaoPlataforma;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.dto.HistoricoExportacaoResponse;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.enums.PlataformaVenda;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.interfaces.*;
import br.com.criare3d.util.HistoricoFiltros;
import br.com.criare3d.util.OrdenacaoPaginada;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
public class CalculoHistoricoController {

    private static final DateTimeFormatter FORMATO_DATA_CSV =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"));
    private static final String RESULTADO_CALCULO = "resultadoCalculo";
    private static final String REQUEST_CALCULO = "requestCalculo";
    private static final String CALCULO_PARA_EDITAR = "calculoParaEditar";

    private final CatalogoBuscaService catalogoBuscaService;
    private final CalculoHistoricoService calculoHistoricoService;
    private final PecaService pecaService;
    private final FilamentoService filamentoService;
    private final ImpressoraService impressoraService;
    private final EmbalagemService embalagemService;
    private final ConfiguracaoService configuracaoService;

    public CalculoHistoricoController(
            CatalogoBuscaService catalogoBuscaService,
            CalculoHistoricoService calculoHistoricoService,
            PecaService pecaService,
            FilamentoService filamentoService,
            ImpressoraService impressoraService,
            EmbalagemService embalagemService,
            ConfiguracaoService configuracaoService
    ) {
        this.catalogoBuscaService = catalogoBuscaService;
        this.calculoHistoricoService = calculoHistoricoService;
        this.pecaService = pecaService;
        this.filamentoService = filamentoService;
        this.impressoraService = impressoraService;
        this.embalagemService = embalagemService;
        this.configuracaoService = configuracaoService;
    }

    @GetMapping("/historico/listar")
    public ModelAndView fetchAll(
            @RequestParam(name = "busca", defaultValue = "") String busca,
            @RequestParam(name = "data", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(name = "plataforma", defaultValue = "") String plataforma,
            @RequestParam(name = "produzido", defaultValue = "TODOS") String produzido,
            @RequestParam(name = "vendido", defaultValue = "TODOS") String vendido,
            @RequestParam(name = "ordem", defaultValue = "dataDesc") String ordem,
            @PageableDefault(page = 0, size = 5) Pageable pageable) {

        plataforma = HistoricoFiltros.normalizarPlataforma(plataforma);
        produzido = HistoricoFiltros.normalizarStatus(produzido);
        vendido = HistoricoFiltros.normalizarStatus(vendido);

        ordem = OrdenacaoPaginada.validarOrdem(
                ordem,
                "dataDesc",
                HistoricoFiltros.ORDENACOES
        );

        pageable = OrdenacaoPaginada.aplicar(
                pageable,
                ordem,
                HistoricoFiltros.ORDENACOES
        );

        ModelAndView mv = new ModelAndView("historico/listar");

        Page<HistoricoListaResponse> pagina = catalogoBuscaService.buscarHistorico(
                busca.trim(),
                data,
                plataforma,
                produzido,
                vendido,
                pageable
        );

        mv.addObject("historicos", pagina.getContent());
        mv.addObject("pagina", pagina);
        mv.addObject("busca", busca);
        mv.addObject("data", data);
        mv.addObject("plataforma", plataforma);
        mv.addObject("produzido", produzido);
        mv.addObject("vendido", vendido);
        mv.addObject("ordem", ordem);

        return mv;
    }

    @GetMapping(value = "/historico/exportar.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportarHistoricoCsv() {
        StringBuilder csv = new StringBuilder("\uFEFFPeça;Data do cálculo;Plataforma;Custo unitário;Preço sugerido por unidade;Lucro líquido por unidade;Unidades;Produzido;Vendido\r\n");
        for (HistoricoExportacaoResponse item : calculoHistoricoService.exportarHistorico()) {
            csv.append(campoCsv(item.nomePeca())).append(';')
                    .append(campoCsv(item.dataCalculo() == null
                            ? "" : item.dataCalculo().format(FORMATO_DATA_CSV))).append(';')
                    .append(campoCsv(nomePlataformaCsv(item.plataforma()))).append(';')
                    .append(numeroCsv(item.custoTotal())).append(';')
                    .append(numeroCsv(item.precoSugerido())).append(';')
                    .append(numeroCsv(item.lucroLiquido())).append(';')
                    .append(item.unidades() == null ? 1 : item.unidades()).append(';')
                    .append(Boolean.TRUE.equals(item.produzido()) ? "Sim" : "Não").append(';')
                    .append(Boolean.TRUE.equals(item.vendido()) ? "Sim" : "Não").append("\r\n");
        }
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"historico-calculos.csv\"")
                .contentType(new org.springframework.http.MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String campoCsv(String valor) {
        if (valor == null) return "\"\"";
        String seguro = valor.stripLeading().matches("^[=+@\\-].*") ? "'" + valor : valor;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }

    private String numeroCsv(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString().replace('.', ',');
    }

    private String nomePlataformaCsv(String plataforma) {
        if (plataforma == null || plataforma.isBlank() || "NENHUMA".equals(plataforma)) return "Sem plataforma";
        return switch (plataforma) {
            case "TIKTOK", "TIKTOK_SHOP" -> "TikTok Shop";
            case "SHOPEE" -> "Shopee";
            case "MERCADO_LIVRE" -> "Mercado Livre";
            default -> plataforma;
        };
    }

    @GetMapping("/historico/novo")
    public ModelAndView retornarPaginaCadastro(
            @RequestParam(required = false) UUID pecaCriada,
            @RequestParam(required = false) UUID filamentoCriado,
            @RequestParam(required = false) UUID impressoraCriada,
            @RequestParam(required = false) UUID embalagemCriada,
            @RequestParam(defaultValue = "false") boolean editar,
            @RequestParam(defaultValue = "false") boolean novo,
            HttpSession session) {

        if (novo) {
            session.removeAttribute(REQUEST_CALCULO);
            session.removeAttribute(RESULTADO_CALCULO);
            session.removeAttribute(CALCULO_PARA_EDITAR);
        }

        ModelAndView mv = new ModelAndView("historico/novo");

        CalculoRequest request;

        if (editar && session.getAttribute(CALCULO_PARA_EDITAR) != null) {
            request = (CalculoRequest) session.getAttribute(CALCULO_PARA_EDITAR);
            session.removeAttribute(CALCULO_PARA_EDITAR);
        } else {
            request = new CalculoRequest();

            var configuracao = configuracaoService.buscarOuCriarConfiguracao();

            if (Boolean.TRUE.equals(configuracao.getMarkupFixo())) {
                request.setMarkupPercentual(configuracao.getMarkupPercentual());
            } else {
                request.setMarkupPercentual(new BigDecimal("30.00"));
            }
        }

        if (request.getFilamentos() == null || request.getFilamentos().isEmpty()) {
            request.getFilamentos().add(new ItemCalculoRequest());
        }

        if (pecaCriada != null) {
            request.setPecaId(pecaCriada);
        }

        if (impressoraCriada != null) {
            request.setImpressoraId(impressoraCriada);
        }

        if (embalagemCriada != null) {
            request.setEmbalagemId(embalagemCriada);
        }

        mv.addObject("calculo", request);
        mv.addObject("pecaCriada", pecaCriada);
        mv.addObject("filamentoCriado", filamentoCriado);
        mv.addObject("impressoraCriada", impressoraCriada);
        mv.addObject("embalagemCriada", embalagemCriada);

        adicionarPreviewFoto(mv, request);
        carregarDadosFormulario(mv);

        return mv;
    }

    /*
     * O cálculo segue o padrão POST -> GET.
     * O resultado fica na sessão e o POST responde com HTTP 303.
     */
    @PostMapping("/historico/calcular")
    public Object calcular(
            @Valid @ModelAttribute("calculo") CalculoRequest request,
            BindingResult bd,
            @RequestParam(value = "fotoPeca", required = false) MultipartFile fotoPeca,
            HttpSession session) {

        try {
            aplicarFoto(request, fotoPeca);

            if (request.isFreteMercadoLivreManualNecessario()
                    && request.getFreteMercadoLivreManual() == null) {
                bd.rejectValue(
                        "freteMercadoLivreManual",
                        "freteMercadoLivreManual.obrigatorio",
                        "Informe o custo de envio que ficará por sua conta."
                );
            }

            if (bd.hasErrors()) {
                ModelAndView mv = new ModelAndView("historico/novo");
                mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
                mv.addObject("calculo", request);
                mv.addObject("respostaCalculoComErro", true);
                adicionarPreviewFoto(mv, request);
                carregarDadosFormulario(mv);
                return mv;
            }

            CalculoHistorico calculo = calculoHistoricoService.calcular(request);

            session.setAttribute(REQUEST_CALCULO, request);
            session.setAttribute(
                    RESULTADO_CALCULO,
                    CalculoResponse.toDto(calculo)
            );

            return ResponseEntity
                    .status(HttpStatus.SEE_OTHER)
                    .location(URI.create("/historico/resultado"))
                    .build();

        } catch (IllegalArgumentException | IOException e) {
            boolean precisaFreteManual = e instanceof IllegalArgumentException
                    && e.getMessage() != null
                    && e.getMessage().toLowerCase(java.util.Locale.ROOT)
                    .contains("informe o frete manualmente");

            if (precisaFreteManual) {
                request.setFreteMercadoLivreManualNecessario(true);
            }

            ModelAndView mv = new ModelAndView("historico/novo");
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            mv.addObject("calculo", request);
            mv.addObject("respostaCalculoComErro", true);

            if (!precisaFreteManual) {
                mv.addObject("erro", e instanceof IOException
                        ? "Não foi possível processar a foto. Tente selecionar a imagem novamente."
                        : e.getMessage());
            }

            adicionarPreviewFoto(mv, request);
            carregarDadosFormulario(mv);

            return mv;
        }
    }

    @GetMapping("/historico/resultado")
    public ModelAndView resultado(
            HttpSession session,
            @RequestParam(defaultValue = "LUCRO_DESEJADO") String modoComparacao,
            @RequestParam(required = false) BigDecimal precoVendaFixo
    ) {
        CalculoRequest request =
                (CalculoRequest) session.getAttribute(REQUEST_CALCULO);

        CalculoResponse resultado =
                (CalculoResponse) session.getAttribute(RESULTADO_CALCULO);

        if (request == null || resultado == null) {
            return new ModelAndView("redirect:/historico/novo?novo=true");
        }

        if (!"PRECO_FIXO".equals(modoComparacao)) {
            modoComparacao = "LUCRO_DESEJADO";
        }

        ModelAndView mv = new ModelAndView("historico/resultado");

        mv.addObject("calculo", request);
        mv.addObject("resultado", resultado);
        adicionarResumoLote(mv, resultado);
        mv.addObject("modoComparacao", modoComparacao);
        mv.addObject("precoVendaFixo", precoVendaFixo);

        mv.addObject(
                "comparacoes",
                compararPlataformas(request, resultado, modoComparacao, precoVendaFixo)
        );

        adicionarPreviewFoto(mv, request);

        return mv;
    }

    /*
     * Calcula a mesma peça nas três plataformas.
     * Se alguma delas precisar de uma informação que não foi fornecida,
     * o motivo aparece como aviso somente naquela linha.
     */
    private List<ComparacaoPlataforma> compararPlataformas(
            CalculoRequest base,
            CalculoResponse resultadoBase,
            String modoComparacao,
            BigDecimal precoVendaFixo
    ) {
        record Opcao(PlataformaVenda plataforma, String nome) {
        }

        List<Opcao> opcoes = List.of(
                new Opcao(PlataformaVenda.SHOPEE, "Shopee"),
                new Opcao(PlataformaVenda.TIKTOK, "TikTok Shop"),
                new Opcao(PlataformaVenda.MERCADO_LIVRE, "Mercado Livre")
        );

        boolean precoFixo = "PRECO_FIXO".equals(modoComparacao);

        List<ComparacaoPlataforma> linhas = new ArrayList<>();

        for (Opcao opcao : opcoes) {

            ComparacaoPlataforma linha = new ComparacaoPlataforma();

            linha.setNome(opcao.nome());
            linha.setCodigo(opcao.plataforma().name());

            linha.setSelecionada(
                    base.getPlataformaVenda() == opcao.plataforma()
                            || (opcao.plataforma() == PlataformaVenda.TIKTOK
                            && "TIKTOK_SHOP".equals(
                            String.valueOf(base.getPlataformaVenda())
                    ))
            );

            if (precoFixo && (precoVendaFixo == null
                    || precoVendaFixo.compareTo(BigDecimal.ZERO) <= 0)) {

                linha.setAviso("Informe um preço de venda maior que zero.");

                linhas.add(linha);
                continue;
            }

            // O cálculo originalmente enviado já contém os valores da plataforma
            // selecionada. Reutilizá-los evita repetir toda a leitura e validação
            // da peça, impressora, filamentos e configuração nessa linha.
            if (!precoFixo && linha.isSelecionada()) {
                preencherValoresComparacao(linha, resultadoBase.getPrecoVendaPlataforma(),
                        resultadoBase.getTaxasPlataforma(), resultadoBase.getLucroLiquido());
                linhas.add(linha);
                continue;
            }

            try {
                CalculoRequest copia = copiarParaComparacao(base);
                copia.setPlataformaVenda(opcao.plataforma());

                CalculoHistorico calculo;

                if (precoFixo) {
                    calculo = calculoHistoricoService.calcularComPrecoFixo(
                            copia,
                            precoVendaFixo
                    );
                } else {
                    calculo = calculoHistoricoService.calcular(copia);
                }

                BigDecimal preco = calculo.getPrecoVendaPlataforma();
                BigDecimal lucro = calculo.getLucroLiquido();

                preencherValoresComparacao(linha, preco, calculo.getTaxasPlataforma(), lucro);

            } catch (IllegalArgumentException e) {
                linha.setAviso(e.getMessage());
            }

            linhas.add(linha);
        }

        if (precoFixo) {
            linhas.stream()
                    .filter(ComparacaoPlataforma::isCalculada)
                    .filter(l -> l.getLucro() != null)
                    .max(Comparator.comparing(ComparacaoPlataforma::getLucro))
                    .ifPresent(l -> {
                        l.setMelhor(true);
                        l.setEtiquetaMelhor("maior lucro");
                    });

        } else {
            linhas.stream()
                    .filter(ComparacaoPlataforma::isCalculada)
                    .filter(l -> l.getPrecoVenda() != null)
                    .min(Comparator.comparing(ComparacaoPlataforma::getPrecoVenda))
                    .ifPresent(l -> {
                        l.setMelhor(true);
                        l.setEtiquetaMelhor("menor preço");
                    });
        }

        return linhas;
    }

    private void preencherValoresComparacao(
            ComparacaoPlataforma linha,
            BigDecimal preco,
            BigDecimal taxas,
            BigDecimal lucro
    ) {
        linha.setPrecoVenda(preco);
        linha.setTaxas(taxas);
        linha.setLucro(lucro);

        if (preco != null && preco.compareTo(BigDecimal.ZERO) > 0 && lucro != null) {
            linha.setMargem(lucro.multiply(new BigDecimal("100"))
                    .divide(preco, 1, RoundingMode.HALF_UP));
        }
    }


    private CalculoRequest copiarParaComparacao(CalculoRequest original) {

        CalculoRequest copia = new CalculoRequest();

        copia.setPecaId(original.getPecaId());
        copia.setImpressoraId(original.getImpressoraId());
        copia.setEmbalagemId(original.getEmbalagemId());
        copia.setTempoImpressaoMinutos(original.getTempoImpressaoMinutos());
        copia.setTempoTrabalhoMinutos(original.getTempoTrabalhoMinutos());
        copia.setMarkupPercentual(original.getMarkupPercentual());

        List<ItemCalculoRequest> itens = new ArrayList<>();

        if (original.getFilamentos() != null) {
            for (ItemCalculoRequest item : original.getFilamentos()) {

                ItemCalculoRequest novo = new ItemCalculoRequest();

                novo.setFilamentoId(item.getFilamentoId());
                novo.setQuantidadeFilamentoGramas(
                        item.getQuantidadeFilamentoGramas()
                );

                itens.add(novo);
            }
        }

        copia.setFilamentos(itens);

        copia.setTipoAnuncio(original.getTipoAnuncio());
        copia.setPercentualComissaoPlataforma(
                original.getPercentualComissaoPlataforma()
        );
        copia.setCustoAdicionalPlataforma(
                original.getCustoAdicionalPlataforma()
        );
        copia.setPercentualImposto(original.getPercentualImposto());
        copia.setFreteVendedor(original.getFreteVendedor());

        copia.setTaxaCpfShopee(original.isTaxaCpfShopee());
        copia.setTipoPessoaShopee(original.getTipoPessoaShopee());
        copia.setMais450PedidosShopee(original.isMais450PedidosShopee());

        copia.setFreteGratisTikTok(original.isFreteGratisTikTok());
        copia.setPercentualAfiliado(original.getPercentualAfiliado());
        copia.setIsencaoComissaoTikTok(original.isIsencaoComissaoTikTok());

        copia.setFreteMercadoLivreManual(original.getFreteMercadoLivreManual());
        copia.setFreteMercadoLivreManualNecessario(original.isFreteMercadoLivreManualNecessario());

        String faixaPeso = original.getFaixaPesoPacote();

        if (faixaPeso == null || faixaPeso.isBlank()) {
            faixaPeso = estimarFaixaPesoPacote(original);
        }

        copia.setFaixaPesoPacote(faixaPeso);

        return copia;
    }

    private String estimarFaixaPesoPacote(CalculoRequest request) {

        BigDecimal pesoImpressao = BigDecimal.ZERO;

        if (request.getFilamentos() != null) {
            for (ItemCalculoRequest item : request.getFilamentos()) {

                if (item.getQuantidadeFilamentoGramas() != null) {
                    pesoImpressao = pesoImpressao.add(
                            item.getQuantidadeFilamentoGramas()
                    );
                }
            }
        }

        if (pesoImpressao.compareTo(BigDecimal.ZERO) == 0
                && request.getQuantidadeFilamentoGramas() != null) {

            pesoImpressao = request.getQuantidadeFilamentoGramas();
        }

        // Margem estimada para a embalagem.
        BigDecimal pesoEstimado = pesoImpressao.add(
                new BigDecimal("100")
        );

        if (pesoEstimado.compareTo(new BigDecimal("300")) <= 0) {
            return "ATE_300G";
        }

        if (pesoEstimado.compareTo(new BigDecimal("500")) <= 0) {
            return "DE_300G_500G";
        }

        if (pesoEstimado.compareTo(new BigDecimal("1000")) <= 0) {
            return "DE_500G_1KG";
        }

        if (pesoEstimado.compareTo(new BigDecimal("1500")) <= 0) {
            return "DE_1KG_1_5KG";
        }

        if (pesoEstimado.compareTo(new BigDecimal("2000")) <= 0) {
            return "DE_1_5KG_2KG";
        }

        if (pesoEstimado.compareTo(new BigDecimal("3000")) <= 0) {
            return "DE_2KG_3KG";
        }

        if (pesoEstimado.compareTo(new BigDecimal("4000")) <= 0) {
            return "DE_3KG_4KG";
        }

        if (pesoEstimado.compareTo(new BigDecimal("5000")) <= 0) {
            return "DE_4KG_5KG";
        }

        throw new IllegalArgumentException(
                "O peso estimado do pacote ultrapassa 5 kg. "
                        + "Não foi possível calcular o frete do Mercado Livre."
        );
    }


    @PostMapping("/historico/editar")
    public ResponseEntity<Void> editar(
            @ModelAttribute("calculo") CalculoRequest request,
            HttpSession session) {

        session.setAttribute(CALCULO_PARA_EDITAR, request);

        return ResponseEntity
                .status(HttpStatus.SEE_OTHER)
                .location(URI.create("/historico/novo?editar=true"))
                .build();
    }

    @PostMapping("/historico/salvar")
    public ModelAndView salvar(
            @Valid @ModelAttribute("calculo") CalculoRequest request,
            BindingResult bd,
            HttpSession session) {

        ModelAndView mv = new ModelAndView("historico/novo");
        mv.addObject("calculo", request);

        if (bd.hasErrors()) {
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            adicionarPreviewFoto(mv, request);
            carregarDadosFormulario(mv);
            return mv;
        }

        try {
            CalculoHistorico calculo = calculoHistoricoService.calcular(request);

            calculo.setFotoPecaBase64(request.getFotoPecaBase64());
            calculo.setFotoPecaContentType(request.getFotoPecaContentType());

            calculoHistoricoService.create(calculo);

            session.removeAttribute(REQUEST_CALCULO);
            session.removeAttribute(RESULTADO_CALCULO);

            return new ModelAndView("redirect:/historico/listar?sucesso=historicoSalvo");

        } catch (IllegalArgumentException e) {
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            mv.addObject("erro", e.getMessage());
            adicionarPreviewFoto(mv, request);
            carregarDadosFormulario(mv);

            return mv;
        }
    }

    @GetMapping("/historico/detalhes/{id}")
    public ModelAndView fetchById(@PathVariable UUID id) {

        return calculoHistoricoService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("historico/detalhes");
                    CalculoResponse historico = CalculoResponse.toDto(entidade);
                    mv.addObject("historico", historico);
                    adicionarResumoLote(mv, historico);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/historico/listar"));
    }

    @GetMapping("/historico/{id}/orcamento")
    public ModelAndView abrirOrcamento(@PathVariable UUID id) {
        return calculoHistoricoService.fetchById(id)
                .map(entidade -> {
                    CalculoResponse calculo = CalculoResponse.toDto(entidade);
                    ModelAndView mv = new ModelAndView("historico/orcamento");
                    mv.addObject("calculo", calculo);
                    adicionarResumoLote(mv, calculo);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/historico/listar?erro=naoEncontrado"));
    }

    private void adicionarResumoLote(ModelAndView mv, CalculoResponse calculo) {
        int unidades = calculo.getUnidades() == null ? 1 : calculo.getUnidades();
        BigDecimal precoUnitario = calculo.getPrecoVendaPlataforma() != null
                ? calculo.getPrecoVendaPlataforma() : calculo.getPrecoSugerido();
        BigDecimal lucroUnitario = calculo.getLucroLiquido() != null
                ? calculo.getLucroLiquido() : calculo.getLucroEstimado();
        mv.addObject("unidades", unidades);
        mv.addObject("custoTotalLote", calculo.getCustoTotal().multiply(BigDecimal.valueOf(unidades)));
        mv.addObject("precoUnitario", precoUnitario);
        mv.addObject("precoTotalLote", precoUnitario.multiply(BigDecimal.valueOf(unidades)));
        mv.addObject("lucroTotalLote", lucroUnitario.multiply(BigDecimal.valueOf(unidades)));
    }

    @GetMapping("/historico/{id}/foto")
    @ResponseBody
    public ResponseEntity<byte[]> buscarFoto(@PathVariable UUID id) {
        return calculoHistoricoService.fetchById(id)
                .filter(calculo -> calculo.getFotoPecaBase64() != null
                        && !calculo.getFotoPecaBase64().isBlank())
                .map(calculo -> {
                    String tipo = calculo.getFotoPecaContentType();
                    if (!"image/jpeg".equals(tipo)
                            && !"image/png".equals(tipo)
                            && !"image/webp".equals(tipo)) {
                        tipo = "image/jpeg";
                    }

                    return ResponseEntity.ok()
                            .header("Cache-Control", "private, max-age=3600")
                            .contentType(org.springframework.http.MediaType.parseMediaType(tipo))
                            .body(Base64.getDecoder().decode(calculo.getFotoPecaBase64()));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void aplicarFoto(CalculoRequest request, MultipartFile fotoPeca)
            throws IOException {

        if (fotoPeca == null || fotoPeca.isEmpty()) {
            return;
        }

        if (fotoPeca.getSize() > 3 * 1024 * 1024) {
            throw new IllegalArgumentException("A foto deve ter no máximo 3 MB.");
        }

        byte[] bytes = fotoPeca.getBytes();
        String contentType = detectarTipoImagem(bytes);
        if (contentType == null) {
            throw new IllegalArgumentException(
                    "O arquivo não parece ser uma imagem JPG, PNG ou WEBP válida."
            );
        }

        request.setFotoPecaBase64(
                Base64.getEncoder().encodeToString(bytes)
        );

        request.setFotoPecaContentType(contentType);
    }

    private String detectarTipoImagem(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A
                && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return "image/png";
        }
        if (bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private void adicionarPreviewFoto(ModelAndView mv, CalculoRequest request) {

        if (request.getFotoPecaBase64() != null
                && !request.getFotoPecaBase64().isBlank()
                && request.getFotoPecaContentType() != null) {

            mv.addObject(
                    "fotoPecaPreview",
                    "data:" + request.getFotoPecaContentType()
                            + ";base64," + request.getFotoPecaBase64()
            );
        }
    }

    private void carregarDadosFormulario(ModelAndView mv) {

        Pageable pageable = PageRequest.of(0, 1000);

        mv.addObject("pecas", pecaService.fetchAll(pageable).getContent());
        mv.addObject("filamentos", filamentoService.fetchAll(pageable).getContent());
        mv.addObject("impressoras", impressoraService.fetchAll(pageable).getContent());
        mv.addObject("embalagens", embalagemService.fetchAll(pageable).getContent());
        var configuracao = configuracaoService.buscarOuCriarConfiguracao();
        mv.addObject("configuracao", configuracao);
        boolean tarifaSemValor = configuracao.getTarifaEnergiaKwh() == null
                || configuracao.getTarifaEnergiaKwh().signum() == 0;
        boolean markupFixoSemValor = Boolean.TRUE.equals(configuracao.getMarkupFixo())
                && (configuracao.getMarkupPercentual() == null
                || configuracao.getMarkupPercentual().signum() == 0);
        mv.addObject("avisoConfiguracaoIncompleta", tarifaSemValor || markupFixoSemValor);
    }

}
