package br.com.criare3d.service;

import br.com.criare3d.models.Usuario;
import br.com.criare3d.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UsuarioLogadoService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioLogadoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario buscarUsuarioAtual() {
        Authentication autenticacao =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null || !autenticacao.isAuthenticated() || "anonymousUser".equals(autenticacao.getPrincipal())) {
            throw new IllegalStateException("Nenhum usuário autenticado.");
        }

        String email = autenticacao.getName().trim().toLowerCase(Locale.ROOT);

        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Não foi possível localizar o usuário autenticado."));
    }

    public UUID buscarIdUsuarioAtual() {
        return buscarUsuarioAtual().getId();
    }
}