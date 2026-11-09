package br.com.criare3d.controller;

import br.com.criare3d.dto.AlterarSenhaRequest;
import br.com.criare3d.service.interfaces.UsuarioService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import br.com.criare3d.config.PrimeiroAcessoSenhaObrigatoriaFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@Controller
public class AlterarSenhaController {

    private final UsuarioService usuarioService;

    public AlterarSenhaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/senha")
    public String formulario(Model model) {
        model.addAttribute("alterarSenha", new AlterarSenhaRequest());
        model.addAttribute("primeiroAcesso", false);
        return "usuario/alterar-senha";
    }

    @GetMapping("/senha/primeiro-acesso")
    public String formularioPrimeiroAcesso(Model model) {
        model.addAttribute("alterarSenha", new AlterarSenhaRequest());
        model.addAttribute("primeiroAcesso", true);
        return "usuario/alterar-senha";
    }

    @PostMapping({"/senha", "/senha/primeiro-acesso"})
    public String alterar(
            @Valid @ModelAttribute("alterarSenha") AlterarSenhaRequest request,
            BindingResult resultado,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes,
            HttpServletRequest requisicaoWeb,
            Model model
    ) {
        boolean primeiroAcesso = requisicaoWeb.getRequestURI().endsWith("/senha/primeiro-acesso");
        model.addAttribute("primeiroAcesso", primeiroAcesso);
        if (!resultado.hasErrors()
                && !request.getNovaSenha().equals(request.getConfirmarNovaSenha())) {
            resultado.rejectValue(
                    "confirmarNovaSenha",
                    "senha.diferente",
                    "As novas senhas não coincidem."
            );
        }

        if (resultado.hasErrors()) {
            response.setStatus(HttpStatusCode.valueOf(422).value());
            return "usuario/alterar-senha";
        }

        try {
            usuarioService.alterarSenhaAtual(request.getSenhaAtual(), request.getNovaSenha());
        } catch (IllegalArgumentException e) {
            String campo = e.getMessage().contains("senha atual") ? "senhaAtual" : "novaSenha";
            resultado.rejectValue(campo, "senha.invalida", e.getMessage());
            response.setStatus(HttpStatusCode.valueOf(422).value());
            return "usuario/alterar-senha";
        }

        if (primeiroAcesso) {
            new SecurityContextLogoutHandler().logout(
                    requisicaoWeb,
                    response,
                    SecurityContextHolder.getContext().getAuthentication()
            );
            return "redirect:/login?senhaAlterada";
        }

        redirectAttributes.addFlashAttribute("senhaAlterada", true);
        HttpSession sessao = requisicaoWeb.getSession(false);
        if (sessao != null) sessao.removeAttribute(PrimeiroAcessoSenhaObrigatoriaFilter.SESSAO_TROCA_OBRIGATORIA);
        return "redirect:/senha";
    }
}
