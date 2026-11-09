package br.com.criare3d.controller;

import br.com.criare3d.models.Tenant;
import br.com.criare3d.security.UsuarioPrincipal;
import br.com.criare3d.repository.TenantRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;

@Controller
public class TemaController {
    private static final String PRIMARIA_PADRAO = "#492e1e";
    private static final String DESTAQUE_PADRAO = "#e9a695";
    private static final String FUNDO_PADRAO = "#f5e7da";
    private static final String CACHE_TEMA = "private, max-age=31536000, immutable";

    private final TenantRepository tenantRepository;

    public TemaController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping(value = "/tema/{tenantId}/{versao}.css", produces = "text/css")
    @ResponseBody
    public ResponseEntity<String> css(@PathVariable UUID tenantId, @PathVariable long versao) {
        UsuarioPrincipal principal = principalAtual();
        if (!temaPertenceAoUsuario(principal, tenantId, versao)) return ResponseEntity.notFound().build();

        String primaria = corSegura(principal.getCorPrimaria(), PRIMARIA_PADRAO);
        String destaque = corSegura(principal.getCorDestaque(), DESTAQUE_PADRAO);
        String fundo = corSegura(principal.getCorFundo(), FUNDO_PADRAO);
        String textoDestaque = textoContraste(destaque);
        String textoPrimaria = textoContraste(primaria);
        String css = """
                :root {
                  --cor-primaria: %s;
                  --cor-destaque: %s;
                  --cor-fundo: %s;
                  --texto-sobre-destaque: %s;
                  --texto-sobre-primaria: %s;
                  --marrom: var(--cor-primaria);
                  --destaque: var(--cor-destaque);
                  --fundo: var(--cor-fundo);
                  --marrom-claro: color-mix(in srgb, var(--cor-primaria) 72%%, var(--cor-fundo));
                  --borda: color-mix(in srgb, var(--cor-primaria) 15%%, var(--cor-fundo));
                  --superficie-suave: color-mix(in srgb, var(--cor-fundo) 22%%, white);
                  --superficie: color-mix(in srgb, var(--cor-fundo) 8%%, white);
                  --destaque-suave: color-mix(in srgb, var(--cor-destaque) 22%%, white);
                  --destaque-medio: color-mix(in srgb, var(--cor-destaque) 42%%, white);
                  --destaque-escuro: color-mix(in srgb, var(--cor-destaque) 70%%, var(--cor-primaria));
                  --marrom-medio: color-mix(in srgb, var(--cor-primaria) 72%%, var(--cor-destaque));
                  --dashboard-tiktok: var(--cor-primaria);
                  --dashboard-shopee: var(--cor-destaque);
                  --dashboard-mercado-livre: color-mix(in srgb, var(--cor-primaria) 48%%, var(--cor-destaque));
                  --dashboard-sem-plataforma: color-mix(in srgb, var(--cor-primaria) 25%%, var(--cor-fundo));
                  --dashboard-outro: color-mix(in srgb, var(--cor-primaria) 60%%, var(--cor-fundo));
                  --dashboard-custo-filamento: var(--cor-primaria);
                  --dashboard-custo-falhas: color-mix(in srgb, var(--cor-destaque) 22%%, white);
                  --dashboard-custo-energia: color-mix(in srgb, var(--cor-destaque) 35%%, var(--cor-fundo));
                  --dashboard-custo-depreciacao: color-mix(in srgb, var(--cor-destaque) 68%%, var(--cor-primaria));
                  --dashboard-custo-embalagem: var(--cor-destaque);
                  --dashboard-custo-mao-obra: color-mix(in srgb, var(--cor-primaria) 68%%, var(--cor-destaque));
                  --dashboard-custo-manutencao: color-mix(in srgb, var(--cor-primaria) 42%%, var(--cor-destaque));
                  --dashboard-chart-fill: color-mix(in srgb, var(--cor-destaque) 25%%, transparent);
                  --dashboard-chart-grid: var(--borda);
                  --dashboard-chart-text: var(--marrom-claro);
                }
                """.formatted(primaria, destaque, fundo, textoDestaque, textoPrimaria);

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/css; charset=UTF-8"))
                .header("Cache-Control", CACHE_TEMA)
                .eTag(etag(tenantId, versao, "css"))
                .body(css);
    }

    @GetMapping(value = "/tema/{tenantId}/{versao}/logo.png", produces = "image/png")
    public ResponseEntity<byte[]> logo(@PathVariable UUID tenantId, @PathVariable long versao) {
        UsuarioPrincipal principal = principalAtual();
        if (!temaPertenceAoUsuario(principal, tenantId, versao) || !principal.isTenantTemLogo()) {
            return ResponseEntity.notFound().build();
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null || tenant.getLogo() == null || tenant.getLogo().length == 0) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = tenant.getLogo();
        if (!pngValido(bytes)) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Cache-Control", CACHE_TEMA)
                .eTag(etag(tenantId, versao, "logo"))
                .body(bytes);
    }

    private boolean temaPertenceAoUsuario(UsuarioPrincipal principal, UUID tenantId, long versao) {
        return principal != null && tenantId.equals(principal.getTenantId())
                && (versao == principal.getTemaVersao()
                    // Sessões que já existiam antes da inclusão do tema continuam usando o padrão.
                    || (principal.getTemaVersao() == 0 && versao == 1));
    }

    private UsuarioPrincipal principalAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.isAuthenticated()
                && autenticacao.getPrincipal() instanceof UsuarioPrincipal principal ? principal : null;
    }

    private String corSegura(String cor, String padrao) {
        return cor != null && cor.matches("#[0-9A-Fa-f]{6}") ? cor : padrao;
    }

    private String textoContraste(String hex) {
        double r = canalLinear(Integer.parseInt(hex.substring(1, 3), 16));
        double g = canalLinear(Integer.parseInt(hex.substring(3, 5), 16));
        double b = canalLinear(Integer.parseInt(hex.substring(5, 7), 16));
        double luminancia = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        double contrasteEscuro = (luminancia + 0.05) / (0.019 + 0.05);
        double contrasteClaro = 1.05 / (luminancia + 0.05);
        return contrasteEscuro >= contrasteClaro ? "#2b211d" : "#ffffff";
    }

    private double canalLinear(int valor) {
        double canal = valor / 255.0;
        return canal <= 0.04045 ? canal / 12.92 : Math.pow((canal + 0.055) / 1.055, 2.4);
    }

    private boolean pngValido(byte[] bytes) {
        byte[] assinatura = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length < assinatura.length) return false;
        for (int i = 0; i < assinatura.length; i++) {
            if (bytes[i] != assinatura[i]) return false;
        }
        return true;
    }

    private String etag(UUID tenantId, long versao, String recurso) {
        return "\"tenant-" + tenantId + "-tema-" + versao + "-" + recurso + "\"";
    }
}
