package br.com.criare3d.repository;

import br.com.criare3d.models.PedidoOrcamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoOrcamentoRepository extends JpaRepository<PedidoOrcamento, UUID> {
    @org.springframework.data.jpa.repository.Query("select p from PedidoOrcamento p where p.compradorId=:id and p.arquivado=:arquivado and (:status is null or p.status=:status) and (lower(p.titulo) like lower(concat('%',:busca,'%')) or lower(p.cliente) like lower(concat('%',:busca,'%'))) order by p.criadoEm desc")
    org.springframework.data.domain.Page<PedidoOrcamento> buscarDoComprador(@org.springframework.data.repository.query.Param("id") java.util.UUID id, @org.springframework.data.repository.query.Param("arquivado") boolean arquivado, @org.springframework.data.repository.query.Param("busca") String busca, @org.springframework.data.repository.query.Param("status") br.com.criare3d.models.enums.StatusPedido status, org.springframework.data.domain.Pageable pagina);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PedidoOrcamento p WHERE p.id=:id")
    Optional<PedidoOrcamento> bloquear(@Param("id") UUID id);

    @Query("""
        SELECT p FROM PedidoOrcamento p WHERE p.arquivado=:arquivados
        AND (:status IS NULL OR
          (p.status=:status AND (:status<>br.com.criare3d.models.enums.StatusPedido.APROVADO OR EXISTS
            (SELECT i.id FROM ItemPedido i LEFT JOIN i.loteProducao l WHERE i.pedido=p AND i.quantidade>i.usarEstoque AND (l.id IS NULL OR l.produzido IS NULL OR l.produzido=false))))
          OR (:status=br.com.criare3d.models.enums.StatusPedido.PRONTO AND p.status=br.com.criare3d.models.enums.StatusPedido.APROVADO AND NOT EXISTS
            (SELECT i.id FROM ItemPedido i LEFT JOIN i.loteProducao l WHERE i.pedido=p AND i.quantidade>i.usarEstoque AND (l.id IS NULL OR l.produzido IS NULL OR l.produzido=false))))
        AND (lower(p.titulo) LIKE lower(concat('%',:busca,'%')) OR lower(p.cliente) LIKE lower(concat('%',:busca,'%')))
        ORDER BY p.criadoEm DESC
        """)
    Page<PedidoOrcamento> buscar(@Param("busca") String busca, @Param("arquivados") boolean arquivados, @Param("status") br.com.criare3d.models.enums.StatusPedido status, Pageable pagina);

    /**
     * Quantos orçamentos cada cliente tem e quando foi o último (cancelados ficam de fora).
     */
    interface ResumoCliente {
        UUID getCompradorId();

        long getTotal();

        long getAbertos();

        java.time.LocalDateTime getUltimo();
    }

    @Query("""
        select p.compradorId as compradorId, count(p) as total, max(p.criadoEm) as ultimo,
        sum(case when p.status in (br.com.criare3d.models.enums.StatusPedido.ENVIADO,
            br.com.criare3d.models.enums.StatusPedido.APROVADO,
            br.com.criare3d.models.enums.StatusPedido.PRONTO) then 1 else 0 end) as abertos
        from PedidoOrcamento p where p.compradorId in :ids and p.arquivado=false
        and p.status<>br.com.criare3d.models.enums.StatusPedido.CANCELADO group by p.compradorId
        """)
    List<ResumoCliente> resumoPorComprador(@Param("ids") Collection<UUID> ids);
}
