
package br.com.criare3d.service;

import br.com.criare3d.models.Usuario;
import br.com.criare3d.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional( propagation = Propagation.REQUIRED)
public class UsuarioDetailsServiceImp implements UserDetailsService {
    @Autowired
    private UsuarioRepository usuarioRepository;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        return User.withUsername(usuario.getEmail())
                .password(usuario.getPassword())
                .roles(usuario.getPerfil().name())
                .build();
    }
}
