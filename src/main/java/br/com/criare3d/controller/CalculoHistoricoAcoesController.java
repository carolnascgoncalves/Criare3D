package br.com.criare3d.controller;
import br.com.criare3d.service.ResultadoOperacao;

import br.com.criare3d.service.HistoricoCalculoService;
import br.com.criare3d.service.ProducaoService;
import br.com.criare3d.service.VendaService;


import br.com.criare3d.util.HistoricoFiltros;
import br.com.criare3d.util.OrdenacaoPaginada;
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
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public String operacaoVinculada(IllegalArgumentException erro) {
        return "redirect:/historico/listar?erro=operacaoRegistrada";
    }

    private final HistoricoCalculoService historicoCalculoService;
    private final ProducaoService producaoService;
    private final VendaService vendaService;

    public CalculoHistoricoAcoesController(HistoricoCalculoService historicoCalculoService, ProducaoService producaoService, VendaService vendaService) {
        this.historicoCalculoService = historicoCalculoService;
        this.producaoService = producaoService;
        this.vendaService = vendaService;
    }

    @PostMapping("/historico/remover/{id}")
    public ModelAndView remover(@PathVariable UUID id) {
        if (!historicoCalculoService.existsById(id)) {
            return new ModelAndView("redirect:/historico/listar?erro=naoEncontrado");
        }
        historicoCalculoService.delete(id);
        return new ModelAndView("redirect:/historico/listar?sucesso=historicoRemovido");
    }

    @PostMapping("/historico/remover-lote")
    public ModelAndView removerSelecionados(
            @RequestParam(name = "ids", required = false) List<UUID> ids,
            @RequestParam(name = "busca", defaultValue = "") String busca,
            @RequestParam(name = "data", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(name = "plataforma", defaultValue = "") String plataforma,
            @RequestParam(name = "produzido", defaultValue = "TODOS") String produzido,
            @RequestParam(name = "vendido", defaultValue = "TODOS") String vendido,
            @RequestParam(name = "ordem", defaultValue = "dataDesc") String ordem,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "totalRegistros", defaultValue = "0") long totalRegistros,
            @RequestParam(name = "tamanhoPagina", defaultValue = "10") int tamanhoPagina) {
        if (ids == null || ids.isEmpty()) {
            return HistoricoFiltros.redirecionarComFiltros(
                    "erro", "nenhumSelecionado", busca, data, plataforma, produzido, vendido, ordem, page);
        }
        historicoCalculoService.deleteAll(ids);
        page = OrdenacaoPaginada.ajustarPaginaAposExclusao(
                page, totalRegistros, ids.size(), tamanhoPagina);
        return HistoricoFiltros.redirecionarComFiltros(
                "sucesso", "historicosRemovidos", busca, data, plataforma, produzido, vendido, ordem, page);
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
        ResultadoOperacao resultado = producaoService.atualizarProduzido(id, valor);
        if (!ResultadoOperacao.OK.equals(resultado)) {
            return HistoricoFiltros.redirecionarComFiltros(
                    "erro", resultado.codigo(), busca, data, plataforma, produzido, vendido, ordem, page);
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
        ResultadoOperacao resultado = vendaService.atualizarVendido(id, valor);
        if (!ResultadoOperacao.OK.equals(resultado)) {
            return HistoricoFiltros.redirecionarComFiltros(
                    "erro", resultado.codigo(), busca, data, plataforma, produzido, vendido, ordem, page);
        }
        return HistoricoFiltros.redirecionarComFiltros(
                "sucesso", "vendaAtualizada", busca, data, plataforma, produzido, vendido, ordem, page);
    }
}
