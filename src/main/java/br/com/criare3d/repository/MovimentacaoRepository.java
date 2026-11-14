package br.com.criare3d.repository;
import br.com.criare3d.models.Movimentacao;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.UUID;
public interface MovimentacaoRepository extends JpaRepository<Movimentacao,UUID> {
 @Query("select m from Movimentacao m where m.data>=:inicio and m.data<:fim and (:tipo='' or m.tipo=:tipo) and lower(m.descricao) like lower(concat('%',:busca,'%'))")
 Page<Movimentacao> buscar(@Param("inicio")LocalDateTime inicio,@Param("fim")LocalDateTime fim,@Param("tipo")String tipo,@Param("busca")String busca,Pageable pagina);
}
