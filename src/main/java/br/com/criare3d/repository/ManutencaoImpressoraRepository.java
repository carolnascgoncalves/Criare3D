package br.com.criare3d.repository;

import br.com.criare3d.models.ManutencaoImpressora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManutencaoImpressoraRepository extends JpaRepository<ManutencaoImpressora, UUID> {

    List<ManutencaoImpressora> findAllByImpressora_IdOrderByDataDescIdDesc(UUID impressoraId);

    Optional<ManutencaoImpressora> findByIdAndImpressora_Id(UUID id, UUID impressoraId);

    long countByImpressora_Id(UUID impressoraId);

    @Query("""
        SELECT COALESCE(SUM(m.valor), 0)
        FROM ManutencaoImpressora m
        WHERE m.impressora.id = :impressoraId
          AND m.impressora.usuario.id = :usuarioId
        """)
    BigDecimal somarValoresPorImpressoraEUsuario(
            @Param("impressoraId") UUID impressoraId,
            @Param("usuarioId") UUID usuarioId
    );
}
