package br.com.criare3d.api.auth;

import br.com.criare3d.models.Tenant;
import br.com.criare3d.models.Usuario;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.security.UsuarioPrincipal;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** Consulta de identidade antes de abrir qualquer sessão Hibernate de um tenant. */
@Service
public class ApiContas {
    private final JdbcTemplate jdbc;

    public ApiContas(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT =
            """
            SELECT u.id,u.email,u.password,u.perfil,u.troca_senha_obrigatoria,u.tenant_id
            FROM usuario u LEFT JOIN tenant t ON t.id=u.tenant_id
            WHERE (u.tenant_id IS NULL OR (t.ativo=true AND t.excluido_em IS NULL)) AND
            """;

    public Optional<UsuarioPrincipal> porEmail(String email) {
        return buscar("lower(u.email)=?", email.strip().toLowerCase(Locale.ROOT));
    }

    public Optional<UsuarioPrincipal> porId(UUID id) {
        return buscar("u.id=?", id);
    }

    private Optional<UsuarioPrincipal> buscar(String filtro, Object valor) {
        return jdbc
                .query(
                        SELECT + filtro,
                        (rs, n) -> {
                            Tenant tenant = null;
                            UUID tenantId = rs.getObject("tenant_id", UUID.class);
                            if (tenantId != null) {
                                tenant = new Tenant();
                                tenant.setId(tenantId);
                            }
                            return new UsuarioPrincipal(
                                    Usuario.builder()
                                            .id(rs.getObject("id", UUID.class))
                                            .email(rs.getString("email"))
                                            .password(rs.getString("password"))
                                            .perfil(Perfil.valueOf(rs.getString("perfil")))
                                            .tenant(tenant)
                                            .trocaSenhaObrigatoria(
                                                    rs.getBoolean("troca_senha_obrigatoria"))
                                            .build());
                        },
                        valor)
                .stream()
                .filter(UsuarioPrincipal::isEnabled)
                .findFirst();
    }
}
