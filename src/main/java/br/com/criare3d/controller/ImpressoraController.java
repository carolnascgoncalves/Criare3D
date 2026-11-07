package br.com.criare3d.controller;

import br.com.criare3d.dto.*;
import br.com.criare3d.models.Filamento;
import br.com.criare3d.models.Impressora;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import br.com.criare3d.service.interfaces.ManutencaoImpressoraService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;
import java.util.List;
import org.springframework.http.HttpStatus;

@Controller
public class ImpressoraController {

    @Autowired
    private ImpressoraService impressoraService;

    @Autowired
    private CalculoHistoricoService calculoHistoricoService;

    @Autowired
    private CatalogoBuscaService catalogoBuscaService;

    @Autowired
    private ManutencaoImpressoraService manutencaoImpressoraService;

    @GetMapping("/impressora/listar")
    public ModelAndView fetchAll(@RequestParam(name = "busca", defaultValue = "") String busca, @PageableDefault(page = 0, size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {

        ModelAndView mv = new ModelAndView("impressora/listar");

        Page<Impressora> pagina = busca.isBlank()
                ? impressoraService.fetchAll(pageable)
                : catalogoBuscaService.buscarImpressoras(busca.trim(), pageable);

        mv.addObject("impressoras", pagina.getContent());
        mv.addObject("pagina", pagina);
        mv.addObject("busca", busca);

        return mv;
    }

    @GetMapping("/impressora/novo")
    public ModelAndView retornarPaginaCadastro(@RequestParam(defaultValue = "") String origem) {
        ModelAndView mv = new ModelAndView("impressora/novo");
        mv.addObject("impressora", new ImpressoraRequest());
        mv.addObject("origem", origem);

        return mv;
    }

    @PostMapping("/impressora/cadastrar")
    public ModelAndView create(@Valid @ModelAttribute("impressora") ImpressoraRequest request, BindingResult bd, @RequestParam(defaultValue = "") String origem) {
        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("impressora/novo");
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            mv.addObject("impressora", request);
            mv.addObject("origem", origem);
            return mv;
        }

        Impressora entidade = impressoraService.create(ImpressoraRequest.toEntity(request));

        if ("calculo".equals(origem)) {
            return new ModelAndView("redirect:/historico/novo?impressoraCriada=" + entidade.getId());
        }

        return new ModelAndView("redirect:/impressora/listar?sucesso=impressoraCriada");
    }

    @GetMapping("/impressora/detalhes/{id}")
    public ModelAndView fetchById(@PathVariable UUID id) {
        return manutencaoImpressoraService.resumo(id)
                .map(resumo -> {
                    ModelAndView mv = new ModelAndView("impressora/detalhes");
                    mv.addObject("impressora", resumo.impressora());
                    mv.addObject("manutencoes", resumo.registros());
                    mv.addObject("manutencao", new ManutencaoImpressoraRequest());
                    mv.addObject("totalGastoManutencao", resumo.totalGasto());
                    mv.addObject("minutosImpressos", resumo.minutosImpressos());
                    mv.addObject("manutencaoPorHoraCalculada", resumo.custoPorHora());
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/impressora/listar"));
    }

    @PostMapping("/impressora/{id}/manutencoes")
    public ModelAndView registrarManutencao(
            @PathVariable UUID id,
            @Valid @ModelAttribute("manutencao") ManutencaoImpressoraRequest request,
            BindingResult bd) {
        if (bd.hasErrors()) {
            return manutencaoImpressoraService.resumo(id)
                    .map(resumo -> {
                        ModelAndView mv = new ModelAndView("impressora/detalhes");
                        mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
                        mv.addObject("impressora", resumo.impressora());
                        mv.addObject("manutencao", request);
                        mv.addObject("manutencoes", resumo.registros());
                        mv.addObject("totalGastoManutencao", resumo.totalGasto());
                        mv.addObject("minutosImpressos", resumo.minutosImpressos());
                        mv.addObject("manutencaoPorHoraCalculada", resumo.custoPorHora());
                        return mv;
                    })
                    .orElseGet(() -> new ModelAndView("redirect:/impressora/listar"));
        }

        if (manutencaoImpressoraService.registrar(id, request).isEmpty()) {
            return new ModelAndView("redirect:/impressora/listar");
        }
        return new ModelAndView("redirect:/impressora/detalhes/" + id + "?manutencao=registrada");
    }

    @PostMapping("/impressora/{impressoraId}/manutencoes/{manutencaoId}/excluir")
    public ModelAndView excluirManutencao(
            @PathVariable UUID impressoraId,
            @PathVariable UUID manutencaoId) {
        manutencaoImpressoraService.excluir(impressoraId, manutencaoId);
        return new ModelAndView("redirect:/impressora/detalhes/" + impressoraId + "?manutencao=excluida");
    }

    @GetMapping("/impressora/editar/{id}")
    public ModelAndView retornarPaginaEdicao(@PathVariable UUID id) {
        return impressoraService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("impressora/edicao");
                    mv.addObject("impressora", ImpressoraResponse.toDto(entidade));
                    mv.addObject("id", id);
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/impressora/listar"));
    }

    @PostMapping("/impressora/remover/{id}")
    public ModelAndView deleteById(@PathVariable UUID id) {
        if (!impressoraService.existsById(id)) {
            return new ModelAndView("redirect:/impressora/listar?erro=impressoraNaoEncontrada");
        }

        long quantidade = calculoHistoricoService.contarPorImpressora(id);

        if (quantidade > 0) {
            return new ModelAndView(
                    "redirect:/impressora/listar?erro=impressoraEmUso&quantidade=" + quantidade);
        }
        if (manutencaoImpressoraService.contar(id) > 0) {
            return new ModelAndView("redirect:/impressora/listar?erro=impressoraComManutencao");
        }

        impressoraService.delete(id);
        return new ModelAndView("redirect:/impressora/listar?sucesso=impressoraRemovida");
    }

    @PostMapping("/impressora/remover-lote")
    public ModelAndView deleteSelected(
            @RequestParam(name = "ids", required = false) List<UUID> ids,
            @RequestParam(name = "busca", defaultValue = "") String busca,
            @RequestParam(name = "page", defaultValue = "0") int page,
            RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) return new ModelAndView("redirect:/impressora/listar?erro=nenhumaImpressoraSelecionada");
        List<UUID> bloqueadas = impressoraService.deleteUnused(ids);
        if (!bloqueadas.isEmpty()) redirect.addFlashAttribute("idsNaoExcluidos", bloqueadas);
        redirect.addAttribute("busca", busca);
        redirect.addAttribute("page", page);
        if (!bloqueadas.isEmpty()) redirect.addAttribute("erro", "impressorasEmUso");
        else redirect.addAttribute("sucesso", "impressorasRemovidas");
        return new ModelAndView("redirect:/impressora/listar");
    }

    @PostMapping("/impressora/atualizar/{id}")
    public ModelAndView atualizarImpressora(
            @PathVariable UUID id,
            @Valid @ModelAttribute("impressora") ImpressoraRequest request,
            BindingResult bd) {

        if (bd.hasErrors()) {
            if (!impressoraService.existsById(id)) return new ModelAndView("redirect:/impressora/listar");
            ModelAndView mv = new ModelAndView("impressora/edicao");
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            mv.addObject("impressora", request);
            mv.addObject("id", id);
            return mv;
        }

        if (impressoraService.update(id, ImpressoraRequest.toEntity(request)).isEmpty()) {
            return new ModelAndView("redirect:/impressora/listar");
        }

        return new ModelAndView("redirect:/impressora/listar?sucesso=impressoraAtualizada");
    }
}
