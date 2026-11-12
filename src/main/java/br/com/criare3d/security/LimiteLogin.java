package br.com.criare3d.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

/** Limites independentes por conta e origem; nunca guarda e-mail ou senha no cache. */
@Service
public class LimiteLogin {
    private final Clock clock;
    private final Cache<String, Estado> estados = Caffeine.newBuilder()
            .maximumSize(100_000).expireAfterWrite(Duration.ofHours(2)).build();

    public LimiteLogin() { this(Clock.systemUTC()); }
    @org.springframework.beans.factory.annotation.Autowired
    public LimiteLogin(Clock clock) { this.clock = clock; }

    public synchronized long esperaSegundos(String email, String ip) {
        return Math.max(espera(chave("email", normalizar(email))), espera(chave("ip", ip)));
    }

    public synchronized Instant bloqueadoAte(String email, String ip) {
        return clock.instant().plusSeconds(esperaSegundos(email, ip));
    }

    public synchronized void falhou(String email, String ip) {
        registrar(chave("email", normalizar(email)));
        registrar(chave("ip", ip));
    }

    public synchronized void autenticou(String email) {
        estados.invalidate(chave("email", normalizar(email)));
    }

    public String identificadorConta(String email) { return chave("email", normalizar(email)); }
    public String identificadorOrigem(String ip) { return chave("ip", ip); }

    private long espera(String chave) {
        Estado estado = estados.getIfPresent(chave);
        if (estado == null) return 0;
        long millis = Duration.between(clock.instant(), estado.bloqueadoAte()).toMillis();
        return Math.max(0, (millis + 999) / 1000);
    }

    private void registrar(String chave) {
        Instant agora = clock.instant();
        Estado anterior = estados.getIfPresent(chave);
        int falhas = anterior == null || anterior.ultimaFalha().plus(Duration.ofHours(1)).isBefore(agora)
                ? 1 : Math.min(30, anterior.falhas() + 1);
        long segundos = falhas < 5 ? 0 : Math.min(3600, 30L << Math.min(7, falhas - 5));
        estados.put(chave, new Estado(falhas, agora, agora.plusSeconds(segundos)));
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    static String chave(String tipo, String valor) {
        try {
            return tipo + ":" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(valor).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private record Estado(int falhas, Instant ultimaFalha, Instant bloqueadoAte) {}
}
