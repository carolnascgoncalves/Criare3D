package br.com.criare3d.controller;

import br.com.criare3d.security.LimiteLogin;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/** Rota de administração, protegida pela mesma regra /clientes/** do SecurityConfig. */
@RestController
public class DiagnosticoOrigemController {
    private final LimiteLogin limite;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DiagnosticoOrigemController.class);
    public DiagnosticoOrigemController(LimiteLogin limite) { this.limite = limite; }

    @GetMapping("/clientes/diagnostico-origem")
    public org.springframework.http.ResponseEntity<Map<String, String>> origem(HttpServletRequest request) {
        String origem = limite.identificadorOrigem(request.getRemoteAddr());
        LOG.info("Diagnóstico de origem; origem={}", origem);
        return org.springframework.http.ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(Map.of("origem", origem));
    }
}
