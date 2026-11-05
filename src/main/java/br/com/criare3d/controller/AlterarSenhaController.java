package br.com.criare3d.controller;

import br.com.criare3d.dto.AlterarSenhaRequest;
import br.com.criare3d.service.interfaces.UsuarioService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AlterarSenhaController {

    private final UsuarioService usuarioService;

    public AlterarSenhaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/senha")
    public String formulario(Model model) {
        model.addAttribute("alterarSenha", new AlterarSenhaRequest());
        return "usuario/alterar-senha";
    }

    @PostMapping("/senha")
    public String alterar(
            @Valid @ModelAttribute("alterarSenha") AlterarSenhaRequest request,
            BindingResult resultado,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        if (!resultado.hasErrors()
                && !request.getNovaSenha().equals(request.getConfirmarNovaSenha())) {
            resultado.rejectValue(
                    "confirmarNovaSenha",
                    "senha.diferente",
                    "As novas senhas não coincidem."
            );
        }

        if (resultado.hasErrors()) {
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return "usuario/alterar-senha";
        }

        try {
            usuarioService.alterarSenhaAtual(request.getSenhaAtual(), request.getNovaSenha());
        } catch (IllegalArgumentException e) {
            String campo = e.getMessage().contains("senha atual") ? "senhaAtual" : "novaSenha";
            resultado.rejectValue(campo, "senha.invalida", e.getMessage());
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return "usuario/alterar-senha";
        }

        redirectAttributes.addFlashAttribute("senhaAlterada", true);
        return "redirect:/senha";
    }
}
