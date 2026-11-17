package br.com.criare3d.repository;

import br.com.criare3d.models.VendaProdutoLoja;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendaProdutoLojaRepository extends JpaRepository<VendaProdutoLoja, UUID> {
    Page<VendaProdutoLoja> findAllByOrderByDataVendaDesc(Pageable pagina);

    @Query("select v from VendaProdutoLoja v where lower(v.nomeProduto) like lower(concat('%',:busca,'%')) order by v.dataVenda desc,v.id")
    Page<VendaProdutoLoja> pesquisar(@Param("busca") String busca, Pageable pagina);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VendaProdutoLoja v where v.id=:id")
    Optional<VendaProdutoLoja> bloquear(@Param("id") UUID id);

    interface Resumo {
        Long getUnidades();

        BigDecimal getReceita();

        BigDecimal getLucro();

        Long getSemCusto();
    }

    @Query("""
        select coalesce(sum(v.quantidade-v.canceladas),0) as unidades,
          coalesce(sum(v.precoUnitario*(v.quantidade-v.canceladas)),0) as receita,
          coalesce(sum(((v.precoUnitario-v.custoUnitario)*(v.quantidade-v.canceladas)-round(v.custoEmbalagem*coalesce(v.quantidadeEmbalagens,v.quantidade)*(v.quantidade-v.canceladas)/v.quantidade,2))),0) as lucro,
          coalesce(sum(case when v.custoUnitario is null then v.quantidade-v.canceladas else 0 end),0) as semCusto
        from VendaProdutoLoja v where v.dataVenda>=:inicio and v.dataVenda<:fim
        """)
    Resumo resumir(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("""
        select year(v.dataVenda),month(v.dataVenda),day(v.dataVenda),sum(((v.precoUnitario-v.custoUnitario)*(v.quantidade-v.canceladas)-round(v.custoEmbalagem*coalesce(v.quantidadeEmbalagens,v.quantidade)*(v.quantidade-v.canceladas)/v.quantidade,2)))
        from VendaProdutoLoja v where v.dataVenda>=:inicio and v.dataVenda<:fim and v.custoUnitario is not null and v.quantidade>v.canceladas
        group by year(v.dataVenda),month(v.dataVenda),day(v.dataVenda)
        """)
    List<Object[]> lucroPorDia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("""
        select year(v.dataVenda),month(v.dataVenda),sum(((v.precoUnitario-v.custoUnitario)*(v.quantidade-v.canceladas)-round(v.custoEmbalagem*coalesce(v.quantidadeEmbalagens,v.quantidade)*(v.quantidade-v.canceladas)/v.quantidade,2)))
        from VendaProdutoLoja v where v.dataVenda>=:inicio and v.dataVenda<:fim and v.custoUnitario is not null and v.quantidade>v.canceladas
        group by year(v.dataVenda),month(v.dataVenda)
        """)
    List<Object[]> lucroPorMes(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("select min(v.dataVenda) from VendaProdutoLoja v where v.custoUnitario is not null and v.quantidade>v.canceladas")
    LocalDateTime primeiraVendaComLucro();
}
