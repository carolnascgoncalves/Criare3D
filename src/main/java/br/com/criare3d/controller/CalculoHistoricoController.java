
package br.com.criare3d.controller;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.CalculoResponse;
import br.com.criare3d.dto.ComparacaoPlataforma;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.enums.PlataformaVenda;
import br.com.criare3d.service.CalculoHistoricoServiceImp;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.interfaces.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Controller
public class CalculoHistoricoController {

    private static final String RESULTADO_CALCULO = "resultadoCalculo";
    private static final String REQUEST_CALCULO = "requestCalculo";
    private static final String CALCULO_PARA_EDITAR = "calculoParaEditar";

    @Autowired
    private CatalogoBuscaService catalogoBuscaService;

    @Autowired
    private CalculoHistoricoService calculoHistoricoService;

    @Autowired
    private CalculoHistoricoServiceImp calculoHistoricoServiceImp;

    @Autowired
    private PecaService pecaService;

    @Autowired
    private FilamentoService filamentoService;

    @Autowired
    private ImpressoraService impressoraService;

    @Autowired
    private EmbalagemService embalagemService;

    @Autowired
    private ConfiguracaoService configuracaoService;

    @GetMapping("/historico/listar")
    public ModelAndView fetchAll(
            @RequestParam(name = "busca", defaultValue = "") String busca,
            @RequestParam(name = "data", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @PageableDefault(page = 0, size = 5, sort = "dataCalculo",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        ModelAndView mv = new ModelAndView("historico/listar");

        Page<CalculoHistorico> pagina = catalogoBuscaService.buscarHistorico(
                busca.trim(),
                data,
                pageable
        );

        mv.addObject("historicos", pagina.getContent());
        mv.addObject("pagina", pagina);
        mv.addObject("busca", busca);
        mv.addObject("data", data);

        return mv;
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

            if (bd.hasErrors()) {
                ModelAndView mv = new ModelAndView("historico/novo");
                mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
                mv.addObject("calculo", request);
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
            ModelAndView mv = new ModelAndView("historico/novo");
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            mv.addObject("calculo", request);

            mv.addObject("erro", e instanceof IOException
                    ? "Não foi possível processar a foto. Tente selecionar a imagem novamente."
                    : e.getMessage());

            adicionarPreviewFoto(mv, request);
            carregarDadosFormulario(mv);

            return mv;
        }
    }

    @GetMapping("/historico/resultado")
    public ModelAndView resultado(HttpSession session) {

        CalculoRequest request =
                (CalculoRequest) session.getAttribute(REQUEST_CALCULO);

        CalculoResponse resultado =
                (CalculoResponse) session.getAttribute(RESULTADO_CALCULO);

        if (request == null || resultado == null) {
            return new ModelAndView("redirect:/historico/novo?novo=true");
        }

        ModelAndView mv = new ModelAndView("historico/resultado");
        mv.addObject("calculo", request);
        mv.addObject("resultado", resultado);

        // Novo: comparação entre as plataformas.
        mv.addObject("comparacoes", compararPlataformas(request));

        adicionarPreviewFoto(mv, request);

        return mv;
    }

    /*
     * Calcula a mesma peça nas três plataformas.
     * Se alguma delas precisar de uma informação que não foi fornecida,
     * o motivo aparece como aviso somente naquela linha.
     */
    private List<ComparacaoPlataforma> compararPlataformas(CalculoRequest base) {

        record Opcao(PlataformaVenda plataforma, String nome) {}

        List<Opcao> opcoes = List.of(
                new Opcao(PlataformaVenda.SHOPEE, "Shopee"),
                new Opcao(PlataformaVenda.TIKTOK, "TikTok Shop"),
                new Opcao(PlataformaVenda.MERCADO_LIVRE, "Mercado Livre")
        );

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

            try {
                CalculoRequest copia = copiarParaComparacao(base);
                copia.setPlataformaVenda(opcao.plataforma());

                CalculoHistorico calculo = calculoHistoricoService.calcular(copia);

                BigDecimal preco = calculo.getPrecoVendaPlataforma();
                BigDecimal lucro = calculo.getLucroLiquido();

                linha.setPrecoVenda(preco);
                linha.setTaxas(calculo.getTaxasPlataforma());
                linha.setLucro(lucro);

                if (preco != null
                        && preco.compareTo(BigDecimal.ZERO) > 0
                        && lucro != null) {

                    linha.setMargem(
                            lucro.multiply(new BigDecimal("100"))
                                    .divide(preco, 1, RoundingMode.HALF_UP)
                    );
                }

            } catch (IllegalArgumentException e) {
                linha.setAviso(e.getMessage());
            }

            linhas.add(linha);
        }

        linhas.stream()
                .filter(ComparacaoPlataforma::isCalculada)
                .filter(l -> l.getLucro() != null)
                .max(Comparator.comparing(ComparacaoPlataforma::getLucro))
                .ifPresent(l -> l.setMelhor(true));

        return linhas;
    }


    private CalculoRequest copiarParaComparacao(CalculoRequest original) {

        CalculoRequest copia = new CalculoRequest();

        copia.setPecaId(original.getPecaId());
        copia.setImpressoraId(original.getImpressoraId());
        copia.setEmbalagemId(original.getEmbalagemId());
        copia.setTempoImpressaoMinutos(original.getTempoImpressaoMinutos());
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
            CalculoHistorico calculo = calculoHistoricoServiceImp.calcular(request);

            calculo.setFotoPecaBase64(request.getFotoPecaBase64());
            calculo.setFotoPecaContentType(request.getFotoPecaContentType());

            calculoHistoricoService.create(calculo);

            session.removeAttribute(REQUEST_CALCULO);
            session.removeAttribute(RESULTADO_CALCULO);

            return new ModelAndView("redirect:/historico/listar");

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
                    mv.addObject("historico", CalculoResponse.toDto(entidade));
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/historico/listar"));
    }

    private void aplicarFoto(CalculoRequest request, MultipartFile fotoPeca)
            throws IOException {

        if (fotoPeca == null || fotoPeca.isEmpty()) {
            return;
        }

        if (fotoPeca.getSize() > 3 * 1024 * 1024) {
            throw new IllegalArgumentException("A foto deve ter no máximo 3 MB.");
        }

        String contentType = fotoPeca.getContentType();

        if (contentType == null || !(
                contentType.equals("image/jpeg")
                        || contentType.equals("image/png")
                        || contentType.equals("image/webp")
        )) {
            throw new IllegalArgumentException(
                    "Formato inválido. Envie uma imagem JPG, PNG ou WEBP."
            );
        }

        request.setFotoPecaBase64(
                Base64.getEncoder().encodeToString(fotoPeca.getBytes())
        );

        request.setFotoPecaContentType(contentType);
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
        mv.addObject("configuracao", configuracaoService.buscarOuCriarConfiguracao());
    }

    @PostMapping("/historico/remover/{id}")
    public ModelAndView deleteById(@PathVariable UUID id) {

        if (calculoHistoricoService.existsById(id)) {
            calculoHistoricoService.delete(id);
        }

        return new ModelAndView(
                "redirect:/historico/listar?sucesso=historicoRemovido"
        );
    }
}
