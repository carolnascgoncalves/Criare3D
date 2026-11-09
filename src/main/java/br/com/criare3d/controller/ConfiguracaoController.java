
package br.com.criare3d.controller;

import br.com.criare3d.dto.ConfiguracaoRequest;
import br.com.criare3d.models.Configuracao;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;

    public ConfiguracaoController(ConfiguracaoService configuracaoService) {
        this.configuracaoService = configuracaoService;
    }

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
            mv.setStatus(HttpStatusCode.valueOf(422));
            mv.addObject("configuracao", request);
            mv.addObject("mensagemErro", bd.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElse("Revise os campos destacados antes de salvar."));
            return mv;
        }

        Configuracao dados = ConfiguracaoRequest.toEntity(request);
        configuracaoService.update(atual.getId(), dados);

        return new ModelAndView("redirect:/configuracao?sucesso");
    }
}
