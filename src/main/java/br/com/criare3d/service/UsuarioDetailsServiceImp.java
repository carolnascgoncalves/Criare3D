
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
    private final TenantTemaCache tenantTemaCache;

    public UsuarioDetailsServiceImp(UsuarioRepository usuarioRepository, TenantTemaCache tenantTemaCache) {
        this.usuarioRepository = usuarioRepository;
        this.tenantTemaCache = tenantTemaCache;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));

        var tenantId = usuario.getTenant() == null ? null : usuario.getTenant().getId();
        var tema = tenantId == null ? null : tenantTemaCache.buscarTema(tenantId).orElse(null);
        return new br.com.criare3d.security.UsuarioPrincipal(usuario, tema);
    }
}
