package br.com.criare3d.repository;

import br.com.criare3d.models.PecaArquivoPlaca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PecaArquivoPlacaRepository extends JpaRepository<PecaArquivoPlaca, UUID> {
    @Query("select p.pecaId as pecaId, p.numero as numero from PecaArquivoPlaca p where p.pecaId in :pecaIds")
    List<PecaArquivoPlacaReferencia> listarReferenciasPorPecaIds(@Param("pecaIds") Collection<UUID> pecaIds);

    List<PecaArquivoPlaca> findAllByPecaIdOrderByNumero(UUID pecaId);

    Optional<PecaArquivoPlaca> findByPecaIdAndNumero(UUID pecaId, int numero);

    void deleteAllByPecaId(UUID pecaId);
}
