package br.com.criare3d.api;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.criare3d.api.auth.*;
import br.com.criare3d.exception.RegraNegocioException;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.security.*;

import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.*;
import java.time.*;
import java.util.*;

class ApiAuthServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneOffset.UTC);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ApiContas contas = mock(ApiContas.class);
    private final BCryptPasswordEncoder senhas = new BCryptPasswordEncoder(4);
    private final ApiJwtService jwt =
            new ApiJwtService("YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=", clock);
    private final LimiteLogin limite = new LimiteLogin(clock);
    private final ApiAuthService auth =
            new ApiAuthService(jdbc, contas, jwt, senhas, limite, clock);
    private UsuarioPrincipal usuario;
    private final UUID familia = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        var t = new Tenant();
        t.setId(UUID.randomUUID());
        usuario =
                new UsuarioPrincipal(
                        Usuario.builder()
                                .id(UUID.randomUUID())
                                .email("teste@example.test")
                                .password(senhas.encode("SenhaCorreta123!"))
                                .perfil(Perfil.USER)
                                .tenant(t)
                                .build());
        when(contas.porEmail(anyString())).thenReturn(Optional.of(usuario));
        when(contas.porId(usuario.getUsuarioId())).thenReturn(Optional.of(usuario));
    }

    @Test
    void loginEmiteJwtCurtoEGuardaSomenteHashDoRefresh() {
        var tokens = auth.login(usuario.getUsername(), "SenhaCorreta123!", "127.0.0.1");
        assertThat(tokens.expiresIn()).isEqualTo(900);
        assertThat(tokens.refreshToken()).hasSize(43);
        var claims = jwt.ler(tokens.accessToken());
        assertThat(claims.getSubject()).isEqualTo(usuario.getUsuarioId().toString());
        assertThat(claims.getExpiresAt()).isEqualTo(clock.instant().plusSeconds(900));
        verify(jdbc)
                .update(
                        contains("INSERT INTO api_refresh_token"),
                        eq(ApiJwtService.hash(tokens.refreshToken())),
                        eq(usuario.getUsuarioId()),
                        eq(usuario.getTenantId()),
                        any(UUID.class),
                        eq(ApiJwtService.credencial(usuario)),
                        eq(Timestamp.from(clock.instant())),
                        eq(Timestamp.from(clock.instant().plus(Duration.ofDays(30)))));
    }

    @Test
    void contaInexistenteESenhaErradaTemMesmoErroELimiteEhReaproveitado() {
        when(contas.porEmail("ausente@example.test")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auth.login("ausente@example.test", "SenhaIncorreta", "ip-a"))
                .isInstanceOfSatisfying(
                        RegraNegocioException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("credencial_invalida"));
        for (int i = 0; i < 5; i++)
            assertThatThrownBy(() -> auth.login(usuario.getUsername(), "SenhaIncorreta", "ip-b"))
                    .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> auth.login(usuario.getUsername(), "SenhaCorreta123!", "ip-b"))
                .isInstanceOf(ApiAuthService.LoginLimitado.class);
        verify(jdbc, never()).update(contains("INSERT INTO"), any(Object[].class));
    }

    private void rascunhoRefresh(boolean revogado, Instant expira, String credencial)
            throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject("usuario_id", UUID.class)).thenReturn(usuario.getUsuarioId());
        when(rs.getObject("tenant_id", UUID.class)).thenReturn(usuario.getTenantId());
        when(rs.getObject("familia_id", UUID.class)).thenReturn(familia);
        when(rs.getString("credencial_hash")).thenReturn(credencial);
        when(rs.getTimestamp("expira_em")).thenReturn(Timestamp.from(expira));
        when(rs.getTimestamp("revogado_em"))
                .thenReturn(revogado ? Timestamp.from(clock.instant()) : null);
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenAnswer(
                        i -> {
                            RowMapper<?> mapper = i.getArgument(1);
                            return List.of(mapper.mapRow(rs, 0));
                        });
    }

    @Test
    void refreshRotacionaSemProrrogarPrazoAbsolutoDaSessao() throws Exception {
        Instant fim = clock.instant().plusSeconds(3600);
        rascunhoRefresh(false, fim, ApiJwtService.credencial(usuario));
        var tokens = auth.refresh("r".repeat(43));
        assertThat(tokens.refreshToken()).isNotEqualTo("r".repeat(43));
        assertThat(tokens.refreshExpiraEm().toInstant()).isEqualTo(fim);
        assertThat(jwt.ler(tokens.accessToken()).getClaimAsString("sid"))
                .isEqualTo(familia.toString());
        verify(jdbc)
                .update(
                        contains("WHERE token_hash=?"),
                        eq(Timestamp.from(clock.instant())),
                        eq(ApiJwtService.hash("r".repeat(43))));
    }

    @Test
    void reutilizacaoRevogaFamiliaInteira() throws Exception {
        rascunhoRefresh(true, clock.instant().plusSeconds(3600), ApiJwtService.credencial(usuario));
        assertThatThrownBy(() -> auth.refresh("r".repeat(43)))
                .isInstanceOf(ApiAuthService.RefreshReutilizado.class);
        verify(jdbc)
                .update(
                        contains("UPDATE api_sessao SET revogado_em=? WHERE id=? AND usuario_id=?"),
                        eq(Timestamp.from(clock.instant())),
                        eq(familia),
                        eq(usuario.getUsuarioId()));
        verify(jdbc, never()).update(contains("INSERT INTO"), any(Object[].class));
    }

    @Test
    void refreshExpiradoOuDeSenhaAnteriorNaoEmiteCredenciais() throws Exception {
        rascunhoRefresh(false, clock.instant(), ApiJwtService.credencial(usuario));
        assertThatThrownBy(() -> auth.refresh("r".repeat(43)))
                .isInstanceOf(RegraNegocioException.class);
        rascunhoRefresh(false, clock.instant().plusSeconds(3600), "hash-da-senha-anterior");
        assertThatThrownBy(() -> auth.refresh("r".repeat(43)))
                .isInstanceOf(RegraNegocioException.class);
        verify(jdbc, never()).update(contains("INSERT INTO"), any(Object[].class));
    }

    @Test
    void semChaveConfiguradaNaoHabilitaLoginENaoAfetaInicializacaoDoSite() {
        var semChave = new ApiJwtService("", clock);
        assertThatThrownBy(() -> semChave.emitir(usuario, familia))
                .isInstanceOfSatisfying(
                        RegraNegocioException.class,
                        e -> assertThat(e.getStatus().value()).isEqualTo(503));
        assertThatIllegalStateException()
                .isThrownBy(
                        () ->
                                new ApiJwtService(
                                        Base64.getEncoder().encodeToString(new byte[8]), clock));
    }
}
