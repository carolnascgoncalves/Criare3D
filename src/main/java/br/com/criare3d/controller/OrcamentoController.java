package br.com.criare3d.controller;

import br.com.criare3d.dto.CalculoResponse;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
public class OrcamentoController {
    private final CalculoHistoricoService calculoHistoricoService;

    public OrcamentoController(CalculoHistoricoService calculoHistoricoService) {
        this.calculoHistoricoService = calculoHistoricoService;
    }

    @GetMapping("/historico/{id}/orcamento")
    public ModelAndView abrirOrcamento(@PathVariable UUID id) {
        return calculoHistoricoService.fetchById(id)
                .map(entidade -> {
                    CalculoResponse calculo = CalculoResponse.toDto(entidade);
                    int unidades = calculo.getUnidades() == null ? 1 : calculo.getUnidades();
                    BigDecimal precoUnitario = calculo.getPrecoVendaPlataforma() != null
                            ? calculo.getPrecoVendaPlataforma() : calculo.getPrecoSugerido();
                    BigDecimal lucroUnitario = calculo.getLucroLiquido() != null
                            ? calculo.getLucroLiquido() : calculo.getLucroEstimado();
                    ModelAndView mv = new ModelAndView("historico/orcamento");
                    mv.addObject("calculo", calculo);
                    mv.addObject("unidades", unidades);
                    mv.addObject("fornadas", calculo.getFornadas() == null ? unidades : calculo.getFornadas());
                    mv.addObject("custoTotalLote", calculo.getCustoTotal().multiply(BigDecimal.valueOf(unidades)));
                    mv.addObject("precoUnitario", precoUnitario);
                    mv.addObject("precoTotalLote", precoUnitario.multiply(BigDecimal.valueOf(unidades)));
                    mv.addObject("lucroTotalLote", lucroUnitario.multiply(BigDecimal.valueOf(unidades)));
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/historico/listar?erro=naoEncontrado"));
    }
}
