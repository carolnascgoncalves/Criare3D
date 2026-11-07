package br.com.criare3d.repository;

import br.com.criare3d.models.Impressora;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ImpressoraRepository extends JpaRepository<Impressora, UUID> {

    Page<Impressora> findAllByUsuario_Id(UUID usuarioId, Pageable pageable);

    long countByUsuario_Id(UUID usuarioId);

    List<Impressora> findAllByIdInAndUsuario_Id(Collection<UUID> ids, UUID usuarioId);

    Page<Impressora> findByNomeContainingIgnoreCaseAndUsuario_Id(String nome, UUID usuarioId, Pageable pageable);

    Optional<Impressora> findByIdAndUsuario_Id(UUID id, UUID usuarioId);

    boolean existsByIdAndUsuario_Id(UUID id, UUID usuarioId);
}
