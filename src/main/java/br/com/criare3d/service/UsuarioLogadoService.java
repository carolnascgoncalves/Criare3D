package br.com.criare3d.service;

import br.com.criare3d.models.Usuario;
import br.com.criare3d.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UsuarioLogadoService {

    private static final String USUARIO_ATUAL_REQUEST_ATTRIBUTE =
            UsuarioLogadoService.class.getName() + ".usuarioAtual";

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

        RequestAttributes atributosRequisicao = RequestContextHolder.getRequestAttributes();
        if (atributosRequisicao != null) {
            Object usuarioEmCache = atributosRequisicao.getAttribute(
                    USUARIO_ATUAL_REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            if (usuarioEmCache instanceof Usuario usuario) return usuario;
        }

        String email = autenticacao.getName().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Não foi possível localizar o usuário autenticado."));
        if (atributosRequisicao != null) {
            atributosRequisicao.setAttribute(
                    USUARIO_ATUAL_REQUEST_ATTRIBUTE, usuario, RequestAttributes.SCOPE_REQUEST);
        }
        return usuario;
    }

    public UUID buscarIdUsuarioAtual() {
        return buscarUsuarioAtual().getId();
    }
}
