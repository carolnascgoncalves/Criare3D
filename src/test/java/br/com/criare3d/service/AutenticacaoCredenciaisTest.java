package br.com.criare3d.service;

import br.com.criare3d.config.SecurityConfig;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AutenticacaoCredenciaisTest {
    @Test void hashesAnterioresContinuamAutenticandoAdminECliente() {
        for (Perfil perfil : new Perfil[]{Perfil.ADMIN, Perfil.USER}) {
            var usuarios = mock(UsuarioRepository.class);
            var usuario = Usuario.builder().id(UUID.randomUUID()).email("conta@example.test").perfil(perfil)
                    .password(new BCryptPasswordEncoder(4).encode("SenhaCorreta123!")).build();
            if (perfil == Perfil.USER) { var tenant = new Tenant(); tenant.setId(UUID.randomUUID()); usuario.setTenant(tenant); }
            when(usuarios.findByEmail("conta@example.test")).thenReturn(Optional.of(usuario));
            var provider = new DaoAuthenticationProvider(new UsuarioDetailsServiceImp(usuarios));
            provider.setPasswordEncoder(new SecurityConfig(null,null).passwordEncoder());
            var auth = provider.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(" CONTA@example.test ", "SenhaCorreta123!"));
            assertThat(auth.isAuthenticated()).isTrue();
            assertThat(auth.getName()).isEqualTo("conta@example.test");
        }
    }
}
