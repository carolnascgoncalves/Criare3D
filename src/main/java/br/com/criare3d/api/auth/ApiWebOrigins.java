package br.com.criare3d.api.auth;

import br.com.criare3d.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.*;
import java.util.*;

@Component
public class ApiWebOrigins {
    private final List<String> origens;
    public ApiWebOrigins(@Value("${criare.api.web.origins:http://localhost:8081,http://127.0.0.1:8081}") String configuradas) {
        origens = Arrays.stream(configuradas.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
        for (String origem : origens) {
            var uri = java.net.URI.create(origem);
            if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty()))
                throw new IllegalStateException("API_WEB_ORIGINS deve conter origens HTTP/HTTPS sem caminho ou curinga.");
        }
    }
    public CorsConfigurationSource cors() {
        var configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(origens);
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Cost3D-Web"));
        configuracao.setExposedHeaders(List.of("Retry-After", "Location"));
        configuracao.setAllowCredentials(true);
        configuracao.setMaxAge(600L);
        var fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/v1/**", configuracao);
        return fonte;
    }
    public void exigirNavegador(HttpServletRequest request) {
        String origem = request.getHeader("Origin");
        int porta = request.getServerPort();
        String servidor = request.getScheme() + "://" + request.getServerName()
            + ((porta == 80 && "http".equals(request.getScheme())) || (porta == 443 && "https".equals(request.getScheme())) ? "" : ":" + porta);
        if (!"1".equals(request.getHeader("X-Cost3D-Web")) || origem == null
            || !(origens.contains(origem) || origem.equals(servidor)))
            throw new RegraNegocioException("origem_nao_permitida", "Origem da solicitação não permitida.", HttpStatus.FORBIDDEN);
    }
}
