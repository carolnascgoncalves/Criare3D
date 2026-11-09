package br.com.criare3d.service;

import br.com.criare3d.models.Tenant;
import br.com.criare3d.models.Usuario;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.security.UsuarioPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UsuarioLogadoService {

    public Usuario buscarUsuarioAtual() {
        UsuarioPrincipal principal = buscarPrincipalAtual();
        Tenant tenant = null;
        if (principal.getTenantId() != null) {
            tenant = new Tenant();
            tenant.setId(principal.getTenantId());
        }

        return Usuario.builder()
                .id(principal.getUsuarioId())
                .email(principal.getUsername())
                .perfil(principal.getPerfil())
                .trocaSenhaObrigatoria(principal.isTrocaSenhaObrigatoria())
                .tenant(tenant)
                .build();
    }

    public UUID buscarIdUsuarioAtual() {
        return buscarPrincipalAtual().getUsuarioId();
    }

    public UUID buscarTenantIdAtual() {
        return buscarPrincipalAtual().getTenantId();
    }

    public boolean usuarioAtualEhAdmin() {
        return buscarPrincipalAtual().getPerfil() == Perfil.ADMIN;
    }

    private UsuarioPrincipal buscarPrincipalAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()
                || !(autenticacao.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new IllegalStateException("Não foi possível identificar a conta autenticada.");
        }
        return principal;
    }
}
