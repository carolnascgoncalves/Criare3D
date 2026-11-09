package br.com.criare3d.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Rotas antigas encaminhadas para o painel atual de clientes. */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @GetMapping({"", "/", "/listar"})
    public String listar() {
        return "redirect:/clientes/listar";
    }

    @GetMapping("/novo")
    public String novo() {
        return "redirect:/clientes/novo";
    }

    @PostMapping("/cadastrar")
    public String cadastrar() {
        return "redirect:/clientes/novo";
    }
}
