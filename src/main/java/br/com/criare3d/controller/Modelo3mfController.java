package br.com.criare3d.controller;

import br.com.criare3d.dto.Modelo3mfResponse;
import br.com.criare3d.service.Modelo3mfParser;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
public class Modelo3mfController {
    private final Modelo3mfParser parser;

    public Modelo3mfController(Modelo3mfParser parser) {
        this.parser = parser;
    }

    @PostMapping(value = "/calculo-rapido/analisar-arquivo", consumes = "multipart/form-data")
    public ResponseEntity<?> analisarConsulta(@RequestParam("arquivo3mf") MultipartFile arquivo) {
        return analisarArquivo(arquivo, true);
    }

    @PostMapping(value = "/peca/arquivo-3mf/analisar", consumes = "multipart/form-data")
    public ResponseEntity<?> analisar(@RequestParam("arquivo3mf") MultipartFile arquivo) {
        return analisarArquivo(arquivo, false);
    }

    private ResponseEntity<?> analisarArquivo(MultipartFile arquivo, boolean consulta) {
        try {
            Modelo3mfResponse resposta = consulta ? parser.analisarConsulta(arquivo) : parser.analisar(arquivo);
            return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(resposta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422))
                    .body(Map.of("erro", e.getMessage() == null ? "Arquivo 3MF inválido." : e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422))
                    .body(Map.of("erro", "Não foi possível analisar esse arquivo 3MF."));
        }
    }
}
