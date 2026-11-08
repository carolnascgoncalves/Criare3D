
package br.com.criare3d.controller;

import br.com.criare3d.dto.UsuarioRequest;
import br.com.criare3d.dto.RedefinirSenhaRequest;
import br.com.criare3d.service.interfaces.UsuarioService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Locale;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/listar")
    public ModelAndView listar() {
        ModelAndView mv = new ModelAndView("usuario/listar");
        mv.addObject("usuarios", usuarioService.listarUsuarios());
        mv.addObject("redefinirSenha", new RedefinirSenhaRequest());
        return mv;
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("usuario", new UsuarioRequest());
        return "usuario/novo";
    }

    @PostMapping("/cadastrar")
    public String cadastrar(
            @Valid @ModelAttribute("usuario") UsuarioRequest request,
            BindingResult bd,
            HttpServletResponse response) {

        if (!bd.hasErrors()) {
            if (!request.getPassword().equals(request.getConfirmarPassword())) {
                bd.rejectValue(
                        "confirmarPassword",
                        "senha.diferente",
                        "As senhas não coincidem."
                );
            }
        }

        if (bd.hasErrors()) {
            response.setStatus(422);
            return "usuario/novo";
        }

        try {
            usuarioService.cadastrarUsuario(
                    request.getEmail().trim().toLowerCase(Locale.ROOT),
                    request.getPassword()
            );
        } catch (IllegalArgumentException e) {
            bd.rejectValue("email", "email.duplicado", e.getMessage());
            response.setStatus(422);
            return "usuario/novo";
        }

        return "redirect:/usuarios/listar?sucesso";
    }

    @PostMapping("/{id}/redefinir-senha")
    public String redefinirSenha(
            @PathVariable java.util.UUID id,
            @Valid @ModelAttribute("redefinirSenha") RedefinirSenhaRequest request,
            BindingResult resultado,
            HttpServletResponse response,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
            Model model) {

        if (!resultado.hasErrors()
                && !request.getSenhaTemporaria().equals(request.getConfirmarSenhaTemporaria())) {
            resultado.rejectValue("confirmarSenhaTemporaria", "senha.diferente", "As senhas não coincidem.");
        }
        if (resultado.hasErrors()) {
            response.setStatus(422);
            model.addAttribute("usuarios", usuarioService.listarUsuarios());
            model.addAttribute("usuarioIdComErro", id);
            return "usuario/listar";
        }

        try {
            usuarioService.redefinirSenhaPorAdmin(id, request.getSenhaTemporaria());
        } catch (IllegalArgumentException erro) {
            redirectAttributes.addFlashAttribute("erroRedefinicao", erro.getMessage());
            return "redirect:/usuarios/listar";
        }

        redirectAttributes.addFlashAttribute("senhaRedefinida", true);
        return "redirect:/usuarios/listar";
    }
}
