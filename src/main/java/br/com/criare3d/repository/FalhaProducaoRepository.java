package br.com.criare3d.repository;
import br.com.criare3d.models.FalhaProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface FalhaProducaoRepository extends JpaRepository<FalhaProducao,UUID> {
 List<FalhaProducao> findByCalculo_IdOrderByDataRegistroDesc(UUID id);
 boolean existsByCalculo_IdAndCanceladaFalse(UUID id);
 long countByFilamento_Id(UUID id);
 @org.springframework.data.jpa.repository.Query("SELECT DISTINCT f.filamento.id FROM FalhaProducao f WHERE f.filamento.id IN :ids")
 List<UUID> buscarFilamentosComRegistro(@org.springframework.data.repository.query.Param("ids") Collection<UUID> ids);
}
