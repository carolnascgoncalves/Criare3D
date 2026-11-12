package br.com.criare3d.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

/** Adaptador de entrega: credenciais somente no ambiente; links nunca usam o Host da requisição. */
@Service
public class EmailAcessoService {
    @Value("${CRIARE_EMAIL_API_KEY:}") private String apiKey;
    @Value("${CRIARE_EMAIL_REMETENTE:}") private String remetente;
    @Value("${CRIARE_PUBLIC_URL:}") private String publicUrl;
    @org.springframework.beans.factory.annotation.Autowired private HttpIntegracaoService http;

    public boolean habilitado() {
        if (apiKey.isBlank() || remetente.isBlank() || publicUrl.isBlank()) return false;
        try {
            URI uri = URI.create(publicUrl);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getQuery() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException erro) { return false; }
    }

    public void enviarRecuperacao(String destinatario, String token) throws Exception {
        String link = publicUrl.replaceAll("/+$", "") + "/recuperar-senha/redefinir?token=" + token;
        String json = JsonMapper.builder().build().writeValueAsString(Map.of(
                "from", remetente, "to", java.util.List.of(destinatario), "subject", "Redefina sua senha — Criare3D",
                "text", "Recebemos uma solicitação para redefinir sua senha. O link vale por 15 minutos e só pode ser usado uma vez:\n"
                        + link + "\nSe não foi você, ignore esta mensagem. Sua senha continua a mesma."));
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
        int status = http.enviar(request);
        if (status < 200 || status >= 300) throw new IllegalStateException("Falha na entrega da recuperação: HTTP " + status);
    }
}
