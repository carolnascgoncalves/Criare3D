package br.com.criare3d.controller;

import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.util.HistoricoFiltros;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
public class CalculoHistoricoAcoesController {
    private final CalculoHistoricoService calculoHistoricoService;

    public CalculoHistoricoAcoesController(CalculoHistoricoService calculoHistoricoService) {
        this.calculoHistoricoService = calculoHistoricoService;
    }

    @PostMapping("/historico/remover/{id}")
    public ModelAndView remover(@PathVariable UUID id) {
        if (!calculoHistoricoService.existsById(id)) {
            return new ModelAndView("redirect:/historico/listar?erro=naoEncontrado");
        }
        calculoHistoricoService.delete(id);
        return new ModelAndView("redirect:/historico/listar?sucesso=historicoRemovido");
    }

    @PostMapping("/historico/remover-lote")
    public ModelAndView removerSelecionados(@RequestParam(name = "ids", required = false) List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ModelAndView("redirect:/historico/listar?erro=nenhumSelecionado");
        }
        calculoHistoricoService.deleteAll(ids);
        return new ModelAndView("redirect:/historico/listar?sucesso=historicosRemovidos");
    }

    @PostMapping("/historico/{id}/produzido")
    public ModelAndView atualizarProduzido(
            @PathVariable UUID id,
            @RequestParam boolean valor,
            @RequestParam(name = "busca", defaultValue = "") String busca,
            @RequestParam(name = "data", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(name = "plataforma", defaultValue = "") String plataforma,
            @RequestParam(name = "produzido", defaultValue = "TODOS") String produzido,
            @RequestParam(name = "vendido", defaultValue = "TODOS") String vendido,
            @RequestParam(name = "ordem", defaultValue = "dataDesc") String ordem,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        String resultado = calculoHistoricoService.atualizarProduzido(id, valor);
        if (!"ok".equals(resultado)) {
            return HistoricoFiltros.redirecionarComFiltros(
                    "erro", resultado, busca, data, plataforma, produzido, vendido, ordem, page);
        }
        return HistoricoFiltros.redirecionarComFiltros(
                "sucesso", "producaoAtualizada", busca, data, plataforma, produzido, vendido, ordem, page);
    }

    @PostMapping("/historico/{id}/vendido")
    public ModelAndView atualizarVendido(
            @PathVariable UUID id,
            @RequestParam boolean valor,
            @RequestParam(name = "busca", defaultValue = "") String busca,
            @RequestParam(name = "data", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(name = "plataforma", defaultValue = "") String plataforma,
            @RequestParam(name = "produzido", defaultValue = "TODOS") String produzido,
            @RequestParam(name = "vendido", defaultValue = "TODOS") String vendido,
            @RequestParam(name = "ordem", defaultValue = "dataDesc") String ordem,
            @RequestParam(name = "page", defaultValue = "0") int page) {
        String resultado = calculoHistoricoService.atualizarVendido(id, valor);
        if (!"ok".equals(resultado)) {
            return HistoricoFiltros.redirecionarComFiltros(
                    "erro", resultado, busca, data, plataforma, produzido, vendido, ordem, page);
        }
        return HistoricoFiltros.redirecionarComFiltros(
                "sucesso", "vendaAtualizada", busca, data, plataforma, produzido, vendido, ordem, page);
    }
}
