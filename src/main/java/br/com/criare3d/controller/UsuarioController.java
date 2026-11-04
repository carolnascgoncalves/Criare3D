
package br.com.criare3d.controller;

import br.com.criare3d.dto.UsuarioRequest;
import br.com.criare3d.service.interfaces.UsuarioService;
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
        return mv;
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("usuario", new UsuarioRequest());
        return "usuario/novo";
    }

    @PostMapping("/cadastrar")
    public String cadastrar(@Valid @ModelAttribute("usuario") UsuarioRequest request, BindingResult bd) {
        if (!bd.hasErrors()) {
            if (!request.getPassword().equals(request.getConfirmarPassword())) {
                bd.rejectValue("confirmarPassword", "senha.diferente", "As senhas não coincidem.");
            }
        }

        if (bd.hasErrors()) {
            return "usuario/novo";
        }

        try {
            usuarioService.cadastrarUsuario(request.getEmail().trim().toLowerCase(Locale.ROOT), request.getPassword());
        } catch (IllegalArgumentException e) {
            bd.rejectValue("email", "email.duplicado", e.getMessage());
            return "usuario/novo";
        }

        return "redirect:/usuarios/listar?sucesso";
    }
}
