package br.com.criare3d.controller;

import br.com.criare3d.dto.SimulacaoHistoricoResponse;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
public class SimulacaoController {

    private final CalculoHistoricoService calculoHistoricoService;

    public SimulacaoController(CalculoHistoricoService calculoHistoricoService) {
        this.calculoHistoricoService = calculoHistoricoService;
    }

    @GetMapping("/simulacao")
    public ModelAndView abrir() {
        ModelAndView mv = new ModelAndView("simulacao/index");
        mv.addObject("calculos", calculoHistoricoService.buscarCalculosParaSimulacao(100));
        return mv;
    }

    @GetMapping(value = "/simulacao/calculo/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SimulacaoHistoricoResponse> buscarCalculo(@PathVariable UUID id) {
        return ResponseEntity.of(calculoHistoricoService.buscarCalculoParaSimulacao(id));
    }
}
