package br.com.criare3d.service;

import br.com.criare3d.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

class RecuperacaoSenhaTest {
    @Test void encoderNaoTruncaSenhaELoginLongoRetornaInvalido() {
        var encoder = new SecurityConfig(null, null).passwordEncoder();
        String senha = "á".repeat(36);
        String hash = encoder.encode(senha);
        assertThat(encoder.matches(senha, hash)).isTrue();
        assertThat(encoder.matches(senha + "a", hash)).isFalse();
        assertThatIllegalArgumentException().isThrownBy(() -> encoder.encode(senha + "a"));
    }

    @Test void recuperacaoRejeitaTokenMalformadoAntesDeAcessarBanco() {
        var service = new RecuperacaoSenhaService(org.mockito.Mockito.mock(br.com.criare3d.repository.UsuarioRepository.class),
                org.mockito.Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class), java.time.Clock.systemUTC(),
                org.mockito.Mockito.mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                org.mockito.Mockito.mock(EmailAcessoService.class), org.mockito.Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class),
                org.mockito.Mockito.mock(org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor.class),
                org.mockito.Mockito.mock(org.springframework.security.core.session.SessionRegistry.class));
        for (String token : new String[]{"", "a".repeat(42), "a".repeat(44), "<script>"})
            assertThatIllegalArgumentException().isThrownBy(() -> service.redefinir(token, "SenhaDeTeste123!"))
                    .withMessageContaining("Link inválido ou expirado");
        assertThatIllegalArgumentException().isThrownBy(() -> service.redefinir("a".repeat(43), "á".repeat(37)));
    }

    @Test void emailSoAtivaComConfiguracaoCompletaEUrlHttpsConfiavel() {
        var service = new EmailAcessoService(org.mockito.Mockito.mock(HttpIntegracaoService.class));
        ReflectionTestUtils.setField(service, "apiKey", "teste");
        ReflectionTestUtils.setField(service, "remetente", "acesso@example.test");
        for (String url : new String[]{"", "http://example.test", "não é uma URL", "https://pessoa:senha@example.test", "https://example.test?origem=1"}) {
            ReflectionTestUtils.setField(service, "publicUrl", url);
            assertThat(service.habilitado()).isFalse();
        }
        ReflectionTestUtils.setField(service, "publicUrl", "https://example.test");
        assertThat(service.habilitado()).isTrue();
    }
}
