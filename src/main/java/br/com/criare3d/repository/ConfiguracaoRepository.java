package br.com.criare3d.repository;

import br.com.criare3d.models.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, UUID> {

    Optional<Configuracao> findFirstByUsuario_Id(UUID usuarioId);

    Optional<Configuracao> findFirstByTenantIdAndUsuario_Id(UUID tenantId, UUID usuarioId);

    Optional<Configuracao> findByIdAndUsuario_Id(UUID id, UUID usuarioId);

    boolean existsByIdAndUsuario_Id(UUID id, UUID usuarioId);

    Page<Configuracao> findAllByUsuario_Id(UUID usuarioId, Pageable pageable);
}
