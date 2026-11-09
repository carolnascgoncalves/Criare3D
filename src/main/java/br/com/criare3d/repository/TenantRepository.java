package br.com.criare3d.repository;

import br.com.criare3d.models.Tenant;
import br.com.criare3d.dto.TenantTemaProjection;
import br.com.criare3d.dto.ClienteAdminResumo;
import br.com.criare3d.models.enums.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("""
        SELECT t.id AS id,
               t.nome AS nome,
               t.slug AS slug,
               t.ativo AS ativo,
               t.corPrimaria AS corPrimaria,
               t.corDestaque AS corDestaque,
               t.corFundo AS corFundo,
               t.temaVersao AS temaVersao,
               CASE WHEN t.logo IS NULL THEN false ELSE true END AS temLogo,
               u.id AS usuarioId,
               u.email AS email
        FROM Tenant t
        LEFT JOIN Usuario u ON u.tenant = t AND u.perfil = :perfil
        ORDER BY t.nome
        """)
    List<ClienteAdminResumo> listarClientesAdmin(@Param("perfil") Perfil perfil);

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
