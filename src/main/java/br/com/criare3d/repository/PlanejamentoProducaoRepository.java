package br.com.criare3d.repository;
import br.com.criare3d.models.PlanejamentoProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;
public interface PlanejamentoProducaoRepository extends JpaRepository<PlanejamentoProducao,UUID> {
 @org.springframework.data.jpa.repository.Query("""
  SELECT p FROM PlanejamentoProducao p
  WHERE p.cancelado=false AND p.concluido=false
   AND COALESCE(p.calculo.unidadesVendidas,0)<p.calculo.unidades
   AND p.impressora.tenantId=p.tenantId AND p.calculo.tenantId=p.tenantId
  ORDER BY p.inicio ASC
 """)
 Page<PlanejamentoProducao> findByCanceladoFalseAndConcluidoFalseOrderByInicioAsc(Pageable pagina);
 @org.springframework.data.jpa.repository.Query("""
  SELECT p FROM PlanejamentoProducao p
  WHERE p.impressora.id=:id AND p.cancelado=false AND p.concluido=false AND p.fim IS NOT NULL
   AND COALESCE(p.calculo.produzido,false)=false
   AND COALESCE(p.calculo.unidadesVendidas,0)<p.calculo.unidades
   AND p.impressora.tenantId=p.tenantId AND p.calculo.tenantId=p.tenantId
  ORDER BY p.fim DESC
 """)
 List<PlanejamentoProducao> buscarPendentesDaImpressora(@org.springframework.data.repository.query.Param("id") UUID id,Pageable pagina);
 default Optional<PlanejamentoProducao> findFirstByImpressora_IdAndCanceladoFalseAndConcluidoFalseOrderByFimDesc(UUID id){return buscarPendentesDaImpressora(id,PageRequest.of(0,1)).stream().findFirst();}
 @org.springframework.data.jpa.repository.Query("""
  SELECT p FROM PlanejamentoProducao p WHERE p.cancelado=false
   AND (p.concluido=true
        OR COALESCE(p.calculo.unidadesVendidas,0)>=p.calculo.unidades)
   AND p.impressora.tenantId=p.tenantId AND p.calculo.tenantId=p.tenantId
  ORDER BY p.inicio DESC
 """)
 Page<PlanejamentoProducao> findByCanceladoFalseAndConcluidoTrueOrderByInicioDesc(Pageable pagina);
 @org.springframework.data.jpa.repository.Modifying
 @org.springframework.data.jpa.repository.Query("UPDATE PlanejamentoProducao p SET p.concluido = true WHERE p.calculo.id = :id AND p.cancelado = false")
 int concluirPorCalculo(@org.springframework.data.repository.query.Param("id") UUID id);
 boolean existsByCalculo_Id(UUID id);
}
