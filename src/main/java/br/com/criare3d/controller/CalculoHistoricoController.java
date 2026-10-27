
        package br.com.criare3d.controller;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.CalculoResponse;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.service.CalculoHistoricoServiceImp;
import br.com.criare3d.service.interfaces.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
public class CalculoHistoricoController {

    @Autowired
    private CalculoHistoricoService calculoHistoricoService;

    @Autowired
    private CalculoHistoricoServiceImp calculoHistoricoServiceImp;

    @Autowired
    private PecaService pecaService;

    @Autowired
    private FilamentoService filamentoService;

    @Autowired
    private ImpressoraService impressoraService;

    @Autowired
    private EmbalagemService embalagemService;

    @Autowired
    private ConfiguracaoService configuracaoService;

    @GetMapping("/historico/listar")
    public ModelAndView fetchAll(
            @PageableDefault(page = 0, size = 10, sort = "id",
                    direction = Sort.Direction.DESC) Pageable pageable) {

        ModelAndView mv = new ModelAndView("historico/listar");
        mv.addObject("historicos",
                calculoHistoricoService.fetchAll(pageable).getContent());
        return mv;
    }

    @GetMapping("/historico/novo")
    public ModelAndView retornarPaginaCadastro() {
        ModelAndView mv = new ModelAndView("historico/novo");
        mv.addObject("calculo", new CalculoRequest());
        carregarDadosFormulario(mv);
        return mv;
    }

    @PostMapping("/historico/calcular")
    public ModelAndView calcular(
            @Valid @ModelAttribute("calculo") CalculoRequest request,
            BindingResult bd) {

        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("historico/novo");
            mv.addObject("calculo", request);
            carregarDadosFormulario(mv);
            return mv;
        }

        try {
            CalculoHistorico calculo = calculoHistoricoServiceImp.calcular(request);

            ModelAndView mv = new ModelAndView("historico/resultado");
            mv.addObject("calculo", request);
            mv.addObject("resultado", CalculoResponse.toDto(calculo));
            return mv;

        } catch (IllegalArgumentException e) {
            ModelAndView mv = new ModelAndView("historico/novo");
            mv.addObject("calculo", request);
            mv.addObject("erro", e.getMessage());
            carregarDadosFormulario(mv);
            return mv;
        }
    }

    @PostMapping("/historico/editar")
    public ModelAndView editar(
            @ModelAttribute("calculo") CalculoRequest request) {

        ModelAndView mv = new ModelAndView("historico/novo");
        mv.addObject("calculo", request);
        carregarDadosFormulario(mv);
        return mv;
    }

    @PostMapping("/historico/salvar")
    public ModelAndView salvar(
            @Valid @ModelAttribute("calculo") CalculoRequest request,
            BindingResult bd) {

        ModelAndView mv = new ModelAndView("historico/novo");
        mv.addObject("calculo", request);

        if (bd.hasErrors()) {
            carregarDadosFormulario(mv);
            return mv;
        }

        try {
            CalculoHistorico calculo = calculoHistoricoServiceImp.calcular(request);
            calculoHistoricoService.create(calculo);

            return new ModelAndView("redirect:/historico/listar");
        } catch (IllegalArgumentException e) {
            mv.addObject("erro", e.getMessage());
            carregarDadosFormulario(mv);
            return mv;
        }
    }

    @GetMapping("/historico/detalhes/{id}")
    public ModelAndView fetchById(@PathVariable UUID id) {
        return calculoHistoricoService.fetchById(id)
                .map(entidade -> {
                    ModelAndView mv = new ModelAndView("historico/detalhes");
                    mv.addObject("historico", CalculoResponse.toDto(entidade));
                    return mv;
                })
                .orElseGet(() -> new ModelAndView("redirect:/historico/listar"));
    }

    private void carregarDadosFormulario(ModelAndView mv) {
        Pageable pageable = PageRequest.of(0, 1000);

        mv.addObject("pecas", pecaService.fetchAll(pageable).getContent());
        mv.addObject("filamentos", filamentoService.fetchAll(pageable).getContent());
        mv.addObject("impressoras", impressoraService.fetchAll(pageable).getContent());
        mv.addObject("embalagens", embalagemService.fetchAll(pageable).getContent());
        mv.addObject("configuracao", configuracaoService.buscarOuCriarConfiguracao());
    }

    @PostMapping("/historico/remover/{id}")
    public ModelAndView deleteById(@PathVariable UUID id) {
        if (calculoHistoricoService.existsById(id)) {
            calculoHistoricoService.delete(id);
        }

        return new ModelAndView(
                "redirect:/historico/listar?sucesso=historicoRemovido");
    }
}
