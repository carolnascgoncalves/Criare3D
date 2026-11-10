package br.com.criare3d.controller;

import br.com.criare3d.dto.EstimativaDepreciacaoResponse;
import br.com.criare3d.service.MercadoLivreDepreciacaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/impressoras/depreciacao")
public class DepreciacaoImpressoraApiController {
    private final MercadoLivreDepreciacaoService service;

    public DepreciacaoImpressoraApiController(MercadoLivreDepreciacaoService service) {
        this.service = service;
    }

    @GetMapping("/estimativa")
    public EstimativaDepreciacaoResponse estimar(
            @RequestParam String modelo,
            @RequestParam(required = false) UUID impressoraId) {
        return service.estimar(modelo, impressoraId);
    }
}
