
package br.com.criare3d.controller;

import br.com.criare3d.dto.EmbalagemRequest;
import br.com.criare3d.dto.EmbalagemResponse;
import br.com.criare3d.dto.EmbalagemDadosRequest;
import br.com.criare3d.models.Embalagem;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.EmbalagemService;
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

import java.util.UUID;

@Controller
public class EmbalagemController {

    @Autowired
    private EmbalagemService embalagemService;

    @Autowired
    private CalculoHistoricoService calculoHistoricoService;

    @Autowired
    private CatalogoBuscaService catalogoBuscaService;

    @GetMapping("/embalagem/listar")
    public ModelAndView fetchAll(@RequestParam(name = "busca", defaultValue = "") String busca, @PageableDefault(page = 0, size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {

        ModelAndView mv = new ModelAndView("embalagem/listar");

        Page<Embalagem> pagina = busca.isBlank()
                ? embalagemService.fetchAll(pageable)
                : catalogoBuscaService.buscarEmbalagens(busca.trim(), pageable);

        mv.addObject("embalagens", pagina.getContent());
        mv.addObject("pagina", pagina);
        mv.addObject("busca", busca);

        return mv;
    }

    @GetMapping("/embalagem/novo")
    public ModelAndView retornarPaginaCadastro(@RequestParam(defaultValue = "") String origem) {
        ModelAndView mv = new ModelAndView("embalagem/novo");
        mv.addObject("embalagem", new EmbalagemRequest());
        mv.addObject("origem", origem);

        return mv;
    }

    @PostMapping("/embalagem/cadastrar")
    public ModelAndView create(@Valid @ModelAttribute("embalagem") EmbalagemRequest request, BindingResult bd, @RequestParam(defaultValue = "") String origem) {
        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("embalagem/novo");
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            mv.addObject("embalagem", request);
            mv.addObject("origem", origem);
            return mv;
        }

        Embalagem entidade = embalagemService.create(EmbalagemRequest.toEntity(request));

        if ("calculo".equals(origem)) {
            return new ModelAndView("redirect:/historico/novo?embalagemCriada=" + entidade.getId());
        }

        return new ModelAndView("redirect:/embalagem/listar");
    }

    @GetMapping("/embalagem/detalhes/{id}")
    public ModelAndView fetchById(@PathVariable UUID id) {
        return embalagemService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("embalagem/detalhes");
                    mv.addObject("embalagem", entidade);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/embalagem/listar"));
    }

    @GetMapping("/embalagem/editar/{id}")
    public ModelAndView retornarPaginaEdicao(@PathVariable UUID id) {
        return embalagemService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("embalagem/edicao");
                    mv.addObject("embalagem", EmbalagemResponse.toDto(entidade));
                    mv.addObject("id", id);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/embalagem/listar"));
    }

    @PostMapping("/embalagem/atualizar/{id}")
    public ModelAndView atualizarEmbalagem(
            @PathVariable UUID id,
            @Valid @ModelAttribute("embalagem") EmbalagemDadosRequest request,
            BindingResult bd) {

        var atual = embalagemService.fetchById(id);

        if (atual.isEmpty()) {
            return new ModelAndView("redirect:/embalagem/listar");
        }

        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("embalagem/edicao");
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            mv.addObject("embalagem", request);
            mv.addObject("id", id);
            return mv;
        }

        embalagemService.update(id, EmbalagemDadosRequest.toEntity(request));
        return new ModelAndView("redirect:/embalagem/listar");
    }

    @PostMapping("/embalagem/remover/{id}")
    public ModelAndView deleteById(@PathVariable UUID id) {
        if (!embalagemService.existsById(id)) {
            return new ModelAndView("redirect:/embalagem/listar");
        }

        long quantidade = calculoHistoricoService.contarPorEmbalagem(id);

        if (quantidade > 0) {
            return new ModelAndView(
                    "redirect:/embalagem/listar?erro=embalagemEmUso&quantidade=" + quantidade);
        }

        embalagemService.delete(id);
        return new ModelAndView("redirect:/embalagem/listar?sucesso=embalagemRemovida");
    }
}
