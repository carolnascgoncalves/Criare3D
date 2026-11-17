package br.com.criare3d.service;

import br.com.criare3d.dto.FotoConteudo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Uma foto por cálculo, vinculada ao rascunho persistente. Fotos avulsas expiram em 30 minutos.
 */
@Service
public class FotoRascunhoService {
    public FotoRascunhoService(JdbcTemplate jdbc, UsuarioLogadoService usuario, Clock clock) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc, "jdbc");
        this.usuario = java.util.Objects.requireNonNull(usuario, "usuario");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
    }

    private final JdbcTemplate jdbc;
    private final UsuarioLogadoService usuario;
    private final Clock clock;

    public UUID salvar(String tipo, byte[] bytes) {
        return salvar(null, tipo, bytes);
    }

    public UUID salvar(UUID rascunhoId, String tipo, byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        if (bytes.length > 3 * 1024 * 1024 || tipo == null || !Set.of("image/png", "image/jpeg", "image/webp").contains(tipo))
            throw new IllegalArgumentException("Envie uma foto PNG, JPEG ou WebP com até 3 MB.");
        UUID id = UUID.randomUUID();
        int alterados = jdbc.update("""
                INSERT INTO foto_rascunho(id, usuario_id, tenant_id, tipo, bytes, atualizado_em, rascunho_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (rascunho_id) DO UPDATE SET id = EXCLUDED.id, tipo = EXCLUDED.tipo,
                    bytes = EXCLUDED.bytes, atualizado_em = EXCLUDED.atualizado_em
                WHERE foto_rascunho.usuario_id=EXCLUDED.usuario_id AND foto_rascunho.tenant_id=EXCLUDED.tenant_id
                """, id, usuario.buscarIdUsuarioAtual(), usuario.buscarTenantIdAtual(), tipo, bytes,
            java.sql.Timestamp.from(clock.instant()), rascunhoId);
        if (alterados != 1) throw new IllegalArgumentException("Rascunho de foto não encontrado.");
        return id;
    }

    public Optional<FotoConteudo> buscar(Object id) {
        if (!(id instanceof UUID)) return Optional.empty();
        return jdbc.query("""
            SELECT f.tipo, f.bytes FROM foto_rascunho f WHERE f.id=? AND f.usuario_id=? AND f.tenant_id=?
            AND (f.atualizado_em>? OR EXISTS (
                SELECT 1 FROM calculo_rascunho r WHERE r.id=f.rascunho_id AND r.usuario_id=f.usuario_id
                AND r.tenant_id=f.tenant_id AND r.atualizado_em>?
            ))
            """,
            (rs, n) -> new FotoConteudo(rs.getString(1), rs.getBytes(2)), id, usuario.buscarIdUsuarioAtual(),
            usuario.buscarTenantIdAtual(), java.sql.Timestamp.from(clock.instant().minusSeconds(1800)),
            java.sql.Timestamp.from(clock.instant().minus(CalculoRascunhoService.RETENCAO))).stream().findFirst();
    }

    public void excluir(Object id) {
        if (id instanceof UUID)
            jdbc.update("DELETE FROM foto_rascunho WHERE id = ? AND usuario_id = ? AND tenant_id = ?",
                id, usuario.buscarIdUsuarioAtual(), usuario.buscarTenantIdAtual());
    }
}
