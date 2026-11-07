
package br.com.criare3d.controller;

import br.com.criare3d.dto.PecaRequest;
import br.com.criare3d.dto.PecaResponse;
import br.com.criare3d.models.Filamento;
import br.com.criare3d.models.Peca;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.PecaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;
import java.util.List;

@Controller
public class PecaController {

    @Autowired
    private PecaService pecaService;

    @Autowired
    private CalculoHistoricoService calculoHistoricoService;

    @Autowired
    private CatalogoBuscaService catalogoBuscaService;

    @GetMapping("/peca/listar")
    public ModelAndView fetchAll(@RequestParam(name = "busca", defaultValue = "") String busca, @PageableDefault(page = 0, size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {

        ModelAndView mv = new ModelAndView("peca/listar");

        Page<Peca> pagina = busca.isBlank()
                ? pecaService.fetchAll(pageable)
                : catalogoBuscaService.buscarPecas(busca.trim(), pageable);

        mv.addObject("pecas", pagina.getContent());
        mv.addObject("pagina", pagina);
        mv.addObject("busca", busca);

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
        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("peca/novo");
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            mv.addObject("peca", request);
            mv.addObject("origem", origem);
            return mv;
        }

        Peca entidade = pecaService.create(PecaRequest.toEntity(request));

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

        long quantidade = calculoHistoricoService.contarPorPeca(id);

        if (quantidade > 0) {
            return new ModelAndView(
                    "redirect:/peca/listar?erro=pecaEmUso&quantidade=" + quantidade
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
            RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) return new ModelAndView("redirect:/peca/listar?erro=nenhumaPecaSelecionada");
        List<UUID> bloqueadas = pecaService.deleteUnused(ids);
        if (!bloqueadas.isEmpty()) redirect.addFlashAttribute("idsNaoExcluidos", bloqueadas);
        redirect.addAttribute("busca", busca);
        redirect.addAttribute("page", page);
        if (!bloqueadas.isEmpty()) redirect.addAttribute("erro", "pecasEmUso");
        else redirect.addAttribute("sucesso", "pecasRemovidas");
        return new ModelAndView("redirect:/peca/listar");
    }

    @PostMapping("/peca/atualizar/{id}")
    public ModelAndView atualizarPeca(@PathVariable UUID id, @Valid @ModelAttribute("peca") PecaRequest request, BindingResult bd) {
        if (bd.hasErrors()) {
            if (!pecaService.existsById(id)) return new ModelAndView("redirect:/peca/listar");
            ModelAndView mv = new ModelAndView("peca/edicao");
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            mv.addObject("peca", request);
            mv.addObject("id", id);
            return mv;
        }

        if (pecaService.update(id, PecaRequest.toEntity(request)).isEmpty()) {
            return new ModelAndView("redirect:/peca/listar");
        }

        return new ModelAndView("redirect:/peca/listar?sucesso=pecaAtualizada");
    }
}
