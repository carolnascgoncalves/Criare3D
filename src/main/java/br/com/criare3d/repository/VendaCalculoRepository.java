package br.com.criare3d.repository;

import br.com.criare3d.models.VendaCalculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VendaCalculoRepository extends JpaRepository<VendaCalculo, UUID> {
    List<VendaCalculo> findByCalculo_IdOrderByDataVendaDesc(UUID calculoId);
    boolean existsByCalculo_Id(UUID calculoId);
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(v) > 0 FROM VendaCalculo v WHERE v.calculo.id = :id AND v.unidades > v.unidadesCanceladas")
    boolean existeVendaAtiva(@org.springframework.data.repository.query.Param("id") UUID id);
    void deleteByCalculo_Id(UUID calculoId);
}
