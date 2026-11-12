package br.com.criare3d.repository;

import br.com.criare3d.models.Filamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FilamentoRepository extends
        JpaRepository<Filamento, UUID>,
        JpaSpecificationExecutor<Filamento> {

    List<Filamento> findByArquivadoFalseOrderByDataCadastroDescNomeAsc(Pageable pageable);

    @Query("""
        SELECT f FROM Filamento f
        WHERE f.arquivado = false AND f.estoqueGramas IS NOT NULL
          AND f.pesoRoloGramas IS NOT NULL
          AND f.pesoRoloGramas > 0
          AND f.estoqueGramas * 10 <= f.pesoRoloGramas
        ORDER BY f.estoqueGramas ASC
        """)
    List<Filamento> buscarComEstoqueCritico(Pageable pageable);

    Page<Filamento> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    List<Filamento> findAllByIdIn(Collection<UUID> ids);
    Page<Filamento> findByArquivadoFalse(Pageable pageable);
    Page<Filamento> findByArquivadoTrue(Pageable pageable);
    Page<Filamento> findByNomeContainingIgnoreCaseAndArquivadoFalse(String nome, Pageable pageable);
    long countByArquivadoFalse();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Filamento e WHERE e.id = :id")
    java.util.Optional<Filamento> bloquearPorId(@org.springframework.data.repository.query.Param("id") UUID id);
}
