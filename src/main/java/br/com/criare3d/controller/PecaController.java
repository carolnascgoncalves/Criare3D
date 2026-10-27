package br.com.criare3d.controller;

import br.com.criare3d.dto.PecaRequest;
import br.com.criare3d.dto.PecaResponse;
import br.com.criare3d.models.Peca;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.PecaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
public class PecaController {

    @Autowired
    private PecaService pecaService;

    @Autowired
    private CalculoHistoricoService calculoHistoricoService;

    @GetMapping("/peca/listar")
    public ModelAndView fetchAll(@PageableDefault(page = 0, size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        ModelAndView mv = new ModelAndView("/peca/listar");
        mv.addObject("pecas", pecaService.fetchAll(pageable).getContent());
        return mv;
    }

    @GetMapping("/peca/novo")
    public ModelAndView retornarPaginaCadastro() {
        ModelAndView mv = new ModelAndView("/peca/novo");
        mv.addObject("peca", new PecaRequest());
        return mv;
    }

    @PostMapping("/peca/cadastrar")
    public ModelAndView create(@Valid @ModelAttribute("peca") PecaRequest request, BindingResult bd) {
        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("/peca/novo");
            mv.addObject("peca", request);
            return mv;
        }

        Peca entidade = PecaRequest.toEntity(request);
        pecaService.create(entidade);

        return new ModelAndView("redirect:/peca/listar");
    }

    @GetMapping("/peca/detalhes/{id}")
    public ModelAndView fetchById(@PathVariable UUID id) {
        return pecaService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("/peca/detalhes");
                    mv.addObject("peca", entidade);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/peca/listar"));
    }

    @GetMapping("/peca/editar/{id}")
    public ModelAndView retornarPaginaEdicao(@PathVariable UUID id) {
        return pecaService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("/peca/edicao");
                    mv.addObject("peca", PecaResponse.toDto(entidade));
                    mv.addObject("id", id);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/peca/listar"));
    }


    @GetMapping("/peca/remover/{id}")
    public ModelAndView deleteById(@PathVariable UUID id) {
        if (!pecaService.existsById(id)) {
            return new ModelAndView("redirect:/peca/listar");
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

    @PostMapping("/peca/atualizar/{id}")
    public ModelAndView atualizarPeca(@PathVariable UUID id, @Valid @ModelAttribute("peca") PecaRequest request, BindingResult bd) {
        var atual = pecaService.fetchById(id);

        if (atual.isEmpty()) {
            return new ModelAndView("redirect:/peca/listar");
        }

        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("/peca/edicao");
            mv.addObject("peca", request);
            mv.addObject("id", id);
            return mv;
        }

        pecaService.update(id, PecaRequest.toEntity(request));

        return new ModelAndView("redirect:/peca/listar");
    }
}
