package br.com.criare3d.repository;

import br.com.criare3d.models.VendaCalculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendaCalculoRepository extends JpaRepository<VendaCalculo, UUID> {
    List<VendaCalculo> findByCalculo_IdOrderByDataVendaDesc(UUID calculoId);
    void deleteByCalculo_Id(UUID calculoId);
}
