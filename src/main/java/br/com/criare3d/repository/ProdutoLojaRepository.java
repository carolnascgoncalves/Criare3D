package br.com.criare3d.repository;

import br.com.criare3d.models.ProdutoLoja;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProdutoLojaRepository extends JpaRepository<ProdutoLoja, UUID> {
    Page<ProdutoLoja> findByArquivadoFalseOrderByNomeAsc(Pageable pagina);

    Page<ProdutoLoja> findByArquivadoTrueOrderByNomeAsc(Pageable pagina);

    Page<ProdutoLoja> findAllByOrderByNomeAsc(Pageable pagina);
}
