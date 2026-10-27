
package br.com.criare3d.repository;

import br.com.criare3d.models.CalculoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CalculoHistoricoRepository extends JpaRepository<CalculoHistorico, UUID> {
    long countByFilamento_Id(UUID filamentoId);

    long countByEmbalagem_Id(UUID embalagemId);

    long countByImpressora_Id(UUID impressoraId);

    long countByPeca_Id(UUID pecaId);
}
