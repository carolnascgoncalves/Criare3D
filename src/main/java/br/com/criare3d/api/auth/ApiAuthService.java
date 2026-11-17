package br.com.criare3d.api.auth;

import br.com.criare3d.exception.RegraNegocioException;
import br.com.criare3d.security.LimiteLogin;
import br.com.criare3d.security.UsuarioPrincipal;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@Service
public class ApiAuthService {
    private static final Duration VALIDADE_REFRESH = Duration.ofDays(30);
    private final JdbcTemplate jdbc;
    private final ApiContas contas;
    private final ApiJwtService jwt;
    private final PasswordEncoder senhas;
    private final LimiteLogin limite;
    private final Clock clock;
    private final String hashInexistente;
    private final SecureRandom aleatorio = new SecureRandom();

    public ApiAuthService(
            JdbcTemplate jdbc,
            ApiContas contas,
            ApiJwtService jwt,
            PasswordEncoder senhas,
            LimiteLogin limite,
            Clock clock) {
        this.jdbc = jdbc;
        this.contas = contas;
        this.jwt = jwt;
        this.senhas = senhas;
        this.limite = limite;
        this.clock = clock;
        hashInexistente = senhas.encode(UUID.randomUUID().toString());
    }

    public record Tokens(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            OffsetDateTime refreshExpiraEm) {}

    @Transactional
    public Tokens login(String email, String senha, String ip) {
        jwt.exigirConfiguracao();
        long espera = limite.esperaSegundos(email, ip);
        if (espera > 0) throw new LoginLimitado(espera);
        var usuario = contas.porEmail(email).orElse(null);
        boolean confere =
                senhas.matches(senha, usuario == null ? hashInexistente : usuario.getPassword());
        if (usuario == null || !confere) {
            limite.falhou(email, ip);
            throw invalido();
        }
        limite.autenticou(email);
        jdbc.update(
                "UPDATE usuario SET ultimo_acesso=? WHERE id=?",
                LocalDateTime.now(clock),
                usuario.getUsuarioId());
        UUID familia = UUID.randomUUID();
        Instant expira = clock.instant().plus(VALIDADE_REFRESH);
        jdbc.update(
                "INSERT INTO api_sessao(id,usuario_id,tenant_id,criado_em,expira_em) VALUES"
                    + " (?,?,?,?,?)",
                familia,
                usuario.getUsuarioId(),
                usuario.getTenantId(),
                Timestamp.from(clock.instant()),
                Timestamp.from(expira));
        return novos(usuario, familia, expira);
    }

    /**
     * O bloqueio serializa a rotação. Reutilização revoga toda a família, inclusive o token
     * recém-rotacionado.
     */
    @Transactional(noRollbackFor = RefreshReutilizado.class)
    public Tokens refresh(String token) {
        jwt.exigirConfiguracao();
        var registros =
                jdbc.query(
                        """
SELECT t.usuario_id,t.tenant_id,t.familia_id,t.credencial_hash,t.expira_em,t.revogado_em,
    s.revogado_em AS sessao_revogada
FROM api_refresh_token t JOIN api_sessao s ON s.id=t.familia_id
WHERE t.token_hash=? FOR UPDATE OF s,t
""",
                        (rs, n) ->
                                new Refresh(
                                        rs.getObject("usuario_id", UUID.class),
                                        rs.getObject("tenant_id", UUID.class),
                                        rs.getObject("familia_id", UUID.class),
                                        rs.getString("credencial_hash"),
                                        rs.getTimestamp("expira_em").toInstant(),
                                        rs.getTimestamp("revogado_em") != null
                                                || rs.getTimestamp("sessao_revogada") != null),
                        ApiJwtService.hash(token));
        if (registros.isEmpty()) throw invalido();
        var r = registros.getFirst();
        if (r.revogado()) {
            revogar(r.familia(), r.usuario());
            throw new RefreshReutilizado();
        }
        if (!r.expira().isAfter(clock.instant())) throw invalido();
        var usuario = contas.porId(r.usuario()).orElseThrow(ApiAuthService::invalido);
        if (!Objects.equals(r.tenant(), usuario.getTenantId())
                || !r.credencial().equals(ApiJwtService.credencial(usuario))) throw invalido();
        jdbc.update(
                "UPDATE api_refresh_token SET revogado_em=? WHERE token_hash=?",
                Timestamp.from(clock.instant()),
                ApiJwtService.hash(token));
        return novos(usuario, r.familia(), r.expira());
    }

    private Tokens novos(UsuarioPrincipal usuario, UUID familia, Instant expira) {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update(
                """
INSERT INTO api_refresh_token(token_hash,usuario_id,tenant_id,familia_id,credencial_hash,criado_em,expira_em)
VALUES (?,?,?,?,?,?,?)
""",
                ApiJwtService.hash(token),
                usuario.getUsuarioId(),
                usuario.getTenantId(),
                familia,
                ApiJwtService.credencial(usuario),
                Timestamp.from(clock.instant()),
                Timestamp.from(expira));
        return new Tokens(
                jwt.emitir(usuario, familia),
                token,
                "Bearer",
                ApiJwtService.VALIDADE.toSeconds(),
                expira.atOffset(ZoneOffset.UTC));
    }

    public boolean familiaAtiva(UUID familia, UUID usuario) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        """
                        SELECT EXISTS(SELECT 1 FROM api_sessao WHERE id=? AND usuario_id=?
                        AND revogado_em IS NULL AND expira_em>?)
                        """,
                        Boolean.class,
                        familia,
                        usuario,
                        Timestamp.from(clock.instant())));
    }

    public void revogar(UUID familia, UUID usuario) {
        jdbc.update(
                "UPDATE api_sessao SET revogado_em=? WHERE id=? AND usuario_id=? AND revogado_em IS"
                    + " NULL",
                Timestamp.from(clock.instant()),
                familia,
                usuario);
    }

    public void revogarUsuario(UUID usuario) {
        jdbc.update(
                "UPDATE api_sessao SET revogado_em=? WHERE usuario_id=? AND revogado_em IS NULL",
                Timestamp.from(clock.instant()),
                usuario);
    }

    static RegraNegocioException invalido() {
        return new RegraNegocioException(
                "credencial_invalida",
                "Credenciais inválidas ou expiradas.",
                HttpStatus.UNAUTHORIZED);
    }

    private record Refresh(
            UUID usuario,
            UUID tenant,
            UUID familia,
            String credencial,
            Instant expira,
            boolean revogado) {}

    public static class RefreshReutilizado extends RegraNegocioException {
        public RefreshReutilizado() {
            super(
                    "refresh_reutilizado",
                    "A sessão foi revogada. Entre novamente.",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    public static class LoginLimitado extends RegraNegocioException {
        private final long segundos;

        public LoginLimitado(long segundos) {
            super(
                    "login_limitado",
                    "Muitas tentativas. Aguarde antes de tentar novamente.",
                    HttpStatus.TOO_MANY_REQUESTS);
            this.segundos = segundos;
        }

        public long getSegundos() {
            return segundos;
        }
    }
}
