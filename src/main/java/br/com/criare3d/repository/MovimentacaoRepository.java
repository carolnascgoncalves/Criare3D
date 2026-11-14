package br.com.criare3d.repository;
import br.com.criare3d.models.Movimentacao;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
public interface MovimentacaoRepository extends JpaRepository<Movimentacao,UUID> {
    /** Total que entrou e saiu no filtro atual, separado por unidade (gramas e unidades não se somam). */
    interface ResumoMedida { String getMedida(); BigDecimal getEntrou(); BigDecimal getSaiu(); }
    interface SaldoAbertura { long getTotal(); BigDecimal getGramas(); }

    String FILTRO = """
            m.data>=:inicio and m.data<:fim
            and (:filamento is null or m.filamentoId=:filamento)
            and (:tipo='' or m.tipo=:tipo)
            and (:saldo=true or m.tipo<>'SALDO_INICIAL')
            and (:direcao='' or (:direcao='entrada' and m.quantidade>0) or (:direcao='saida' and m.quantidade<0))
            and lower(m.descricao) like lower(concat('%',:busca,'%'))""";

    @Query("select m from Movimentacao m where m.data>=:inicio and m.data<:fim and (:tipo='' or m.tipo=:tipo) and lower(m.descricao) like lower(concat('%',:busca,'%'))")
    Page<Movimentacao> buscar(@Param("inicio")LocalDateTime inicio,@Param("fim")LocalDateTime fim,@Param("tipo")String tipo,@Param("busca")String busca,Pageable pagina);

    @Query("select m from Movimentacao m where " + FILTRO)
    Page<Movimentacao> extrato(@Param("inicio")LocalDateTime inicio,@Param("fim")LocalDateTime fim,@Param("tipo")String tipo,@Param("direcao")String direcao,@Param("saldo")boolean saldo,@Param("busca")String busca,@Param("filamento")UUID filamento,Pageable pagina);

    @Query("select m.medida as medida, sum(case when m.quantidade>0 then m.quantidade end) as entrou, sum(case when m.quantidade<0 then m.quantidade end) as saiu from Movimentacao m where " + FILTRO + " group by m.medida")
    List<ResumoMedida> resumo(@Param("inicio")LocalDateTime inicio,@Param("fim")LocalDateTime fim,@Param("tipo")String tipo,@Param("direcao")String direcao,@Param("saldo")boolean saldo,@Param("busca")String busca,@Param("filamento")UUID filamento);

    @Query("select count(m) as total, sum(m.quantidade) as gramas from Movimentacao m where m.tipo='SALDO_INICIAL' and m.medida='g' and m.data>=:inicio and m.data<:fim and (:filamento is null or m.filamentoId=:filamento) and lower(m.descricao) like lower(concat('%',:busca,'%'))")
    SaldoAbertura saldoAbertura(@Param("inicio")LocalDateTime inicio,@Param("fim")LocalDateTime fim,@Param("filamento")UUID filamento,@Param("busca")String busca);

    @Query("select min(m.data) from Movimentacao m")
    LocalDateTime primeiraData();
}
