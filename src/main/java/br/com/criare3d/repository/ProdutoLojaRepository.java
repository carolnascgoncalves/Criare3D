package br.com.criare3d.repository;
import br.com.criare3d.models.ProdutoLoja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
import java.util.*;
public interface ProdutoLojaRepository extends JpaRepository<ProdutoLoja,UUID> {
    Page<ProdutoLoja> findByArquivadoFalseOrderByNomeAsc(Pageable pagina);
    Page<ProdutoLoja> findAllByOrderByNomeAsc(Pageable pagina);
}
