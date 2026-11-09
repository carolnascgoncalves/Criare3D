package br.com.criare3d.repository;

import br.com.criare3d.models.Tenant;
import br.com.criare3d.dto.TenantTemaProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);

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
