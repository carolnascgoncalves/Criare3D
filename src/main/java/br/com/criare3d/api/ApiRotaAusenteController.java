package br.com.criare3d.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.Hidden
@RestController
public class ApiRotaAusenteController {
    @RequestMapping({"/api/v1", "/api/v1/{*caminho}"})
    public ResponseEntity<ProblemDetail> ausente() {
        return ResponseEntity.status(404)
                .body(
                        ApiProblemas.problema(
                                HttpStatus.NOT_FOUND,
                                "rota_nao_encontrada",
                                "Rota não encontrada."));
    }
}
