package br.com.criare3d.controller;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.CalculoResponse;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.service.CalculoHistoricoServiceImp;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.interfaces.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

@Controller
public class CalculoHistoricoController {

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
            @PageableDefault(page = 0, size = 5, sort = "dataCalculo", direction = Sort.Direction.DESC)
            Pageable pageable) {

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
    public ModelAndView retornarPaginaCadastro() {
        ModelAndView mv = new ModelAndView("historico/novo");

        CalculoRequest request = new CalculoRequest();
        request.getFilamentos().add(new ItemCalculoRequest());

        var configuracao = configuracaoService.buscarOuCriarConfiguracao();

        if (Boolean.TRUE.equals(configuracao.getMarkupFixo())) {
            request.setMarkupPercentual(configuracao.getMarkupPercentual());
        } else {
            request.setMarkupPercentual(new BigDecimal("30.00"));
        }

        mv.addObject("calculo", request);
        carregarDadosFormulario(mv);

        return mv;
    }

    @PostMapping("/historico/calcular")
    public ModelAndView calcular(
            @Valid @ModelAttribute("calculo") CalculoRequest request,
            BindingResult bd,
            @RequestParam(value = "fotoPeca", required = false) MultipartFile fotoPeca) {

        try {
            aplicarFoto(request, fotoPeca);

            if (bd.hasErrors()) {
                ModelAndView mv = new ModelAndView("historico/novo");
                mv.addObject("calculo", request);
                adicionarPreviewFoto(mv, request);
                carregarDadosFormulario(mv);
                return mv;
            }

            CalculoHistorico calculo = calculoHistoricoServiceImp.calcular(request);

            ModelAndView mv = new ModelAndView("historico/resultado");
            mv.addObject("calculo", request);
            mv.addObject("resultado", CalculoResponse.toDto(calculo));
            adicionarPreviewFoto(mv, request);

            return mv;

        } catch (IllegalArgumentException | IOException e) {
            ModelAndView mv = new ModelAndView("historico/novo");
            mv.addObject("calculo", request);

            mv.addObject("erro", e instanceof IOException
                    ? "Não foi possível processar a foto. Tente selecionar a imagem novamente."
                    : e.getMessage());

            adicionarPreviewFoto(mv, request);
            carregarDadosFormulario(mv);

            return mv;
        }
    }

    @PostMapping("/historico/editar")
    public ModelAndView editar(@ModelAttribute("calculo") CalculoRequest request) {
        ModelAndView mv = new ModelAndView("historico/novo");

        mv.addObject("calculo", request);
        adicionarPreviewFoto(mv, request);
        carregarDadosFormulario(mv);

        return mv;
    }

    @PostMapping("/historico/salvar")
    public ModelAndView salvar(
            @Valid @ModelAttribute("calculo") CalculoRequest request,
            BindingResult bd) {

        ModelAndView mv = new ModelAndView("historico/novo");
        mv.addObject("calculo", request);

        if (bd.hasErrors()) {
            adicionarPreviewFoto(mv, request);
            carregarDadosFormulario(mv);
            return mv;
        }

        try {
            CalculoHistorico calculo = calculoHistoricoServiceImp.calcular(request);

            calculo.setFotoPecaBase64(request.getFotoPecaBase64());
            calculo.setFotoPecaContentType(request.getFotoPecaContentType());

            calculoHistoricoService.create(calculo);

            return new ModelAndView("redirect:/historico/listar");

        } catch (IllegalArgumentException e) {
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

    private void aplicarFoto(CalculoRequest request, MultipartFile fotoPeca) throws IOException {
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