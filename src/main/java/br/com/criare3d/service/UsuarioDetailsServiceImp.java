
package br.com.criare3d.service;

import br.com.criare3d.models.Usuario;
import br.com.criare3d.repository.UsuarioRepository;
import br.com.criare3d.service.interfaces.UsuarioDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class UsuarioDetailsServiceImp implements UsuarioDetailsService {

    private final UsuarioRepository usuarioRepository;
    public UsuarioDetailsServiceImp(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        return new br.com.criare3d.security.UsuarioPrincipal(usuario);
    }
}
