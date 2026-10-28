
package br.com.criare3d.repository;

import br.com.criare3d.models.CalculoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CalculoHistoricoRepository extends JpaRepository<CalculoHistorico, UUID> {
    long countByFilamento_Id(UUID filamentoId);

    long countByEmbalagem_Id(UUID embalagemId);

    long countByImpressora_Id(UUID impressoraId);

    long countByPeca_Id(UUID pecaId);

    @Query("""
    SELECT DISTINCT c
    FROM CalculoHistorico c
    LEFT JOIN FETCH c.itensFilamento
    WHERE c.id = :id
    """)
    Optional<CalculoHistorico> buscarPorIdComItens(@Param("id") UUID id);

    @Query("""
    SELECT COUNT(i)
    FROM ItemCalculoHistorico i
    WHERE i.filamento.id = :filamentoId
    """)
    long contarUsosDosItensPorFilamento(@Param("filamentoId") UUID filamentoId);
}
