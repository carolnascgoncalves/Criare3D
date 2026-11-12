package br.com.criare3d.service;

import br.com.criare3d.config.AdminBootstrap;
import br.com.criare3d.models.Usuario;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminBootstrapTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final MockEnvironment env = new MockEnvironment().withProperty("ADMIN_EMAIL", "admin@example.test")
            .withProperty("ADMIN_PASSWORD", "SenhaConfigurada123!");
    private Usuario existente(Perfil perfil) {
        var usuario = Usuario.builder().email("admin@example.test").password("hash-anterior").perfil(perfil).build();
        when(usuarios.findByEmail("admin@example.test")).thenReturn(Optional.of(usuario));
        return usuario;
    }
    @Test void ambienteNaoSobrescreveSenhaExistentePorPadrao() {
        var usuario = existente(Perfil.ADMIN);
        new AdminBootstrap(usuarios,encoder,env).run(null);
        assertThat(usuario.getPassword()).isEqualTo("hash-anterior");
        verifyNoInteractions(encoder); verify(usuarios,never()).save(any());
    }
    @Test void recuperacaoExplicitaAtualizaSomenteOAdminCorrespondente() {
        var usuario = existente(Perfil.ADMIN);
        env.withProperty("ADMIN_REDEFINIR_SENHA", "true");
        when(encoder.encode("SenhaConfigurada123!")).thenReturn("hash-novo");
        new AdminBootstrap(usuarios,encoder,env).run(null);
        assertThat(usuario.getPassword()).isEqualTo("hash-novo");
        verify(usuarios).save(usuario);
    }
    @Test void recuperacaoNaoPromoveUsuarioComum() {
        var usuario = existente(Perfil.USER);
        env.withProperty("ADMIN_REDEFINIR_SENHA", "true");
        assertThatIllegalStateException().isThrownBy(() -> new AdminBootstrap(usuarios,encoder,env).run(null));
        assertThat(usuario.getPassword()).isEqualTo("hash-anterior");
        verifyNoInteractions(encoder); verify(usuarios,never()).save(any());
    }
}
