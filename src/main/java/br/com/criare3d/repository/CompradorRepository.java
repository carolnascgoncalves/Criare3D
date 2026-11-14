package br.com.criare3d.repository;
import br.com.criare3d.models.Comprador;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
public interface CompradorRepository extends JpaRepository<Comprador,UUID> {
 @Query("select c from Comprador c where c.arquivado=:arquivados and (lower(c.nome) like lower(concat('%',:busca,'%')) or lower(c.email) like lower(concat('%',:busca,'%')) or c.telefone like concat('%',:busca,'%'))")
 Page<Comprador> buscar(@Param("busca") String busca,@Param("arquivados") boolean arquivados,Pageable pagina);
}
