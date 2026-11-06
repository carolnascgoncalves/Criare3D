
package br.com.criare3d.controller;

import br.com.criare3d.dto.FilamentoRequest;
import br.com.criare3d.dto.FilamentoResponse;
import br.com.criare3d.dto.FilamentoDadosRequest;
import br.com.criare3d.models.Filamento;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.FilamentoService;
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
public class FilamentoController {

    private static final Map<String, Sort> ORDENACOES = Map.of(
            "nomeAsc", Sort.by(Sort.Direction.ASC, "nome"),
            "nomeDesc", Sort.by(Sort.Direction.DESC, "nome"),
            "corAsc", Sort.by(Sort.Direction.ASC, "cor"),
            "corDesc", Sort.by(Sort.Direction.DESC, "cor")
    );

    @Autowired
    private FilamentoService filamentoService;

    @Autowired
    private CalculoHistoricoService calculoHistoricoService;

    @GetMapping("/filamento/listar")
    public ModelAndView fetchAll(
            @RequestParam(name = "busca", required = false) String busca,
            @RequestParam(name = "ordem", defaultValue = "nomeAsc") String ordem,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {

        ordem = OrdenacaoPaginada.validarOrdem(ordem, "nomeAsc", ORDENACOES);
        pageable = OrdenacaoPaginada.aplicar(pageable, ordem, ORDENACOES);

        ModelAndView mv = new ModelAndView("filamento/listar");

        Page<Filamento> pagina = filamentoService.fetchAll(pageable, busca);

        mv.addObject("filamentos", pagina.getContent());
        mv.addObject("pagina", pagina);
        mv.addObject("busca", busca == null ? "" : busca);
        mv.addObject("ordem", ordem);

        return mv;
    }

    @GetMapping("/filamento/novo")
    public ModelAndView retornarPaginaCadastro(@RequestParam(defaultValue = "") String origem) {
        ModelAndView mv = new ModelAndView("filamento/novo");
        mv.addObject("filamento", new FilamentoRequest());
        mv.addObject("origem", origem);

        return mv;
    }

    @PostMapping("/filamento/cadastrar")
    public ModelAndView create(@Valid @ModelAttribute("filamento") FilamentoRequest request, BindingResult bd, @RequestParam(defaultValue = "") String origem) {

        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("filamento/novo");
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            mv.addObject("filamento", request);
            mv.addObject("origem", origem);
            return mv;
        }

        Filamento entidade = filamentoService.create(FilamentoRequest.toEntity(request));

        if ("calculo".equals(origem)) {
            return new ModelAndView("redirect:/historico/novo?filamentoCriado=" + entidade.getId());
        }

        return new ModelAndView("redirect:/filamento/listar?sucesso=filamentoCriado");
    }

    @GetMapping("/filamento/detalhes/{id}")
    public ModelAndView fetchById(@PathVariable UUID id) {
        return filamentoService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("filamento/detalhes");
                    mv.addObject("filamento", entidade);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/filamento/listar"));
    }

    @GetMapping("/filamento/{id}/historico")
    public ModelAndView historicoDoFilamento(@PathVariable UUID id) {
        return filamentoService.fetchById(id)
                .map(filamento -> {
                    ModelAndView mv = new ModelAndView("filamento/historico");
                    mv.addObject("filamento", filamento);
                    mv.addObject("usos", calculoHistoricoService.listarUsosPorFilamento(id));
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/filamento/listar?erro=filamentoNaoEncontrado"));
    }

    @GetMapping("/filamento/editar/{id}")
    public ModelAndView retornarPaginaEdicao(@PathVariable UUID id) {
        return filamentoService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("filamento/edicao");
                    mv.addObject("filamento", FilamentoResponse.toDto(entidade));
                    mv.addObject("id", id);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/filamento/listar"));
    }

    @PostMapping("/filamento/atualizar/{id}")
    public ModelAndView atualizarFilamento(
            @PathVariable UUID id,
            @Valid @ModelAttribute("filamento") FilamentoDadosRequest request,
            BindingResult bd) {

        var atual = filamentoService.fetchById(id);

        if (atual.isEmpty()) {
            return new ModelAndView("redirect:/filamento/listar");
        }

        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("filamento/edicao");
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            mv.addObject("filamento", request);
            mv.addObject("id", id);
            return mv;
        }

        filamentoService.update(id, FilamentoDadosRequest.toEntity(request));
        return new ModelAndView("redirect:/filamento/listar?sucesso=filamentoAtualizado");
    }

    @PostMapping("/filamento/remover/{id}")
    public ModelAndView deleteById(@PathVariable UUID id) {
        if (!filamentoService.existsById(id)) {
            return new ModelAndView("redirect:/filamento/listar?erro=filamentoNaoEncontrado");
        }

        long quantidade = calculoHistoricoService.contarPorFilamento(id);

        if (quantidade > 0) {
            return new ModelAndView(
                    "redirect:/filamento/listar?erro=filamentoEmUso&quantidade=" + quantidade);
        }

        filamentoService.delete(id);
        return new ModelAndView("redirect:/filamento/listar?sucesso=filamentoRemovido");
    }

    @PostMapping("/filamento/remover-lote")
    public ModelAndView deleteSelected(
            @RequestParam(name = "ids", required = false) List<UUID> ids,
            @RequestParam(name = "busca", defaultValue = "") String busca,
            @RequestParam(name = "ordem", defaultValue = "nomeAsc") String ordem,
            @RequestParam(name = "page", defaultValue = "0") int page,
            RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) return new ModelAndView("redirect:/filamento/listar?erro=nenhumFilamentoSelecionado");
        List<UUID> bloqueados = new java.util.ArrayList<>();
        for (UUID id : ids.stream().distinct().toList()) {
            if (!filamentoService.existsById(id)) continue;
            if (calculoHistoricoService.contarPorFilamento(id) > 0) {
                bloqueados.add(id);
                continue;
            }
            filamentoService.delete(id);
        }
        if (!bloqueados.isEmpty()) redirect.addFlashAttribute("idsNaoExcluidos", bloqueados);
        redirect.addAttribute("busca", busca);
        redirect.addAttribute("ordem", ordem);
        redirect.addAttribute("page", page);
        if (!bloqueados.isEmpty()) redirect.addAttribute("erro", "filamentosEmUso");
        else redirect.addAttribute("sucesso", "filamentosRemovidos");
        return new ModelAndView("redirect:/filamento/listar");
    }
}
