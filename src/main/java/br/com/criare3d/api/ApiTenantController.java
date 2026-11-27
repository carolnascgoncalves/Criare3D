package br.com.criare3d.api;

import br.com.criare3d.security.UsuarioPrincipal;
import br.com.criare3d.service.TenantTemaCache;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant")
@SecurityRequirement(name = "bearerAuth")
public class ApiTenantController {
    private final TenantTemaCache temas;

    public ApiTenantController(TenantTemaCache temas) {
        this.temas = temas;
    }

    public record Tema(
            UUID id,
            String nome,
            String corPrimaria,
            String corDestaque,
            String corFundo,
            Long versao,
            String logoUrl) {}

    @GetMapping
    public Tema tema(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        var tema = temas.buscarTema(usuario.getTenantId()).orElseThrow();
        return new Tema(
                usuario.getTenantId(),
                tema.getNome(),
                tema.getCorPrimaria(),
                tema.getCorDestaque(),
                tema.getCorFundo(),
                tema.getTemaVersao(),
                Boolean.TRUE.equals(tema.getTemLogo())
                        ? "/api/v1/tenant/logo?v=" + tema.getTemaVersao()
                        : null);
    }

    @GetMapping("/icone")
    public ApiPainelController.FotoCatalogo icone(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        var imagem=logo(usuario);
        return new ApiPainelController.FotoCatalogo(imagem.getHeaders().getContentType().toString(),
                java.util.Base64.getEncoder().encodeToString(imagem.getBody()));
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        var tema = temas.buscarTema(usuario.getTenantId()).orElseThrow();
        byte[] bytes =
                temas.buscarLogo(usuario.getTenantId(), tema.getTemaVersao())
                        .orElseThrow(NoSuchElementException::new);
        // O cadastro existente aceita PNG, JPEG e WebP. A assinatura determina o Content-Type.
        String tipo =
                bytes.length > 3 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                        ? "image/png"
                        : bytes.length > 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8
                                ? "image/jpeg"
                                : "image/webp";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipo))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(24)).cachePrivate())
                .body(bytes);
    }
}
