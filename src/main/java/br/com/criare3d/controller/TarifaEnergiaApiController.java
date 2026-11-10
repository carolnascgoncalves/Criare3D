package br.com.criare3d.controller;

import br.com.criare3d.dto.TarifaEnergiaReferenciaResponse;
import br.com.criare3d.service.TarifaEnergiaReferenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tarifas-energia")
public class TarifaEnergiaApiController {
    private final TarifaEnergiaReferenciaService service;

    public TarifaEnergiaApiController(TarifaEnergiaReferenciaService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<?> listar() {
        try {
            List<TarifaEnergiaReferenciaResponse> tarifas = service.listar();
            return ResponseEntity.ok(Map.of("tarifas", tarifas, "fonte", "ANEEL"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/localizacao")
    public ResponseEntity<?> localizar(@RequestBody LocalizacaoRequest request) {
        try {
            return ResponseEntity.ok(service.localizarCidade(request.latitude(), request.longitude()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("erro", "Não foi possível identificar a cidade agora."));
        }
    }

    public record LocalizacaoRequest(double latitude, double longitude) { }
}
