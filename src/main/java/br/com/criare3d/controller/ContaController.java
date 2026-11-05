package br.com.criare3d.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class ContaController {

    @GetMapping("/conta")
    public String conta(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        return "usuario/conta";
    }
}
