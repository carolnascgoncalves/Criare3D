package br.com.criare3d.service;

import br.com.criare3d.dto.FotoConteudo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.util.*;

/** Um rascunho por usuário, fora da sessão HTTP e com expiração de 30 minutos. */
@Service
public class FotoRascunhoService {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuarioLogadoService usuario;
    @Autowired private Clock clock;

    public UUID salvar(String tipo, byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        if (bytes.length > 3 * 1024 * 1024 || tipo == null || !Set.of("image/png", "image/jpeg", "image/webp").contains(tipo))
            throw new IllegalArgumentException("Envie uma foto PNG, JPEG ou WebP com até 3 MB.");
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO foto_rascunho(id, usuario_id, tenant_id, tipo, bytes, atualizado_em)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (usuario_id) DO UPDATE SET id = EXCLUDED.id, tipo = EXCLUDED.tipo,
                    bytes = EXCLUDED.bytes, atualizado_em = EXCLUDED.atualizado_em
                """, id, usuario.buscarIdUsuarioAtual(), usuario.buscarTenantIdAtual(), tipo, bytes,
                java.sql.Timestamp.from(clock.instant()));
        return id;
    }

    public Optional<FotoConteudo> buscar(Object id) {
        if (!(id instanceof UUID)) return Optional.empty();
        return jdbc.query("SELECT tipo, bytes FROM foto_rascunho WHERE id = ? AND usuario_id = ? AND tenant_id = ? AND atualizado_em > ?",
                (rs, n) -> new FotoConteudo(rs.getString(1), rs.getBytes(2)), id, usuario.buscarIdUsuarioAtual(),
                usuario.buscarTenantIdAtual(), java.sql.Timestamp.from(clock.instant().minusSeconds(1800))).stream().findFirst();
    }

    public void excluir(Object id) {
        if (id instanceof UUID) jdbc.update("DELETE FROM foto_rascunho WHERE id = ? AND usuario_id = ? AND tenant_id = ?",
                id, usuario.buscarIdUsuarioAtual(), usuario.buscarTenantIdAtual());
    }
}
