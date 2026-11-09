package br.com.criare3d.security;

import br.com.criare3d.models.Usuario;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.dto.TenantTemaProjection;
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
    private final String tenantNome;
    private final String corPrimaria;
    private final String corDestaque;
    private final String corFundo;
    private final long temaVersao;
    private final boolean tenantTemLogo;

    public UsuarioPrincipal(Usuario usuario, boolean tenantAtivo) {
        this(usuario, null, tenantAtivo);
    }

    public UsuarioPrincipal(Usuario usuario, TenantTemaProjection tema) {
        this(usuario, tema, tema != null && Boolean.TRUE.equals(tema.getAtivo()));
    }

    private UsuarioPrincipal(Usuario usuario, TenantTemaProjection tema, boolean tenantAtivo) {
        this.usuarioId = usuario.getId();
        this.tenantId = usuario.getTenant() == null ? null : usuario.getTenant().getId();
        this.email = usuario.getEmail();
        this.password = usuario.getPassword();
        this.perfil = usuario.getPerfil();
        this.trocaSenhaObrigatoria = usuario.isTrocaSenhaObrigatoria();
        this.enabled = perfil == Perfil.ADMIN || (tenantId != null && tenantAtivo);
        this.tenantNome = tema == null ? null : tema.getNome();
        this.corPrimaria = tema == null ? null : tema.getCorPrimaria();
        this.corDestaque = tema == null ? null : tema.getCorDestaque();
        this.corFundo = tema == null ? null : tema.getCorFundo();
        this.temaVersao = tema == null || tema.getTemaVersao() == null ? 1L : tema.getTemaVersao();
        this.tenantTemLogo = tema != null && Boolean.TRUE.equals(tema.getTemLogo());
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

    public String getTenantNome() { return tenantNome; }
    public String getCorPrimaria() { return corPrimaria; }
    public String getCorDestaque() { return corDestaque; }
    public String getCorFundo() { return corFundo; }
    public long getTemaVersao() { return temaVersao; }
    public boolean isTenantTemLogo() { return tenantTemLogo; }

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
