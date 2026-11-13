
package br.com.criare3d.controller;

import br.com.criare3d.service.HistoricoCalculoService;

import br.com.criare3d.dto.PecaRequest;
import br.com.criare3d.dto.PecaResponse;
import br.com.criare3d.dto.PecaMaterialResumo;
import br.com.criare3d.models.Filamento;
import br.com.criare3d.models.Peca;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.PecaArquivoService;

import br.com.criare3d.service.interfaces.PecaService;
import br.com.criare3d.util.OrdenacaoPaginada;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;
import java.util.List;

@Controller
public class PecaController {

    private final PecaService pecaService;
    private final HistoricoCalculoService historicoCalculoService;
    private final CatalogoBuscaService catalogoBuscaService;
    private final PecaArquivoService pecaArquivoService;

    public PecaController(PecaService pecaService,
                          HistoricoCalculoService historicoCalculoService,
                          CatalogoBuscaService catalogoBuscaService,
                          PecaArquivoService pecaArquivoService) {
        this.pecaService = pecaService;
        this.historicoCalculoService = historicoCalculoService;
        this.catalogoBuscaService = catalogoBuscaService;
        this.pecaArquivoService = pecaArquivoService;
    }

    @GetMapping("/peca/listar")
    public ModelAndView fetchAll(@RequestParam(name = "busca", defaultValue = "") String busca, @PageableDefault(page = 0, size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {

        ModelAndView mv = new ModelAndView("peca/listar");

        Page<Peca> pagina = busca.isBlank()
                ? pecaService.fetchAll(pageable)
                : catalogoBuscaService.buscarPecas(busca.trim(), pageable);

        mv.addObject("pecas", pagina.getContent());
        mv.addObject("pagina", pagina);
        mv.addObject("busca", busca);
        mv.addObject("precosPeca", historicoCalculoService.ultimosPrecosPecas(pagina.getContent().stream().map(Peca::getId).toList()));
        mv.addObject("arquivosPeca", pecaArquivoService.buscarResumos(pagina.getContent()));

        return mv;
    }

    @GetMapping("/peca/novo")
    public ModelAndView retornarPaginaCadastro(@RequestParam(defaultValue = "") String origem) {
        ModelAndView mv = new ModelAndView("peca/novo");
        mv.addObject("peca", new PecaRequest());
        mv.addObject("origem", origem);

        return mv;
    }

    @PostMapping("/peca/cadastrar")
    public ModelAndView create(@Valid @ModelAttribute("peca") PecaRequest request, BindingResult bd, @RequestParam(defaultValue = "") String origem) {
        PecaArquivoService.Preparacao arquivo = prepararArquivo(request, bd);
        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("peca/novo");
            mv.setStatus(HttpStatusCode.valueOf(422));
            mv.addObject("peca", request);
            mv.addObject("origem", origem);
            return mv;
        }

        Peca entidade = pecaService.create(PecaRequest.toEntity(request));
        arquivoSalvo(entidade, arquivo);

        if ("calculo".equals(origem)) {
            return new ModelAndView("redirect:/historico/novo?pecaCriada=" + entidade.getId());
        }

        return new ModelAndView("redirect:/peca/listar?sucesso=pecaCriada");
    }

    @GetMapping("/peca/detalhes/{id}")
    public ModelAndView fetchById(@PathVariable UUID id) {
        return pecaService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("peca/detalhes");
                    mv.addObject("peca", entidade);
                    mv.addObject("arquivoPeca", pecaArquivoService.buscarResumo(entidade).orElse(null));
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/peca/listar"));
    }

    @GetMapping("/peca/editar/{id}")
    public ModelAndView retornarPaginaEdicao(@PathVariable UUID id) {
        return pecaService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("peca/edicao");
                    mv.addObject("peca", PecaResponse.toDto(entidade));
                    mv.addObject("id", id);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/peca/listar"));
    }

    @PostMapping("/peca/remover/{id}")
    public ModelAndView deleteById(@PathVariable UUID id) {
        if (!pecaService.existsById(id)) {
            return new ModelAndView("redirect:/peca/listar?erro=pecaNaoEncontrada");
        }

        long quantidade = historicoCalculoService.contarPorPeca(id);

        if (quantidade > 0) {
            return new ModelAndView(
                    "redirect:/peca/listar?erro=pecaEmUso&bloqueado=" + id + "&quantidade=" + quantidade
            );
        }

        pecaService.delete(id);
        return new ModelAndView("redirect:/peca/listar?sucesso=pecaRemovida");
    }

    @PostMapping("/peca/remover-lote")
    public ModelAndView deleteSelected(
            @RequestParam(name = "ids", required = false) List<UUID> ids,
            @RequestParam(name = "busca", defaultValue = "") String busca,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "totalRegistros", defaultValue = "0") long totalRegistros,
            @RequestParam(name = "tamanhoPagina", defaultValue = "10") int tamanhoPagina,
            RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) return new ModelAndView("redirect:/peca/listar?erro=nenhumaPecaSelecionada");
        List<UUID> bloqueadas = pecaService.deleteUnused(ids);
        if (!bloqueadas.isEmpty()) redirect.addFlashAttribute("idsNaoExcluidos", bloqueadas);
        page = OrdenacaoPaginada.ajustarPaginaAposExclusao(
                page, totalRegistros, ids.size() - bloqueadas.size(), tamanhoPagina);
        redirect.addAttribute("busca", busca);
        redirect.addAttribute("page", page);
        if (!bloqueadas.isEmpty()) redirect.addAttribute("erro", "pecasEmUso");
        else redirect.addAttribute("sucesso", "pecasRemovidas");
        return new ModelAndView("redirect:/peca/listar");
    }

    @PostMapping("/peca/atualizar/{id}")
    public ModelAndView atualizarPeca(@PathVariable UUID id, @Valid @ModelAttribute("peca") PecaRequest request, BindingResult bd) {
        PecaArquivoService.Preparacao arquivo = prepararArquivo(request, bd);
        if (bd.hasErrors()) {
            if (!pecaService.existsById(id)) return new ModelAndView("redirect:/peca/listar");
            ModelAndView mv = new ModelAndView("peca/edicao");
            mv.setStatus(HttpStatusCode.valueOf(422));
            mv.addObject("peca", request);
            mv.addObject("id", id);
            return mv;
        }

        var atualizada = pecaService.update(id, PecaRequest.toEntity(request));
        if (atualizada.isEmpty()) {
            return new ModelAndView("redirect:/peca/listar");
        }
        arquivoSalvo(atualizada.get(), arquivo);

        return new ModelAndView("redirect:/peca/listar?sucesso=pecaAtualizada");
    }

    @GetMapping("/peca/{id}/arquivo/placa/{numero}")
    @ResponseBody
    public ResponseEntity<byte[]> imagemPlaca(@PathVariable UUID id, @PathVariable int numero) {
        if (numero < 1 || numero > 8 || pecaService.fetchById(id).isEmpty()) return ResponseEntity.notFound().build();
        return pecaArquivoService.buscarImagem(id, numero)
                .map(imagem -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(imagem.mimeType()))
                        .cacheControl(CacheControl.noCache().cachePrivate())
                        .body(imagem.bytes()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/peca/{id}/materiais-calculo")
    @ResponseBody
    public ResponseEntity<List<PecaMaterialResumo>> materiaisParaCalculo(@PathVariable UUID id) {
        return pecaService.fetchById(id)
                .map(peca -> ResponseEntity.ok(pecaArquivoService.buscarMateriaisParaCalculo(peca)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private PecaArquivoService.Preparacao prepararArquivo(PecaRequest request, BindingResult bd) {
        try {
            var preparacao = pecaArquivoService.preparar(request.getDadosArquivo3mf());
            request.setDadosArquivo3mf(preparacao.map(PecaArquivoService.Preparacao::json).orElse(null));
            return preparacao.orElse(null);
        } catch (IllegalArgumentException e) {
            bd.rejectValue("dadosArquivo3mf", "peca.arquivo.invalido", e.getMessage());
            return null;
        }
    }

    private void arquivoSalvo(Peca peca, PecaArquivoService.Preparacao arquivo) {
        if (arquivo != null) pecaArquivoService.salvar(peca.getId(), arquivo);
    }
}
