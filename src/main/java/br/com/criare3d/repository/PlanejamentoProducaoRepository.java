package br.com.criare3d.repository;
import br.com.criare3d.models.PlanejamentoProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;
public interface PlanejamentoProducaoRepository extends JpaRepository<PlanejamentoProducao,UUID> {
 Page<PlanejamentoProducao> findByCanceladoFalseAndConcluidoFalseOrderByInicioAsc(Pageable pagina);
 Optional<PlanejamentoProducao> findFirstByImpressora_IdAndCanceladoFalseAndConcluidoFalseOrderByFimDesc(UUID id);
 Page<PlanejamentoProducao> findByCanceladoFalseAndConcluidoTrueOrderByInicioDesc(Pageable pagina);
 @org.springframework.data.jpa.repository.Modifying
 @org.springframework.data.jpa.repository.Query("UPDATE PlanejamentoProducao p SET p.concluido = true WHERE p.calculo.id = :id AND p.cancelado = false")
 int concluirPorCalculo(@org.springframework.data.repository.query.Param("id") UUID id);
 boolean existsByCalculo_Id(UUID id);
}
