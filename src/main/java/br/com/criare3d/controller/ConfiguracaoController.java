
package br.com.criare3d.controller;

import br.com.criare3d.dto.ConfiguracaoRequest;
import br.com.criare3d.dto.ConfiguracaoResponse;
import br.com.criare3d.dto.ConfiguracaoDadosRequest;
import br.com.criare3d.service.interfaces.ConfiguracaoService;

import br.com.criare3d.dto.ConfiguracaoRequest;
import br.com.criare3d.models.Configuracao;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ConfiguracaoController {

    @Autowired
    private ConfiguracaoService configuracaoService;

    @GetMapping("/configuracao")
    public ModelAndView abrirConfiguracao() {
        Configuracao configuracao = configuracaoService.buscarOuCriarConfiguracao();

        ModelAndView mv = new ModelAndView("configuracao/index");
        mv.addObject("configuracao", ConfiguracaoRequest.toDto(configuracao));

        return mv;
    }

    @PostMapping("/configuracao/salvar")
    public ModelAndView salvarConfiguracao(
            @Valid @ModelAttribute("configuracao") ConfiguracaoRequest request,
            BindingResult bd) {

        Configuracao atual = configuracaoService.buscarOuCriarConfiguracao();

        if (bd.hasErrors()) {
            ModelAndView mv = new ModelAndView("configuracao/index");
            mv.addObject("configuracao", request);
            return mv;
        }

        Configuracao dados = ConfiguracaoRequest.toEntity(request);
        configuracaoService.update(atual.getId(), dados);

        return new ModelAndView("redirect:/configuracao?sucesso");
    }
}
