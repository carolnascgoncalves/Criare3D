package br.com.criare3d.repository;

import br.com.criare3d.models.Embalagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmbalagemRepository extends JpaRepository<Embalagem, UUID> {

    Page<Embalagem> findAllByUsuario_Id(UUID usuarioId, Pageable pageable);

    Page<Embalagem> findByNomeContainingIgnoreCaseAndUsuario_Id(String nome, UUID usuarioId, Pageable pageable);

    Optional<Embalagem> findByIdAndUsuario_Id(UUID id, UUID usuarioId);

    boolean existsByIdAndUsuario_Id(UUID id, UUID usuarioId);
}