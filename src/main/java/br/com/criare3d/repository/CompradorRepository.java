package br.com.criare3d.repository;
import br.com.criare3d.models.Comprador;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
public interface CompradorRepository extends JpaRepository<Comprador,UUID> {
    /** Telefone só com dígitos, para achar "11945414013" mesmo quando o cadastro tem "(11) 94541-4013". */
    String DIGITOS = "replace(replace(replace(replace(replace(c.telefone,' ',''),'-',''),'(',''),')',''),'+','')";

    @Query("select c from Comprador c where c.arquivado=:arquivados and (lower(c.nome) like lower(concat('%',:busca,'%')) or lower(c.email) like lower(concat('%',:busca,'%')) or c.telefone like concat('%',:busca,'%'))")
    Page<Comprador> buscar(@Param("busca") String busca,@Param("arquivados") boolean arquivados,Pageable pagina);

    @Query("select c from Comprador c where c.arquivado=:arquivados and (lower(c.nome) like lower(concat('%',:busca,'%')) or lower(c.email) like lower(concat('%',:busca,'%')) or c.telefone like concat('%',:busca,'%') or (:digitos<>'' and " + DIGITOS + " like concat('%',:digitos,'%')))")
    Page<Comprador> pesquisar(@Param("busca") String busca,@Param("digitos") String digitos,@Param("arquivados") boolean arquivados,Pageable pagina);

    @Query("select c from Comprador c where lower(c.email)=lower(:email)")
    List<Comprador> porEmail(@Param("email") String email);

    @Query("select c from Comprador c where " + DIGITOS + "=:digitos or " + DIGITOS + "=concat('55',:digitos)")
    List<Comprador> porTelefone(@Param("digitos") String digitos);

    long countByArquivado(boolean arquivado);

    @Query("select count(c) from Comprador c where c.arquivado=false and not exists (select p.id from PedidoOrcamento p where p.compradorId=c.id and p.arquivado=false and p.criadoEm>=:corte)")
    long semOrcamentoDesde(@Param("corte") LocalDateTime corte);
}
