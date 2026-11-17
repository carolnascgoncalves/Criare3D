package br.com.criare3d.repository;

import br.com.criare3d.models.Movimentacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, UUID> {
    /**
     * Total que entrou e saiu no filtro atual, separado por unidade (gramas e unidades não se somam).
     */
    interface ResumoMedida {
        String getMedida();

        BigDecimal getEntrou();

        BigDecimal getSaiu();

        BigDecimal getPecasVendidas();
    }

    interface SaldoAbertura {
        long getTotal();

        BigDecimal getGramas();
    }

    String FILTRO = """
        (:tudo=true or (m.data>=:inicio and m.data<:fim))
        and (:filamento is null or m.filamentoId=:filamento)
        and (:tipo='' or m.tipo=:tipo)
        and (:saldo=true or m.tipo<>'SALDO_INICIAL')
        and (:direcao='' or (:direcao='entrada' and m.quantidade>0) or (:direcao='saida' and m.quantidade<0))
        and (:busca='' or lower(m.descricao) like lower(concat('%',:busca,'%')))""";

    @Query("select m from Movimentacao m where " + FILTRO)
    Page<Movimentacao> extrato(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
                              @Param("tipo") String tipo, @Param("direcao") String direcao,
                              @Param("saldo") boolean saldo, @Param("busca") String busca,
                              @Param("filamento") UUID filamento, @Param("tudo") boolean tudo, Pageable pagina);

    @Query("""
        select m.medida as medida, sum(case when m.quantidade>0 then m.quantidade end) as entrou,
        sum(case when m.quantidade<0 then m.quantidade end) as saiu,
        sum(case when m.tipo in ('VENDA_3D','VENDA_PRODUTO','CANCELAR_VENDA_3D','CANCELAR_VENDA_PRODUTO')
            then -m.quantidade else 0 end) as pecasVendidas
        from Movimentacao m where
        """ + FILTRO + " group by m.medida")
    List<ResumoMedida> resumo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
                             @Param("tipo") String tipo, @Param("direcao") String direcao,
                             @Param("saldo") boolean saldo, @Param("busca") String busca,
                             @Param("filamento") UUID filamento, @Param("tudo") boolean tudo);

    @Query("""
        select count(m) as total, sum(m.quantidade) as gramas from Movimentacao m
        where m.tipo='SALDO_INICIAL' and m.medida='g'
        and (:tudo=true or (m.data>=:inicio and m.data<:fim))
        and (:filamento is null or m.filamentoId=:filamento)
        and (:busca='' or lower(m.descricao) like lower(concat('%',:busca,'%')))
        """)
    SaldoAbertura saldoAbertura(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim,
                              @Param("filamento") UUID filamento, @Param("busca") String busca,
                              @Param("tudo") boolean tudo);
}
