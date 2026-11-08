package br.com.criare3d.repository;

import br.com.criare3d.models.CalculoFoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface CalculoFotoRepository extends JpaRepository<CalculoFoto, UUID> {
    @Query("select f.miniatura from CalculoFoto f where f.calculoId = :id")
    Optional<byte[]> buscarMiniatura(@Param("id") UUID calculoId);
}
