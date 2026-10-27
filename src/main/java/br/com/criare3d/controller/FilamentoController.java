
package br.com.criare3d.controller;

import br.com.criare3d.dto.FilamentoRequest;
import br.com.criare3d.dto.FilamentoResponse;
import br.com.criare3d.dto.FilamentoDadosRequest;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.FilamentoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
public class FilamentoController {

    @Autowired
    private FilamentoService filamentoService;

    @Autowired
    private CalculoHistoricoService calculoHistoricoService;

    @GetMapping("/filamento/listar")
    public ModelAndView fetchAll(@PageableDefault(page = 0, size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        ModelAndView mv = new ModelAndView("/filamento/listar");
        mv.addObject("filamentos", filamentoService.fetchAll(pageable).getContent());
        return mv;
    }

    @GetMapping("/filamento/novo")
    public ModelAndView retornarPaginaCadastro() {
        ModelAndView mv = new ModelAndView("/filamento/novo");
        mv.addObject("filamento", new FilamentoRequest());
        return mv;
    }

    @PostMapping("/filamento/cadastrar")
    public ModelAndView create(@Valid @ModelAttribute("filamento") FilamentoRequest request, BindingResult bd) {
        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("/filamento/novo");
            mv.addObject("filamento", request);
            return mv;
        }

        filamentoService.create(FilamentoRequest.toEntity(request));
        return new ModelAndView("redirect:/filamento/listar");
    }

    @GetMapping("/filamento/detalhes/{id}")
    public ModelAndView fetchById(@PathVariable UUID id) {
        return filamentoService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("/filamento/detalhes");
                    mv.addObject("filamento", entidade);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/filamento/listar"));
    }

    @GetMapping("/filamento/editar/{id}")
    public ModelAndView retornarPaginaEdicao(@PathVariable UUID id) {
        return filamentoService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("/filamento/edicao");
                    mv.addObject("filamento", FilamentoResponse.toDto(entidade));
                    mv.addObject("id", id);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/filamento/listar"));
    }

    @PostMapping("/filamento/atualizar/{id}")
    public ModelAndView atualizarFilamento(@PathVariable UUID id, @Valid @ModelAttribute("filamento") FilamentoDadosRequest request, BindingResult bd) {
        var atual = filamentoService.fetchById(id);

        if (atual.isEmpty()) {
            return new ModelAndView("redirect:/filamento/listar");
        }

        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("/filamento/edicao");
            mv.addObject("filamento", request);
            mv.addObject("id", id);
            return mv;
        }

        filamentoService.update(id, FilamentoDadosRequest.toEntity(request));
        return new ModelAndView("redirect:/filamento/listar");
    }

    @GetMapping("/filamento/remover/{id}")
    public ModelAndView deleteById(@PathVariable UUID id) {
        if (!filamentoService.existsById(id)) {
            return new ModelAndView("redirect:/filamento/listar");
        }

        long quantidade = calculoHistoricoService.contarPorFilamento(id);

        if (quantidade > 0) {
            return new ModelAndView(
                    "redirect:/filamento/listar?erro=filamentoEmUso&quantidade=" + quantidade
            );
        }

        filamentoService.delete(id);
        return new ModelAndView("redirect:/filamento/listar?sucesso=filamentoRemovido");
    }
}
