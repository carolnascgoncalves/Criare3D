package br.com.criare3d.controller;

import br.com.criare3d.security.AvisoLogin;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.*;

@Controller
public class LoginController {
    private final Clock clock;
    public LoginController(Clock clock) { this.clock = clock; }

    @GetMapping("/login")
    public String login(HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        var session = request.getSession(false);
        long segundos = 0;
        if (session != null && session.getAttribute(AvisoLogin.BLOQUEADO_ATE) instanceof Instant ate) {
            long millis = Duration.between(clock.instant(), ate).toMillis();
            segundos = Math.max(0, (millis + 999) / 1000);
            if (segundos == 0) session.removeAttribute(AvisoLogin.BLOQUEADO_ATE);
        }
        model.addAttribute("esperaLogin", segundos);
        return "login";
    }
}
