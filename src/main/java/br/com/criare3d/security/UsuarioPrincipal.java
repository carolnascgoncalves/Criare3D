package br.com.criare3d.security;

import br.com.criare3d.models.Usuario;
import br.com.criare3d.models.enums.Perfil;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public final class UsuarioPrincipal implements UserDetails, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID usuarioId;
    private final UUID tenantId;
    private final String email;
    private final String password;
    private final Perfil perfil;
    private final boolean trocaSenhaObrigatoria;
    private final boolean enabled;

    public UsuarioPrincipal(Usuario usuario, boolean tenantAtivo) {
        this.usuarioId = usuario.getId();
        this.tenantId = usuario.getTenant() == null ? null : usuario.getTenant().getId();
        this.email = usuario.getEmail();
        this.password = usuario.getPassword();
        this.perfil = usuario.getPerfil();
        this.trocaSenhaObrigatoria = usuario.isTrocaSenhaObrigatoria();
        this.enabled = perfil == Perfil.ADMIN || (tenantId != null && tenantAtivo);
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isTrocaSenhaObrigatoria() {
        return trocaSenhaObrigatoria;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
