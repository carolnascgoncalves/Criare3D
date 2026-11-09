package br.com.criare3d.repository;

import br.com.criare3d.models.Tenant;
import br.com.criare3d.dto.TenantTemaProjection;
import br.com.criare3d.dto.ClienteAdminResumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query(value = """
        SELECT t.id AS id,
               t.nome AS nome,
               t.slug AS slug,
               t.ativo AS ativo,
               t.cor_primaria AS "corPrimaria",
               t.cor_destaque AS "corDestaque",
               t.cor_fundo AS "corFundo",
               t.tema_versao AS "temaVersao",
               CASE WHEN t.logo IS NULL THEN FALSE ELSE TRUE END AS "temLogo",
               u.id AS "usuarioId",
               u.email AS email,
               u.ultimo_acesso AS "ultimoAcesso",
               COUNT(ch.id) AS "quantidadeCalculos"
        FROM tenant t
        LEFT JOIN usuario u ON u.tenant_id = t.id AND u.perfil = 'USER'
        LEFT JOIN calculo_historico ch ON ch.tenant_id = t.id
        GROUP BY t.id, t.nome, t.slug, t.ativo, t.cor_primaria, t.cor_destaque,
                 t.cor_fundo, t.tema_versao, t.logo, u.id, u.email, u.ultimo_acesso
        ORDER BY t.nome
        """, nativeQuery = true)
    List<ClienteAdminResumo> listarClientesAdmin();

    @Query("""
        SELECT t.nome AS nome,
               t.corPrimaria AS corPrimaria,
               t.corDestaque AS corDestaque,
               t.corFundo AS corFundo,
               t.temaVersao AS temaVersao,
               t.ativo AS ativo,
               CASE WHEN t.logo IS NULL THEN false ELSE true END AS temLogo
        FROM Tenant t
        WHERE t.id = :tenantId
        """)
    Optional<TenantTemaProjection> buscarTema(@Param("tenantId") UUID tenantId);
}
