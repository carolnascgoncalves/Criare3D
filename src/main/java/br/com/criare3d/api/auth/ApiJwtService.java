package br.com.criare3d.api.auth;

import br.com.criare3d.exception.RegraNegocioException;
import br.com.criare3d.security.UsuarioPrincipal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

import javax.crypto.spec.SecretKeySpec;

@Service
public class ApiJwtService {
    public static final Duration VALIDADE = Duration.ofMinutes(15);
    private static final String EMISSOR = "cost3d";
    private static final String PUBLICO = "cost3d-api-v1";
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final boolean configurado;

    public ApiJwtService(@Value("${criare.api.jwt.secret:}") String segredo, Clock clock) {
        this.clock = clock;
        configurado = segredo != null && !segredo.isBlank();
        byte[] chave;
        if (configurado) {
            try {
                chave = Base64.getDecoder().decode(segredo);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("API_JWT_SECRET deve ser Base64.");
            }
            if (chave.length < 32)
                throw new IllegalStateException(
                        "API_JWT_SECRET precisa de pelo menos 32 bytes aleatórios.");
        } else {
            chave = new byte[32];
            new SecureRandom().nextBytes(chave);
        }
        var segredoHmac = new SecretKeySpec(chave, "HmacSHA256");
        encoder = NimbusJwtEncoder.withSecretKey(segredoHmac).algorithm(MacAlgorithm.HS256).build();
        var nimbus =
                NimbusJwtDecoder.withSecretKey(segredoHmac)
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();
        var tempo = new JwtTimestampValidator(Duration.ZERO);
        tempo.setClock(clock);
        nimbus.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        tempo,
                        new JwtIssuerValidator(EMISSOR),
                        jwt ->
                                jwt.getAudience() != null
                                                && jwt.getAudience().contains(PUBLICO)
                                                && jwt.getExpiresAt() != null
                                                && jwt.getIssuedAt() != null
                                                && !jwt.getIssuedAt().isAfter(clock.instant())
                                        ? OAuth2TokenValidatorResult.success()
                                        : OAuth2TokenValidatorResult.failure(
                                                new OAuth2Error("invalid_token"))));
        decoder = nimbus;
    }

    public void exigirConfiguracao() {
        if (!configurado)
            throw new RegraNegocioException(
                    "api_nao_configurada",
                    "Configure API_JWT_SECRET no servidor para habilitar o login da API.",
                    HttpStatus.SERVICE_UNAVAILABLE);
    }

    public String emitir(UsuarioPrincipal usuario, UUID familia) {
        exigirConfiguracao();
        var agora = clock.instant();
        var claims =
                JwtClaimsSet.builder()
                        .issuer(EMISSOR)
                        .audience(List.of(PUBLICO))
                        .subject(usuario.getUsuarioId().toString())
                        .issuedAt(agora)
                        .expiresAt(agora.plus(VALIDADE))
                        .id(UUID.randomUUID().toString())
                        .claim("sid", familia.toString())
                        .claim("cred", credencial(usuario));
        if (usuario.getTenantId() != null) claims.claim("tenant", usuario.getTenantId().toString());
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
    }

    public Jwt ler(String token) {
        exigirConfiguracao();
        return decoder.decode(token);
    }

    public static String hash(String valor) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String credencial(UsuarioPrincipal usuario) {
        return hash(usuario.getPassword());
    }
}
