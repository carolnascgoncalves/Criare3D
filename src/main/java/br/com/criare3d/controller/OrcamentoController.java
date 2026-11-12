package br.com.criare3d.controller;

import br.com.criare3d.service.HistoricoCalculoService;

import br.com.criare3d.dto.CalculoResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
public class OrcamentoController {
    private final HistoricoCalculoService historicoCalculoService;

    public OrcamentoController(HistoricoCalculoService historicoCalculoService) {
        this.historicoCalculoService = historicoCalculoService;
    }

    @GetMapping("/historico/{id}/orcamento")
    public ModelAndView abrirOrcamento(@PathVariable UUID id) {
        return new ModelAndView("redirect:/orcamentos/novo?calculo="+id);

    }
}
