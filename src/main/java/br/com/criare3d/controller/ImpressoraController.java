package br.com.criare3d.controller;

import br.com.criare3d.dto.*;
import br.com.criare3d.models.Impressora;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
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
public class ImpressoraController {
    @Autowired
    private ImpressoraService impressoraService;

    @Autowired
    private CalculoHistoricoService calculoHistoricoService;

    @GetMapping("/impressora/listar")
    public ModelAndView fetchAll(@PageableDefault(page = 0, size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        ModelAndView mv = new ModelAndView("/impressora/listar");
        mv.addObject("impressoras", impressoraService.fetchAll(pageable).getContent());
        return mv;
    }

    @GetMapping("/impressora/novo")
    public ModelAndView retornarPaginaCadastro() {
        ModelAndView mv = new ModelAndView("/impressora/novo");
        mv.addObject("impressora", new ImpressoraRequest());
        return mv;
    }

    @PostMapping("/impressora/cadastrar")
    public ModelAndView create(@Valid @ModelAttribute("impressora") ImpressoraRequest request, BindingResult bd) {
        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("/impressora/novo");
            mv.addObject("impressora", request);
            return mv;
        }

        Impressora entidade = ImpressoraRequest.toEntity(request);
        impressoraService.create(entidade);

        return new ModelAndView("redirect:/impressora/listar");
    }

    @GetMapping("/impressora/detalhes/{id}")
    public ModelAndView fetchById(@PathVariable UUID id) {
        return impressoraService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("/impressora/detalhes");
                    mv.addObject("impressora", entidade);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/impressora/listar"));
    }

    @GetMapping("/impressora/editar/{id}")
    public ModelAndView retornarPaginaEdicao(@PathVariable UUID id) {
        return impressoraService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("/impressora/edicao");
                    mv.addObject("impressora", ImpressoraResponse.toDto(entidade));
                    mv.addObject("id", id);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/impressora/listar"));
    }


    @GetMapping("/impressora/remover/{id}")
    public ModelAndView deleteById(@PathVariable UUID id) {
        if (!impressoraService.existsById(id)) {
            return new ModelAndView("redirect:/impressora/listar");
        }

        long quantidade = calculoHistoricoService.contarPorImpressora(id);

        if (quantidade > 0) {
            return new ModelAndView(
                    "redirect:/impressora/listar?erro=impressoraEmUso&quantidade=" + quantidade
            );
        }

        impressoraService.delete(id);
        return new ModelAndView("redirect:/impressora/listar?sucesso=impressoraRemovida");
    }

    @PostMapping("/impressora/atualizar/{id}")
    public ModelAndView atualizarImpressora(@PathVariable UUID id, @Valid @ModelAttribute("impressora") ImpressoraRequest request, BindingResult bd) {

        var atual = impressoraService.fetchById(id);

        if (atual.isEmpty()) {
            return new ModelAndView("redirect:/impressora/listar");
        }

        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("/impressora/edicao");
            mv.addObject("impressora", request);
            mv.addObject("id", id);
            return mv;
        }

        impressoraService.update(id, ImpressoraRequest.toEntity(request));

        return new ModelAndView("redirect:/impressora/listar");
    }
}
