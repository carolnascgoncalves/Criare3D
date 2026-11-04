package br.com.criare3d.repository;

import br.com.criare3d.models.Peca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PecaRepository extends JpaRepository<Peca, UUID> {

    Page<Peca> findAllByUsuario_Id(UUID usuarioId, Pageable pageable);

    Page<Peca> findByNomeContainingIgnoreCaseAndUsuario_Id(String nome, UUID usuarioId, Pageable pageable);

    Optional<Peca> findByIdAndUsuario_Id(UUID id, UUID usuarioId);

    boolean existsByIdAndUsuario_Id(UUID id, UUID usuarioId);
}