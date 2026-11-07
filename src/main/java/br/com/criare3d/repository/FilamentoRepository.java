package br.com.criare3d.repository;

import br.com.criare3d.models.Filamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FilamentoRepository extends
        JpaRepository<Filamento, UUID>,
        JpaSpecificationExecutor<Filamento> {

    Page<Filamento> findAllByUsuario_Id(UUID usuarioId, Pageable pageable);

    Page<Filamento> findByNomeContainingIgnoreCaseAndUsuario_Id(String nome, UUID usuarioId, Pageable pageable);

    Optional<Filamento> findByIdAndUsuario_Id(UUID id, UUID usuarioId);

    List<Filamento> findAllByIdInAndUsuario_Id(Collection<UUID> ids, UUID usuarioId);

    boolean existsByIdAndUsuario_Id(UUID id, UUID usuarioId);
}
