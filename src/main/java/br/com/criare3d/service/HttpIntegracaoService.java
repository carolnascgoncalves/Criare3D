package br.com.criare3d.service;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.http.*;
import java.time.Duration;

/** Só abre recursos de rede quando uma integração habilitada faz sua primeira chamada. */
@Service
public class HttpIntegracaoService {
    private HttpClient client;

    private synchronized HttpClient client() {
        if (client == null) client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return client;
    }

    public int enviar(HttpRequest request) throws IOException, InterruptedException {
        return client().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @PreDestroy public synchronized void encerrar() {
        if (client != null) client.close();
    }
}
