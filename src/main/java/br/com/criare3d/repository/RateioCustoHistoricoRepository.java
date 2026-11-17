package br.com.criare3d.repository;

import br.com.criare3d.models.RateioCustoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RateioCustoHistoricoRepository extends JpaRepository<RateioCustoHistorico, UUID> {
    @Query("select coalesce(sum(r.valorUnitario * r.calculo.unidadesVendidas), 0) from RateioCustoHistorico r where r.custoLoja.id = :id")
    BigDecimal recuperado(@Param("id") UUID id);

    @Query("select r from RateioCustoHistorico r join fetch r.calculo where r.custoLoja.id = :id order by r.calculo.dataCalculo desc")
    List<RateioCustoHistorico> listarPecas(@Param("id") UUID id, org.springframework.data.domain.Pageable pagina);
}
