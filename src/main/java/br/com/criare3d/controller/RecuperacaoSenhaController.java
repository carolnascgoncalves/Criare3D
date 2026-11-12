package br.com.criare3d.controller;

import br.com.criare3d.dto.RecuperarSenhaRequest;
import br.com.criare3d.service.RecuperacaoSenhaService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/recuperar-senha")
public class RecuperacaoSenhaController {
    @Autowired private RecuperacaoSenhaService service;

    @ModelAttribute("recuperacaoDisponivel") public boolean disponivel() { return service.habilitado(); }

    @GetMapping public String formulario(Model model) {
        model.addAttribute("recuperacao", new RecuperarSenhaRequest());
        return "usuario/recuperar-senha";
    }

    @PostMapping public String solicitar(@Valid @ModelAttribute("recuperacao") RecuperarSenhaRequest dados,
                                         BindingResult erros, HttpServletRequest request, Model model) {
        if (!erros.hasErrors()) service.solicitar(dados.getEmail(), request.getRemoteAddr());
        model.addAttribute("enviado", true);
        return "usuario/recuperar-senha";
    }

    @GetMapping("/redefinir") public String redefinir(@RequestParam(defaultValue = "") String token,
                                                       Model model, HttpServletResponse response) {
        var dados = new br.com.criare3d.dto.SenhaRecuperadaRequest();
        dados.setToken(token.length() <= 43 ? token : "");
        model.addAttribute("recuperacao", dados);
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");
        return "usuario/redefinir-senha";
    }

    @PostMapping("/redefinir") public String salvar(@Valid @ModelAttribute("recuperacao") br.com.criare3d.dto.SenhaRecuperadaRequest dados,
                BindingResult erros, Model model, HttpServletResponse response) {
        if (erros.hasErrors() || !java.util.Objects.equals(dados.getSenha(), dados.getConfirmarSenha())) {
            model.addAttribute("erro", "Informe e confirme uma senha de 12 a 72 caracteres, com até 72 bytes.");
        } else {
            try {
                service.redefinir(dados.getToken(), dados.getSenha());
                return "redirect:/login?senhaAlterada";
            } catch (IllegalArgumentException erro) { model.addAttribute("erro", erro.getMessage()); }
        }
        dados.setSenha(null); dados.setConfirmarSenha(null);
        response.setStatus(422);
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");
        return "usuario/redefinir-senha";
    }
}
