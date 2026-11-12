package br.com.criare3d.service;

import br.com.criare3d.repository.UsuarioRepository;
import br.com.criare3d.security.LimiteLogin;
import br.com.criare3d.security.UsuarioPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class RecuperacaoSenhaService {
    public RecuperacaoSenhaService(
            UsuarioRepository usuarios,
            JdbcTemplate jdbc,
            Clock clock,
            PasswordEncoder encoder,
            EmailAcessoService emailService,
            PlatformTransactionManager transactions,
            ThreadPoolTaskExecutor notificacoesExecutor,
            SessionRegistry sessoes) {
        this.usuarios = java.util.Objects.requireNonNull(usuarios, "usuarios");
        this.jdbc = java.util.Objects.requireNonNull(jdbc, "jdbc");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        this.limite = new LimiteLogin(clock);
        this.encoder = java.util.Objects.requireNonNull(encoder, "encoder");
        this.emailService = java.util.Objects.requireNonNull(emailService, "emailService");
        this.transactions = java.util.Objects.requireNonNull(transactions, "transactions");
        this.notificacoesExecutor = java.util.Objects.requireNonNull(notificacoesExecutor, "notificacoesExecutor");
        this.sessoes = java.util.Objects.requireNonNull(sessoes, "sessoes");
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RecuperacaoSenhaService.class);
    private final UsuarioRepository usuarios;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final PasswordEncoder encoder;
    private final EmailAcessoService emailService;
    private final PlatformTransactionManager transactions;
    private final ThreadPoolTaskExecutor notificacoesExecutor;
    private final SessionRegistry sessoes;
    private final LimiteLogin limite;
    private final SecureRandom random = new SecureRandom();

    public boolean habilitado() { return emailService.habilitado(); }

    public void solicitar(String email, String ip) {
        if (!habilitado() || limite.esperaSegundos(email, ip) > 0) return;
        limite.falhou(email, ip);
        try {
            notificacoesExecutor.execute(() -> {
                try {
                    Entrega entrega = new TransactionTemplate(transactions).execute(status -> {
                        var usuario = usuarios.findByEmail(email.strip().toLowerCase(Locale.ROOT)).orElse(null);
                        if (usuario == null || (usuario.getTenant() != null && !usuario.getTenant().isAtivo())) return null;
                        byte[] bytes = new byte[32]; random.nextBytes(bytes);
                        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                        // Uma nova solicitação revoga os links anteriores desta conta.
                        jdbc.update("DELETE FROM recuperacao_senha WHERE usuario_id = ?", usuario.getId());
                        jdbc.update("INSERT INTO recuperacao_senha(token_hash, usuario_id, expira_em) VALUES (?, ?, ?)",
                                hash(token), usuario.getId(), java.sql.Timestamp.from(clock.instant().plusSeconds(900)));
                        return new Entrega(usuario.getEmail(), token);
                    });
                    if (entrega != null) emailService.enviarRecuperacao(entrega.email(), entrega.token());
                } catch (Exception erro) {
                    // Não registra endereço, token, credenciais ou corpo de resposta do provedor.
                    LOG.error("Não foi possível entregar recuperação de acesso ({})", erro.getClass().getSimpleName());
                }
            });
        } catch (org.springframework.core.task.TaskRejectedException erro) {
            LOG.warn("Fila de recuperação de acesso cheia");
        }
    }

    @Transactional
    public void redefinir(String token, String senha) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}") || senha == null || senha.length() < 12
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) throw invalido();
        var ids = jdbc.query("SELECT usuario_id FROM recuperacao_senha WHERE token_hash = ? AND expira_em > ? FOR UPDATE",
                (rs, linha) -> rs.getObject(1, UUID.class), hash(token), java.sql.Timestamp.from(clock.instant()));
        if (ids.size() != 1) throw invalido();
        UUID id = ids.getFirst();
        var usuario = usuarios.findById(id).orElseThrow(RecuperacaoSenhaService::invalido);
        if (usuario.getTenant() != null && !usuario.getTenant().isAtivo()) throw invalido();
        usuario.setPassword(encoder.encode(senha));
        usuario.setTrocaSenhaObrigatoria(false);
        usuarios.save(usuario);
        jdbc.update("DELETE FROM recuperacao_senha WHERE usuario_id = ?", id);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                sessoes.getAllPrincipals().stream().filter(p -> p instanceof UsuarioPrincipal u && id.equals(u.getUsuarioId()))
                        .forEach(p -> sessoes.getAllSessions(p, false).forEach(s -> s.expireNow()));
            }
        });
    }

    static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException erro) { throw new IllegalStateException(erro); }
    }
    private static IllegalArgumentException invalido() { return new IllegalArgumentException("Link inválido ou expirado. Solicite uma nova recuperação de senha."); }
    private record Entrega(String email, String token) {}
}
