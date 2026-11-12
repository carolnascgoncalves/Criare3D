package br.com.criare3d.repository;

import br.com.criare3d.models.Peca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PecaRepository extends JpaRepository<Peca, UUID> {

    List<Peca> findAllByIdIn(Collection<UUID> ids);

    Page<Peca> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
    Page<Peca> findByArquivadoFalse(Pageable pageable);
    Page<Peca> findByArquivadoTrue(Pageable pageable);
    Page<Peca> findByNomeContainingIgnoreCaseAndArquivadoFalse(String nome, Pageable pageable);
    long countByArquivadoFalse();
}
