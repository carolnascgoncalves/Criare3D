package br.com.criare3d.controller;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.CalculoResponse;
import br.com.criare3d.dto.HistoricoListaResponse;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.service.*;
import br.com.criare3d.service.interfaces.*;
import br.com.criare3d.util.HistoricoFiltros;
import br.com.criare3d.util.OrdenacaoPaginada;
import br.com.criare3d.dto.RascunhoCalculo;
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
import java.util.UUID;

@Controller
public class CalculoHistoricoController {
    @org.springframework.beans.factory.annotation.Autowired
    private br.com.criare3d.service.ResultadoCalculoPresenter presenter;

    private static final String RESULTADO_CALCULO = "resultadoCalculo";
    private static final String REQUEST_CALCULO = "requestCalculo";
    private static final String CALCULO_PARA_EDITAR = "calculoParaEditar";
    private static final String FOTO_RASCUNHO = "fotoCalculoId";
    @org.springframework.beans.factory.annotation.Autowired
    private br.com.criare3d.service.FotoRascunhoService fotoRascunhoService;
    private static final String FOTO_TIPO_RASCUNHO = "fotoCalculoTipo";

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
        RascunhoCalculo estado) {

        if (novo) {
            estado.removeAttribute("formulario");
            estado.removeAttribute("orcamentoDestino");
            if (orcamento != null) estado.setAttribute("orcamentoDestino", orcamento);
            estado.removeAttribute("custoReferencia");
            estado.removeAttribute("pecaReferencia");
            estado.removeAttribute(REQUEST_CALCULO);
            estado.removeAttribute(RESULTADO_CALCULO);
            estado.removeAttribute("formulario");
            estado.removeAttribute(CALCULO_PARA_EDITAR);
            fotoRascunhoService.excluir(estado.getAttribute(FOTO_RASCUNHO));
            estado.removeAttribute(FOTO_RASCUNHO);
            estado.removeAttribute(FOTO_TIPO_RASCUNHO);
        }

        ModelAndView mv = new ModelAndView("historico/novo");

        CalculoRequest request;

        if (editar && estado.getAttribute(CALCULO_PARA_EDITAR) != null) {
            request = (CalculoRequest) estado.getAttribute(CALCULO_PARA_EDITAR);
            estado.removeAttribute(CALCULO_PARA_EDITAR);
            estado.setAttribute(REQUEST_CALCULO, request);
        } else if (estado.getDados().getFormulario() != null) {
            request = CalculoRascunhoService.formulario(estado.getDados().getFormulario());
        } else if (estado.getAttribute(REQUEST_CALCULO) instanceof CalculoRequest existente) {
            request = existente;
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

        adicionarPreviewFoto(mv, request, estado);
        carregarDadosFormulario(mv);

        return mv;
    }

    /*
     * O cálculo segue o padrão POST -> GET.
     * O resultado fica no rascunho persistente e o POST responde com HTTP 303.
     */
    @PostMapping("/historico/calcular")
    public Object calcular(
        @Valid @ModelAttribute("calculo") CalculoRequest request,
        BindingResult bd,
        @RequestParam(value = "fotoPeca", required = false) MultipartFile fotoPeca,
        RascunhoCalculo estado) {

        try {
            aplicarFoto(request, fotoPeca, estado);

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
                adicionarPreviewFoto(mv, request, estado);
                carregarDadosFormulario(mv);
                return mv;
            }

            var calculo = calculoService.prever(request);

            guardarFotoRascunho(estado, request.getFotoPecaContentType(), request.getFotoPecaBytes());
            estado.setAttribute(FOTO_TIPO_RASCUNHO, request.getFotoPecaContentType());
            CalculoResponse resposta = CalculoMapper.resposta(calculo);
            resposta.setTemFoto(request.getFotoPecaBytes() != null && request.getFotoPecaBytes().length > 0);
            request.setFotoPecaBytes(null);
            request.setFotoPecaContentType(null);
            estado.setAttribute(REQUEST_CALCULO, request);
            estado.setAttribute(
                RESULTADO_CALCULO,
                resposta
            );

            return ResponseEntity
                .status(HttpStatus.SEE_OTHER)
                .location(URI.create("/historico/resultado?rascunhoId=" + estado.getId()))
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

            adicionarPreviewFoto(mv, request, estado);
            carregarDadosFormulario(mv);

            return mv;
        }
    }

    @PostMapping("/historico/previa")
    @ResponseBody
    public ResponseEntity<?> previaCustos(@ModelAttribute CalculoRequest request) {
        try {
            if (request.getMarkupPercentual() == null) request.setMarkupPercentual(BigDecimal.ZERO);
            var previa = calculoService.prever(request);
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
        RascunhoCalculo estado,
        @RequestParam(defaultValue = "LUCRO_DESEJADO") String modoComparacao,
        @RequestParam(required = false) BigDecimal precoVendaFixo,
        @RequestParam(defaultValue = "false") boolean valoresAtualizados
    ) {
        CalculoRequest request =
            (CalculoRequest) estado.getAttribute(REQUEST_CALCULO);

        CalculoResponse resultado =
            (CalculoResponse) estado.getAttribute(RESULTADO_CALCULO);

        if (request == null || resultado == null) {
            return new ModelAndView("redirect:/historico/novo?novo=true");
        }

        ModelAndView mv = presenter.resultado(request, resultado, modoComparacao, precoVendaFixo, valoresAtualizados);
        mv.addObject("orcamentoDestino", estado.getAttribute("orcamentoDestino"));
        if (request.getPecaId().equals(estado.getAttribute("pecaReferencia"))) {
            mv.addObject("custoReferencia", estado.getAttribute("custoReferencia"));
        }

        adicionarPreviewFoto(mv, request, estado);

        return mv;
    }

    @PostMapping("/historico/editar")
    public ResponseEntity<Void> editar(
        @ModelAttribute("calculo") CalculoRequest request,
        RascunhoCalculo estado) {

        // A foto permanece no armazenamento temporário; o rascunho guarda somente seu identificador.
        request.setFotoRemovida(false);

        request.setFotoPecaBytes(null);
        request.setFotoPecaContentType(null);
        estado.removeAttribute("formulario");
        estado.setAttribute(CALCULO_PARA_EDITAR, request);

        return ResponseEntity
            .status(HttpStatus.SEE_OTHER)
            .location(URI.create("/historico/novo?editar=true&rascunhoId=" + estado.getId()))
            .build();
    }

    @PostMapping("/historico/salvar")
    public ModelAndView salvar(
        @Valid @ModelAttribute("calculo") CalculoRequest request,
        BindingResult bd,
        RascunhoCalculo estado) {

        ModelAndView mv = new ModelAndView("historico/novo");
        mv.addObject("calculo", request);

        if (bd.hasErrors()) {
            mv.setStatus(HttpStatusCode.valueOf(422));
            mv.addObject("respostaCalculoComErro", true);
            adicionarPreviewFoto(mv, request, estado);
            carregarDadosFormulario(mv);
            return mv;
        }

        try {
            CalculoResponse resultadoAnterior = (CalculoResponse) estado.getAttribute(RESULTADO_CALCULO);
            request.setFotoPecaBytes(fotoRascunhoService.buscar(estado.getAttribute(FOTO_RASCUNHO)).map(br.com.criare3d.dto.FotoConteudo::bytes).orElse(null));
            request.setFotoPecaContentType((String) estado.getAttribute(FOTO_TIPO_RASCUNHO));
            if (estado.getAttribute(FOTO_RASCUNHO) instanceof UUID && request.getFotoPecaBytes() == null) {
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
                estado.setAttribute(REQUEST_CALCULO, request);
                estado.setAttribute(RESULTADO_CALCULO, atualizado);
                return new ModelAndView("redirect:/historico/resultado?valoresAtualizados=true");
            }

            byte[] foto = fotoRascunhoService.buscar(estado.getAttribute(FOTO_RASCUNHO)).map(br.com.criare3d.dto.FotoConteudo::bytes).orElse(null);
            String tipoFoto = (String) estado.getAttribute(FOTO_TIPO_RASCUNHO);
            calculo.setTemFoto(foto != null && foto.length > 0);

            historicoCalculoService.create(calculo, tipoFoto, foto);

            estado.removeAttribute("custoReferencia");
            estado.removeAttribute("pecaReferencia");

            estado.removeAttribute(REQUEST_CALCULO);
            estado.removeAttribute(RESULTADO_CALCULO);
            estado.removeAttribute("formulario");
            fotoRascunhoService.excluir(estado.getAttribute(FOTO_RASCUNHO));
            estado.removeAttribute(FOTO_RASCUNHO);
            estado.removeAttribute(FOTO_TIPO_RASCUNHO);

            return new ModelAndView("redirect:/historico/listar?sucesso=historicoSalvo");

        } catch (IllegalArgumentException e) {
            mv.setStatus(HttpStatusCode.valueOf(422));
            mv.addObject("respostaCalculoComErro", true);
            mv.addObject("erro", e.getMessage());
            adicionarPreviewFoto(mv, request, estado);
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

    @PostMapping("/historico/rascunho")
    @ResponseBody
    public ResponseEntity<Void> salvarFormularioRascunho(
        @Valid @RequestBody br.com.criare3d.dto.RascunhoFormulario formulario, RascunhoCalculo estado) {
        estado.setAttribute("formulario", formulario);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/historico/rascunho/foto")
    @ResponseBody
    public ResponseEntity<Void> sincronizarFotoRascunho(
        @RequestParam(value = "fotoPeca", required = false) MultipartFile fotoPeca,
        @RequestParam(value = "remover", defaultValue = "false") boolean remover,
        RascunhoCalculo estado) {
        if (remover) {
            fotoRascunhoService.excluir(estado.getAttribute(FOTO_RASCUNHO));
            estado.removeAttribute(FOTO_RASCUNHO);
            estado.removeAttribute(FOTO_TIPO_RASCUNHO);
            return ResponseEntity.noContent().build();
        }
        if (fotoPeca == null || fotoPeca.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            aplicarFoto(new CalculoRequest(), fotoPeca, estado);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IOException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422)).build();
        }
    }

    @GetMapping("/historico/resultado/foto")
    @ResponseBody
    public ResponseEntity<byte[]> buscarFotoTemporaria(RascunhoCalculo estado) {
        byte[] bytes = fotoRascunhoService.buscar(estado.getAttribute(FOTO_RASCUNHO)).map(br.com.criare3d.dto.FotoConteudo::bytes).orElse(null);
        String tipo = (String) estado.getAttribute(FOTO_TIPO_RASCUNHO);
        if (bytes == null || tipo == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().header("Cache-Control", "private, no-store")
            .contentType(org.springframework.http.MediaType.parseMediaType(tipo)).body(bytes);
    }

    private void aplicarFoto(CalculoRequest request, MultipartFile fotoPeca, RascunhoCalculo estado)
        throws IOException {

        if (fotoPeca == null || fotoPeca.isEmpty()) {
            if (request.isFotoRemovida()) {
                request.setFotoPecaBytes(null);
                request.setFotoPecaContentType(null);
                fotoRascunhoService.excluir(estado.getAttribute(FOTO_RASCUNHO));
                estado.removeAttribute(FOTO_RASCUNHO);
                estado.removeAttribute(FOTO_TIPO_RASCUNHO);
                return;
            }
            if (request.getFotoPecaBytes() == null) {
                request.setFotoPecaBytes(fotoRascunhoService.buscar(estado.getAttribute(FOTO_RASCUNHO)).map(br.com.criare3d.dto.FotoConteudo::bytes).orElse(null));
                request.setFotoPecaContentType((String) estado.getAttribute(FOTO_TIPO_RASCUNHO));
            }
            return;
        }

        if (fotoPeca.getSize() > 3 * 1024 * 1024) {
            throw new IllegalArgumentException("A foto deve ter no máximo 3 MB.");
        }

        byte[] bytes = fotoPeca.getBytes();
        String contentType = br.com.criare3d.dto.FotoConteudo.detectarTipoImagem(bytes);
        if (contentType == null) {
            throw new IllegalArgumentException(
                "O arquivo não parece ser uma imagem JPG, PNG ou WEBP válida."
            );
        }

        request.setFotoPecaBytes(bytes);
        request.setFotoPecaContentType(contentType);
        guardarFotoRascunho(estado, contentType, bytes);
        estado.setAttribute(FOTO_TIPO_RASCUNHO, contentType);
    }

    private void guardarFotoRascunho(RascunhoCalculo estado, String tipo, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            fotoRascunhoService.excluir(estado.getAttribute(FOTO_RASCUNHO));
            estado.removeAttribute(FOTO_RASCUNHO);
            return;
        }
        estado.setAttribute(FOTO_RASCUNHO, fotoRascunhoService.salvar(estado.getId(), tipo, bytes));
    }

    private void adicionarPreviewFoto(ModelAndView mv, CalculoRequest request, RascunhoCalculo estado) {
        boolean fotoNaRequest = request.getFotoPecaBytes() != null
            && request.getFotoPecaContentType() != null;
        boolean fotoNoRascunho = estado.getAttribute(FOTO_RASCUNHO) instanceof UUID
            && estado.getAttribute(FOTO_TIPO_RASCUNHO) instanceof String tipo && !tipo.isBlank();
        if (fotoNaRequest || fotoNoRascunho)
            mv.addObject("fotoPecaPreview", "/historico/resultado/foto?rascunhoId=" + estado.getId());
    }

    private void carregarDadosFormulario(ModelAndView mv) {

        Pageable pageable = PageRequest.of(0, 20);

        var pecas = new java.util.ArrayList<>(pecaService.fetchAll(pageable).getContent());
        var filamentos = new java.util.ArrayList<>(filamentoService.fetchAll(pageable).getContent());
        var impressoras = new java.util.ArrayList<>(impressoraService.fetchAll(pageable).getContent());
        var embalagens = new java.util.ArrayList<>(embalagemService.fetchAll(pageable).getContent());
        if (mv.getModel().get("calculo") instanceof CalculoRequest r) {
            if (r.getPecaId() != null && pecas.stream().noneMatch(p -> p.getId().equals(r.getPecaId())))
                pecaService.fetchById(r.getPecaId()).ifPresent(pecas::add);
            if (r.getImpressoraId() != null && impressoras.stream().noneMatch(p -> p.getId().equals(r.getImpressoraId())))
                impressoraService.fetchById(r.getImpressoraId()).ifPresent(impressoras::add);
            if (r.getEmbalagemId() != null && embalagens.stream().noneMatch(p -> p.getId().equals(r.getEmbalagemId())))
                embalagemService.fetchById(r.getEmbalagemId()).ifPresent(embalagens::add);
            if (r.getFilamentos() != null) for (var i : r.getFilamentos())
                if (i.getFilamentoId() != null && filamentos.stream().noneMatch(f -> f.getId().equals(i.getFilamentoId())))
                    filamentoService.fetchById(i.getFilamentoId()).ifPresent(filamentos::add);
        }
        for (String chave : java.util.List.of("pecaCriada", "filamentoCriado", "impressoraCriada", "embalagemCriada")) {
            Object valor = mv.getModel().get(chave);
            if (valor == null) continue;
            UUID criado = UUID.fromString(valor.toString());
            switch (chave) {
                case "pecaCriada" -> {
                    if (pecas.stream().noneMatch(p -> p.getId().equals(criado)))
                        pecaService.fetchById(criado).ifPresent(pecas::add);
                }
                case "filamentoCriado" -> {
                    if (filamentos.stream().noneMatch(p -> p.getId().equals(criado)))
                        filamentoService.fetchById(criado).ifPresent(filamentos::add);
                }
                case "impressoraCriada" -> {
                    if (impressoras.stream().noneMatch(p -> p.getId().equals(criado)))
                        impressoraService.fetchById(criado).ifPresent(impressoras::add);
                }
                case "embalagemCriada" -> {
                    if (embalagens.stream().noneMatch(p -> p.getId().equals(criado)))
                        embalagemService.fetchById(criado).ifPresent(embalagens::add);
                }
            }
        }
        mv.addObject("pecas", pecas);
        mv.addObject("arquivosPeca", pecaArquivoService.buscarResumos(pecas));
        mv.addObject("filamentos", filamentos);
        mv.addObject("impressoras", impressoras);
        mv.addObject("embalagens", embalagens);
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
