package br.com.criare3d.repository;

import br.com.criare3d.models.CustoPersonalizado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustoPersonalizadoRepository extends JpaRepository<CustoPersonalizado, UUID> {
    List<CustoPersonalizado> findByCalculo_IdOrderByDataRegistroDesc(UUID id);

    boolean existsByCalculo_Id(UUID id);
}
