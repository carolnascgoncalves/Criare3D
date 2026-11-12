package br.com.criare3d.service;

import com.github.benmanes.caffeine.cache.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

/** Webhook HTTPS para o monitoramento. Não inclui usuário, URL, parâmetros ou mensagem da exceção. */
@Service
public class AlertaOperacionalService {
    @Value("${CRIARE_ALERTA_WEBHOOK:}") private String webhook;
    @Autowired private ThreadPoolTaskExecutor notificacoesExecutor;
    private final Cache<String, Boolean> recentes = Caffeine.newBuilder().maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5)).build();
    @org.springframework.beans.factory.annotation.Autowired private HttpIntegracaoService http;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AlertaOperacionalService.class);

    public void registrar(Exception erro, int status) {
        if (webhook.isBlank()) return;
        String tipo = erro.getClass().getName();
        if (recentes.asMap().putIfAbsent(status + tipo, true) != null) return;
        try {
            URI destino = URI.create(webhook);
            if (!"https".equals(destino.getScheme()) || destino.getHost() == null) {
                LOG.warn("Webhook de monitoramento precisa de HTTPS"); return;
            }
            notificacoesExecutor.execute(() -> {
                try {
                    String json = JsonMapper.builder().build().writeValueAsString(Map.of(
                            "aplicacao", "Criare3D", "status", status, "tipo", tipo,
                            "mensagem", "Erro de aplicação. Consulte os logs do servidor."));
                    var request = HttpRequest.newBuilder(destino).timeout(Duration.ofSeconds(10))
                            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
                    int resposta = http.enviar(request);
                    if (resposta < 200 || resposta >= 300) LOG.warn("Monitoramento recusou alerta: HTTP {}", resposta);
                } catch (InterruptedException interrompido) {
                    Thread.currentThread().interrupt();
                } catch (Exception falha) { LOG.warn("Falha ao entregar alerta operacional ({})", falha.getClass().getSimpleName()); }
            });
        } catch (RuntimeException falha) { LOG.warn("Alerta operacional indisponível ({})", falha.getClass().getSimpleName()); }
    }
}
