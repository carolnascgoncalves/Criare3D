package br.com.criare3d.repository;
import br.com.criare3d.models.CustoLoja;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import java.time.LocalDate;
public interface CustoLojaRepository extends JpaRepository<CustoLoja, UUID> {
    List<CustoLoja> findAllByOrderByInicioDesc();
    List<CustoLoja> findByAtivoTrueAndInicioLessThanEqualOrderByInicioAsc(LocalDate hoje);
}
