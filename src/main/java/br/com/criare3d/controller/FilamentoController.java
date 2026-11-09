
package br.com.criare3d.controller;

import br.com.criare3d.dto.FilamentoRequest;
import br.com.criare3d.dto.FilamentoResponse;
import br.com.criare3d.dto.FilamentoDadosRequest;
import br.com.criare3d.dto.CompraFilamentoRequest;
import br.com.criare3d.models.Filamento;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.util.OrdenacaoPaginada;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
public class FilamentoController {

    private static final Map<String, Sort> ORDENACOES = Map.of(
            "nomeAsc", Sort.by(Sort.Direction.ASC, "nome"),
            "nomeDesc", Sort.by(Sort.Direction.DESC, "nome"),
            "corAsc", Sort.by(Sort.Direction.ASC, "cor"),
            "corDesc", Sort.by(Sort.Direction.DESC, "cor")
    );

    private final FilamentoService filamentoService;
    private final CalculoHistoricoService calculoHistoricoService;

    public FilamentoController(FilamentoService filamentoService,
                               CalculoHistoricoService calculoHistoricoService) {
        this.filamentoService = filamentoService;
        this.calculoHistoricoService = calculoHistoricoService;
    }

    @GetMapping("/filamento/listar")
    public ModelAndView fetchAll(
            @RequestParam(name = "busca", required = false) String busca,
            @RequestParam(name = "ordem", defaultValue = "nomeAsc") String ordem,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {

        ordem = OrdenacaoPaginada.validarOrdem(ordem, "nomeAsc", ORDENACOES);
        pageable = OrdenacaoPaginada.aplicar(pageable, ordem, ORDENACOES);

        ModelAndView mv = new ModelAndView("filamento/listar");

        Page<Filamento> pagina = filamentoService.fetchAll(pageable, busca);

        Map<UUID, Boolean> filamentosComHistoricoProduzido = new HashMap<>();
        Map<UUID, Integer> percentualEstoque = new HashMap<>();
        List<UUID> idsPagina = pagina.getContent().stream().map(Filamento::getId).toList();
        var idsComHistoricoProduzido = calculoHistoricoService.buscarFilamentosComHistoricoProduzido(idsPagina);

        for (Filamento filamento : pagina.getContent()) {
            filamentosComHistoricoProduzido.put(
                    filamento.getId(),
                    idsComHistoricoProduzido.contains(filamento.getId())
            );

            BigDecimal pesoRolo = filamento.getPesoRoloGramas();
            BigDecimal estoque = filamento.getEstoqueGramas();
            int percentual = 0;
            if (pesoRolo != null && pesoRolo.signum() > 0 && estoque != null) {
                percentual = estoque.multiply(BigDecimal.valueOf(100))
                        .divide(pesoRolo, 0, RoundingMode.HALF_UP).intValue();
                percentual = Math.max(0, Math.min(100, percentual));
            }
            percentualEstoque.put(filamento.getId(), percentual);
        }

        mv.addObject("filamentos", pagina.getContent());
        mv.addObject("pagina", pagina);
        mv.addObject("busca", busca == null ? "" : busca);
        mv.addObject("ordem", ordem);
        mv.addObject("filamentosComHistoricoProduzido", filamentosComHistoricoProduzido);
        mv.addObject("percentualEstoque", percentualEstoque);

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
            mv.setStatus(HttpStatusCode.valueOf(422));
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
                    mv.addObject("compra", new CompraFilamentoRequest());
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

    @PostMapping("/filamento/estoque/{id}")
    public ModelAndView atualizarEstoqueRapido(
            @PathVariable UUID id,
            @RequestParam(name = "estoqueGramas", required = false) java.math.BigDecimal estoqueGramas,
            @RequestParam(name = "busca", defaultValue = "") String busca,
            @RequestParam(name = "ordem", defaultValue = "nomeAsc") String ordem,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "totalRegistros", defaultValue = "0") long totalRegistros,
            @RequestParam(name = "tamanhoPagina", defaultValue = "10") int tamanhoPagina,
            RedirectAttributes redirect) {

        if (estoqueGramas == null || estoqueGramas.signum() < 0
                || estoqueGramas.scale() > 2 || estoqueGramas.precision() - estoqueGramas.scale() > 8) {
            redirect.addAttribute("erro", filamentoService.existsById(id)
                    ? "estoqueInvalido" : "filamentoNaoEncontrado");
        } else {
            Filamento atualizacao = new Filamento();
            atualizacao.setEstoqueGramas(estoqueGramas);
            if (filamentoService.update(id, atualizacao).isPresent()) {
                redirect.addAttribute("sucesso", "estoqueAtualizado");
            } else {
                redirect.addAttribute("erro", "filamentoNaoEncontrado");
            }
        }

        redirect.addAttribute("busca", busca);
        redirect.addAttribute("ordem", ordem);
        redirect.addAttribute("page", Math.max(0, page));
        return new ModelAndView("redirect:/filamento/listar");
    }

    @GetMapping({"/filamento/compra/{id}", "/filamento/{id}/compra"})
    public ModelAndView abrirRegistroCompra(@PathVariable UUID id) {
        return filamentoService.fetchById(id)
                .map(filamento -> {
                    ModelAndView mv = new ModelAndView("filamento/compra");
                    mv.addObject("filamento", filamento);
                    mv.addObject("compra", new CompraFilamentoRequest());
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/filamento/listar?erro=filamentoNaoEncontrado"));
    }

    @PostMapping({"/filamento/compra/{id}", "/filamento/{id}/compra"})
    public ModelAndView registrarCompra(
            @PathVariable UUID id,
            @Valid @ModelAttribute("compra") CompraFilamentoRequest compra,
            BindingResult erros) {
        var filamento = filamentoService.fetchById(id);
        if (filamento.isEmpty()) {
            return new ModelAndView("redirect:/filamento/listar?erro=filamentoNaoEncontrado");
        }

        if (erros.hasErrors()) {
            ModelAndView mv = new ModelAndView("filamento/compra");
            mv.setStatus(HttpStatusCode.valueOf(422));
            mv.addObject("filamento", filamento.get());
            mv.addObject("compra", compra);
            mv.addObject("erroFormulario", true);
            return mv;
        }

        try {
            if (!filamentoService.registrarCompra(id, compra)) {
                return new ModelAndView("redirect:/filamento/listar?erro=filamentoNaoEncontrado");
            }
        } catch (org.springframework.dao.OptimisticLockingFailureException conflito) {
            return new ModelAndView("redirect:/filamento/detalhes/" + id + "?erro=estoqueConflito");
        }
        return new ModelAndView("redirect:/filamento/detalhes/" + id + "?sucesso=compraRegistrada");
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

        if (bd.hasErrors()) {
            if (!filamentoService.existsById(id)) return new ModelAndView("redirect:/filamento/listar");
            ModelAndView mv = new ModelAndView("filamento/edicao");
            mv.setStatus(HttpStatusCode.valueOf(422));
            mv.addObject("filamento", request);
            mv.addObject("id", id);
            return mv;
        }

        if (filamentoService.update(id, FilamentoDadosRequest.toEntity(request)).isEmpty()) {
            return new ModelAndView("redirect:/filamento/listar");
        }
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
            @RequestParam(name = "totalRegistros", defaultValue = "0") long totalRegistros,
            @RequestParam(name = "tamanhoPagina", defaultValue = "10") int tamanhoPagina,
            RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) return new ModelAndView("redirect:/filamento/listar?erro=nenhumFilamentoSelecionado");
        List<UUID> bloqueados = filamentoService.deleteUnused(ids);
        if (!bloqueados.isEmpty()) redirect.addFlashAttribute("idsNaoExcluidos", bloqueados);
        page = OrdenacaoPaginada.ajustarPaginaAposExclusao(
                page, totalRegistros, ids.size() - bloqueados.size(), tamanhoPagina);
        redirect.addAttribute("busca", busca);
        redirect.addAttribute("ordem", ordem);
        redirect.addAttribute("page", page);
        if (!bloqueados.isEmpty()) redirect.addAttribute("erro", "filamentosEmUso");
        else redirect.addAttribute("sucesso", "filamentosRemovidos");
        return new ModelAndView("redirect:/filamento/listar");
    }
}
