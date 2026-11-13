
package br.com.criare3d.controller;

import br.com.criare3d.service.CalculoService;
import br.com.criare3d.service.FotoCalculoService;
import br.com.criare3d.service.HistoricoCalculoService;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.CalculoResponse;
import br.com.criare3d.dto.HistoricoListaResponse;
import br.com.criare3d.dto.ComparacaoPlataforma;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.enums.PlataformaVenda;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.PecaArquivoService;
import br.com.criare3d.service.VerificacaoTarifasPlataforma;
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
import org.springframework.http.HttpStatusCode;
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
import java.util.Locale;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class CalculoHistoricoController {
    @org.springframework.beans.factory.annotation.Autowired
    private br.com.criare3d.service.ResultadoCalculoPresenter presenter;

    private static final String RESULTADO_CALCULO = "resultadoCalculo";
    private static final String REQUEST_CALCULO = "requestCalculo";
    private static final String CALCULO_PARA_EDITAR = "calculoParaEditar";
    private static final String FOTO_SESSAO = "fotoCalculoId";
    @org.springframework.beans.factory.annotation.Autowired
    private br.com.criare3d.service.FotoRascunhoService fotoRascunhoService;
    private static final String FOTO_TIPO_SESSAO = "fotoCalculoTipo";

    private final CatalogoBuscaService catalogoBuscaService;
    private final CalculoService calculoService;
    private final FotoCalculoService fotoCalculoService;
    private final HistoricoCalculoService historicoCalculoService;
    private final PecaService pecaService;
    private final PecaArquivoService pecaArquivoService;
    private final FilamentoService filamentoService;
    private final ImpressoraService impressoraService;
    private final EmbalagemService embalagemService;
    private final ConfiguracaoService configuracaoService;
    private final VerificacaoTarifasPlataforma verificacaoTarifas;

    public CalculoHistoricoController(
            CatalogoBuscaService catalogoBuscaService,
            CalculoService calculoService, FotoCalculoService fotoCalculoService, HistoricoCalculoService historicoCalculoService,
            PecaService pecaService,
            FilamentoService filamentoService,
            ImpressoraService impressoraService,
            EmbalagemService embalagemService,
            ConfiguracaoService configuracaoService,
            VerificacaoTarifasPlataforma verificacaoTarifas,
            PecaArquivoService pecaArquivoService
    ) {
        this.catalogoBuscaService = catalogoBuscaService;
        this.calculoService = calculoService;
        this.fotoCalculoService = fotoCalculoService;
        this.historicoCalculoService = historicoCalculoService;
        this.pecaService = pecaService;
        this.pecaArquivoService = pecaArquivoService;
        this.filamentoService = filamentoService;
        this.impressoraService = impressoraService;
        this.embalagemService = embalagemService;
        this.configuracaoService = configuracaoService;
        this.verificacaoTarifas = verificacaoTarifas;
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
            @PageableDefault(page = 0, size = 10) Pageable pageable) {

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
        mv.addObject("tamanhoPagina", pageable.getPageSize());

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
            @RequestParam(required = false) UUID orcamento,
            HttpSession session) {

        if (!editar && !novo && pecaCriada == null && filamentoCriado == null && impressoraCriada == null && embalagemCriada == null) {
            session.removeAttribute("orcamentoDestino");
        }
        if (novo) {
            session.removeAttribute("orcamentoDestino");
            if (orcamento != null) session.setAttribute("orcamentoDestino", orcamento);
            session.removeAttribute("custoReferencia");
            session.removeAttribute("pecaReferencia");
            session.removeAttribute(REQUEST_CALCULO);
            session.removeAttribute(RESULTADO_CALCULO);
            session.removeAttribute(CALCULO_PARA_EDITAR);
            fotoRascunhoService.excluir(session.getAttribute(FOTO_SESSAO));
            session.removeAttribute(FOTO_SESSAO);
            session.removeAttribute(FOTO_TIPO_SESSAO);
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
                request.setMarkupPercentual(configuracao.getMarkupOpcao1());
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

        adicionarPreviewFoto(mv, request, session);
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
            aplicarFoto(request, fotoPeca, session);

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
                mv.setStatus(HttpStatusCode.valueOf(422));
                mv.addObject("calculo", request);
                mv.addObject("respostaCalculoComErro", true);
                adicionarPreviewFoto(mv, request, session);
                carregarDadosFormulario(mv);
                return mv;
            }

            CalculoHistorico calculo = calculoService.calcular(request);

            guardarFotoRascunho(session, request.getFotoPecaContentType(), request.getFotoPecaBytes());
            session.setAttribute(FOTO_TIPO_SESSAO, request.getFotoPecaContentType());
            CalculoResponse resposta = CalculoResponse.toDto(calculo);
            resposta.setTemFoto(request.getFotoPecaBytes() != null && request.getFotoPecaBytes().length > 0);
            request.setFotoPecaBytes(null);
            request.setFotoPecaContentType(null);
            session.setAttribute(REQUEST_CALCULO, request);
            session.setAttribute(
                    RESULTADO_CALCULO,
                    resposta
            );

            return ResponseEntity
                    .status(HttpStatus.SEE_OTHER)
                    .location(URI.create("/historico/resultado"))
                    .build();

        } catch (IllegalArgumentException | IOException e) {
            boolean precisaFreteManual = e instanceof br.com.criare3d.service.FreteManualNecessarioException;

            if (precisaFreteManual) {
                request.setFreteMercadoLivreManualNecessario(true);
            }

            ModelAndView mv = new ModelAndView("historico/novo");
            mv.setStatus(HttpStatusCode.valueOf(422));
            mv.addObject("calculo", request);
            mv.addObject("respostaCalculoComErro", true);

            if (!precisaFreteManual) {
                mv.addObject("erro", e instanceof IOException
                        ? "Não foi possível processar a foto. Tente selecionar a imagem novamente."
                        : e.getMessage());
            }

            adicionarPreviewFoto(mv, request, session);
            carregarDadosFormulario(mv);

            return mv;
        }
    }

    @PostMapping("/historico/previa")
    @ResponseBody
    public ResponseEntity<?> previaCustos(@ModelAttribute CalculoRequest request) {
        try {
            if (request.getMarkupPercentual() == null) request.setMarkupPercentual(BigDecimal.ZERO);
            CalculoHistorico previa = calculoService.calcular(request);
            int unidades = request.getUnidades() == null ? 1 : request.getUnidades();
            int impressoes = previa.getFornadas() == null ? 1 : previa.getFornadas();
            int aquecimento = previa.getTempoAquecimentoMinutos() == null
                    ? 0 : previa.getTempoAquecimentoMinutos();
            long minutosMaquina = previa.getMinutosMaquinaLote() == null
                    ? br.com.criare3d.service.ModeloImpressao.minutosTotais(
                        previa.getTempoImpressaoMinutos() == null ? 0 : previa.getTempoImpressaoMinutos(),
                        aquecimento, unidades,
                        request.getPecasPorImpressao() == null ? 1 : request.getPecasPorImpressao())
                    : previa.getMinutosMaquinaLote();
            return ResponseEntity.ok(java.util.Map.of(
                    "custoUnidade", previa.getCustoTotal().setScale(2, RoundingMode.HALF_UP),
                    "custoLote", previa.getCustoTotal().multiply(BigDecimal.valueOf(unidades))
                            .setScale(2, RoundingMode.HALF_UP),
                    "impressoes", impressoes,
                    "minutosMaquina", minutosMaquina,
                    "tempoAquecimentoPorImpressao", aquecimento));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.unprocessableEntity().body(java.util.Map.of("erro", ex.getMessage()));
        }
    }

    @GetMapping("/historico/resultado")
    public ModelAndView resultado(
            HttpSession session,
            @RequestParam(defaultValue = "LUCRO_DESEJADO") String modoComparacao,
            @RequestParam(required = false) BigDecimal precoVendaFixo,
            @RequestParam(defaultValue = "false") boolean valoresAtualizados
    ) {
        CalculoRequest request =
                (CalculoRequest) session.getAttribute(REQUEST_CALCULO);

        CalculoResponse resultado =
                (CalculoResponse) session.getAttribute(RESULTADO_CALCULO);

        if (request == null || resultado == null) {
            return new ModelAndView("redirect:/historico/novo?novo=true");
        }

        ModelAndView mv = presenter.resultado(request, resultado, modoComparacao, precoVendaFixo, valoresAtualizados);
        mv.addObject("orcamentoDestino", session.getAttribute("orcamentoDestino"));
        if (request.getPecaId().equals(session.getAttribute("pecaReferencia"))) {
            mv.addObject("custoReferencia", session.getAttribute("custoReferencia"));
        }

        adicionarPreviewFoto(mv, request, session);

        return mv;
    }

    @PostMapping("/historico/editar")
    public ResponseEntity<Void> editar(
            @ModelAttribute("calculo") CalculoRequest request,
            HttpSession session) {

        // A foto permanece no armazenamento temporário; a sessão guarda somente seu identificador.
        request.setFotoRemovida(false);

        request.setFotoPecaBytes(null);
        request.setFotoPecaContentType(null);
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
            mv.setStatus(HttpStatusCode.valueOf(422));
            adicionarPreviewFoto(mv, request, session);
            carregarDadosFormulario(mv);
            return mv;
        }

        try {
            CalculoResponse resultadoAnterior = (CalculoResponse) session.getAttribute(RESULTADO_CALCULO);
            request.setFotoPecaBytes(fotoRascunhoService.buscar(session.getAttribute(FOTO_SESSAO)).map(br.com.criare3d.dto.FotoConteudo::bytes).orElse(null));
            request.setFotoPecaContentType((String) session.getAttribute(FOTO_TIPO_SESSAO));
            if (session.getAttribute(FOTO_SESSAO) instanceof UUID && request.getFotoPecaBytes() == null) {
                throw new IllegalArgumentException("A foto temporária expirou ou foi substituída em outra aba. Anexe a foto novamente antes de salvar.");
            }
            CalculoHistorico calculo = calculoService.calcular(request);

            if (resultadoAnterior != null
                    && (resultadoAnterior.getCustoTotal().compareTo(calculo.getCustoTotal()) != 0
                    || resultadoAnterior.getPrecoSugerido().compareTo(calculo.getPrecoSugerido()) != 0)) {
                CalculoResponse atualizado = CalculoResponse.toDto(calculo);
                atualizado.setTemFoto(request.getFotoPecaBytes() != null);
                request.setFotoPecaBytes(null);
                request.setFotoPecaContentType(null);
                session.setAttribute(REQUEST_CALCULO, request);
                session.setAttribute(RESULTADO_CALCULO, atualizado);
                return new ModelAndView("redirect:/historico/resultado?valoresAtualizados=true");
            }

            byte[] foto = fotoRascunhoService.buscar(session.getAttribute(FOTO_SESSAO)).map(br.com.criare3d.dto.FotoConteudo::bytes).orElse(null);
            String tipoFoto = (String) session.getAttribute(FOTO_TIPO_SESSAO);
            calculo.setTemFoto(foto != null && foto.length > 0);

            historicoCalculoService.create(calculo, tipoFoto, foto);

            session.removeAttribute("custoReferencia");
            session.removeAttribute("pecaReferencia");

            session.removeAttribute(REQUEST_CALCULO);
            session.removeAttribute(RESULTADO_CALCULO);
            fotoRascunhoService.excluir(session.getAttribute(FOTO_SESSAO));
            session.removeAttribute(FOTO_SESSAO);
            session.removeAttribute(FOTO_TIPO_SESSAO);

            return new ModelAndView("redirect:/historico/listar?sucesso=historicoSalvo");

        } catch (IllegalArgumentException e) {
            mv.setStatus(HttpStatusCode.valueOf(422));
            mv.addObject("erro", e.getMessage());
            adicionarPreviewFoto(mv, request, session);
            carregarDadosFormulario(mv);

            return mv;
        }
    }

    @GetMapping("/historico/detalhes/{id}")
    public ModelAndView fetchById(@PathVariable UUID id) {

        return historicoCalculoService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("historico/detalhes");
                    CalculoResponse historico = CalculoResponse.toDto(entidade);
                    mv.addObject("historico", historico);
                    presenter.adicionarResumoLote(mv, historico);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/historico/listar"));
    }

    @GetMapping("/historico/{id}/foto")
    @ResponseBody
    public ResponseEntity<byte[]> buscarFoto(@PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean miniatura,
            @RequestHeader(value = "If-None-Match", required = false) String etagAtual) {
        return fotoCalculoService.buscarConteudoFoto(id, miniatura).map(foto -> {
            byte[] bytes = foto.bytes();
            String etag = etag(bytes);
            if (etag.equals(etagAtual)) return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .header("ETag", etag).<byte[]>build();
            return ResponseEntity.ok().header("ETag", etag)
                    .header("Cache-Control", "private, no-cache, must-revalidate")
                    .contentType(org.springframework.http.MediaType.parseMediaType(foto.tipo()))
                    .body(bytes);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private String etag(byte[] bytes) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
            return "\"" + java.util.HexFormat.of().formatHex(digest) + "\"";
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 não está disponível", impossible);
        }
    }

    @PostMapping("/historico/rascunho/foto")
    @ResponseBody
    public ResponseEntity<Void> sincronizarFotoRascunho(
            @RequestParam(value = "fotoPeca", required = false) MultipartFile fotoPeca,
            @RequestParam(value = "remover", defaultValue = "false") boolean remover,
            HttpSession session) {
        if (remover) {
            fotoRascunhoService.excluir(session.getAttribute(FOTO_SESSAO));
            session.removeAttribute(FOTO_SESSAO);
            session.removeAttribute(FOTO_TIPO_SESSAO);
            return ResponseEntity.noContent().build();
        }
        if (fotoPeca == null || fotoPeca.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            aplicarFoto(new CalculoRequest(), fotoPeca, session);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IOException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422)).build();
        }
    }

    @GetMapping("/historico/resultado/foto")
    @ResponseBody
    public ResponseEntity<byte[]> buscarFotoTemporaria(HttpSession session) {
        byte[] bytes = fotoRascunhoService.buscar(session.getAttribute(FOTO_SESSAO)).map(br.com.criare3d.dto.FotoConteudo::bytes).orElse(null);
        String tipo = (String) session.getAttribute(FOTO_TIPO_SESSAO);
        if (bytes == null || tipo == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().header("Cache-Control", "private, no-store")
                .contentType(org.springframework.http.MediaType.parseMediaType(tipo)).body(bytes);
    }

    private void aplicarFoto(CalculoRequest request, MultipartFile fotoPeca, HttpSession session)
            throws IOException {

        if (fotoPeca == null || fotoPeca.isEmpty()) {
            if (request.isFotoRemovida()) {
                request.setFotoPecaBytes(null);
                request.setFotoPecaContentType(null);
                fotoRascunhoService.excluir(session.getAttribute(FOTO_SESSAO));
            session.removeAttribute(FOTO_SESSAO);
                session.removeAttribute(FOTO_TIPO_SESSAO);
                return;
            }
            if (request.getFotoPecaBytes() == null) {
                request.setFotoPecaBytes(fotoRascunhoService.buscar(session.getAttribute(FOTO_SESSAO)).map(br.com.criare3d.dto.FotoConteudo::bytes).orElse(null));
                request.setFotoPecaContentType((String) session.getAttribute(FOTO_TIPO_SESSAO));
            }
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

        request.setFotoPecaBytes(bytes);
        request.setFotoPecaContentType(contentType);
        guardarFotoRascunho(session, contentType, bytes);
        session.setAttribute(FOTO_TIPO_SESSAO, contentType);
    }

    private void guardarFotoRascunho(HttpSession session, String tipo, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            fotoRascunhoService.excluir(session.getAttribute(FOTO_SESSAO));
            session.removeAttribute(FOTO_SESSAO);
            return;
        }
        session.setAttribute(FOTO_SESSAO, fotoRascunhoService.salvar(tipo, bytes));
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

    private void adicionarPreviewFoto(ModelAndView mv, CalculoRequest request, HttpSession session) {
        boolean fotoNaRequest = request.getFotoPecaBytes() != null
                && request.getFotoPecaContentType() != null;
        boolean fotoNaSessao = session.getAttribute(FOTO_SESSAO) instanceof UUID
                && session.getAttribute(FOTO_TIPO_SESSAO) instanceof String tipo && !tipo.isBlank();
        if (fotoNaRequest || fotoNaSessao)
            mv.addObject("fotoPecaPreview", "/historico/resultado/foto");
    }

    private void carregarDadosFormulario(ModelAndView mv) {

        Pageable pageable = PageRequest.of(0, 1000);

        var pecas = pecaService.fetchAll(pageable).getContent();
        mv.addObject("pecas", pecas);
        mv.addObject("arquivosPeca", pecaArquivoService.buscarResumos(pecas));
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
