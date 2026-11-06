
package br.com.criare3d.controller;

import br.com.criare3d.dto.EmbalagemRequest;
import br.com.criare3d.dto.EmbalagemResponse;
import br.com.criare3d.dto.EmbalagemDadosRequest;
import br.com.criare3d.models.Embalagem;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.EmbalagemService;
import br.com.criare3d.util.OrdenacaoPaginada;
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
import java.util.Map;
import java.util.List;

@Controller
public class EmbalagemController {

    private static final Map<String, Sort> ORDENACOES = Map.of(
            "nomeAsc", Sort.by(Sort.Direction.ASC, "nome"),
            "nomeDesc", Sort.by(Sort.Direction.DESC, "nome"),
            "custoAsc", Sort.by(Sort.Direction.ASC, "custoUnitario"),
            "custoDesc", Sort.by(Sort.Direction.DESC, "custoUnitario")
    );

    @Autowired
    private EmbalagemService embalagemService;

    @Autowired
    private CalculoHistoricoService calculoHistoricoService;

    @Autowired
    private CatalogoBuscaService catalogoBuscaService;

    @GetMapping("/embalagem/listar")
    public ModelAndView fetchAll(@RequestParam(name = "busca", defaultValue = "") String busca,
                                 @RequestParam(name = "ordem", defaultValue = "nomeAsc") String ordem,
                                 @PageableDefault(page = 0, size = 10) Pageable pageable) {

        ordem = OrdenacaoPaginada.validarOrdem(ordem, "nomeAsc", ORDENACOES);
        pageable = OrdenacaoPaginada.aplicar(pageable, ordem, ORDENACOES);

        ModelAndView mv = new ModelAndView("embalagem/listar");

        Page<Embalagem> pagina = busca.isBlank()
                ? embalagemService.fetchAll(pageable)
                : catalogoBuscaService.buscarEmbalagens(busca.trim(), pageable);

        mv.addObject("embalagens", pagina.getContent());
        mv.addObject("pagina", pagina);
        mv.addObject("busca", busca);
        mv.addObject("ordem", ordem);

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

        return new ModelAndView("redirect:/embalagem/listar?sucesso=embalagemCriada");
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
        return new ModelAndView("redirect:/embalagem/listar?sucesso=embalagemAtualizada");
    }

    @PostMapping("/embalagem/remover/{id}")
    public ModelAndView deleteById(@PathVariable UUID id) {
        if (!embalagemService.existsById(id)) {
            return new ModelAndView("redirect:/embalagem/listar?erro=embalagemNaoEncontrada");
        }

        long quantidade = calculoHistoricoService.contarPorEmbalagem(id);

        if (quantidade > 0) {
            return new ModelAndView(
                    "redirect:/embalagem/listar?erro=embalagemEmUso&quantidade=" + quantidade);
        }

        embalagemService.delete(id);
        return new ModelAndView("redirect:/embalagem/listar?sucesso=embalagemRemovida");
    }

    @PostMapping("/embalagem/remover-lote")
    public ModelAndView deleteSelected(
            @RequestParam(name = "ids", required = false) List<UUID> ids,
            @RequestParam(name = "busca", defaultValue = "") String busca,
            @RequestParam(name = "ordem", defaultValue = "nomeAsc") String ordem,
            @RequestParam(name = "page", defaultValue = "0") int page,
            RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) return new ModelAndView("redirect:/embalagem/listar?erro=nenhumaEmbalagemSelecionada");
        List<UUID> bloqueadas = new java.util.ArrayList<>();
        for (UUID id : ids.stream().distinct().toList()) {
            if (!embalagemService.existsById(id)) continue;
            if (calculoHistoricoService.contarPorEmbalagem(id) > 0) {
                bloqueadas.add(id);
                continue;
            }
            embalagemService.delete(id);
        }
        if (!bloqueadas.isEmpty()) redirect.addFlashAttribute("idsNaoExcluidos", bloqueadas);
        redirect.addAttribute("busca", busca);
        redirect.addAttribute("ordem", ordem);
        redirect.addAttribute("page", page);
        if (!bloqueadas.isEmpty()) redirect.addAttribute("erro", "embalagensEmUso");
        else redirect.addAttribute("sucesso", "embalagensRemovidas");
        return new ModelAndView("redirect:/embalagem/listar");
    }
}
