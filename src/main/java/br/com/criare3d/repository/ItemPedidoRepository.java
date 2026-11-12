package br.com.criare3d.repository;
import br.com.criare3d.models.ItemPedido;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
public interface ItemPedidoRepository extends JpaRepository<ItemPedido,UUID> {
 @Query("SELECT COALESCE(SUM(CASE WHEN i.loteEstoque.id=:id THEN i.reservaEstoque ELSE 0 END),0)+COALESCE(SUM(CASE WHEN i.loteProducao.id=:id THEN i.reservaProducao ELSE 0 END),0) FROM ItemPedido i WHERE i.loteEstoque.id=:id OR i.loteProducao.id=:id")
 long reservadas(@Param("id") UUID id);
 long countByPecaId(UUID id);
 long countByImpressoraId(UUID id);
 boolean existsByPlanejamento_Id(UUID id);
 boolean existsByLoteProducao_Id(UUID id);
 @Query("SELECT i.planejamento.id, i.pedido.id FROM ItemPedido i WHERE i.planejamento.id IN :ids")
 java.util.List<Object[]> vinculosPlanejamento(@Param("ids") java.util.Collection<UUID> ids);
 @Modifying(flushAutomatically=true)
 @Query("UPDATE PlanejamentoProducao p SET p.concluido=true WHERE p.id IN (SELECT i.planejamento.id FROM ItemPedido i WHERE i.loteProducao.id=:id)")
 void concluirPlanejamentoPorLote(@Param("id") UUID id);
 @Query("SELECT COUNT(i)>0 FROM ItemPedido i WHERE i.loteEstoque.id=:id OR i.loteProducao.id=:id")
 boolean vinculado(@Param("id") UUID id);
}
