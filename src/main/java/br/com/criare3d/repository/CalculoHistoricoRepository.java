package br.com.criare3d.repository;

import br.com.criare3d.models.CalculoHistorico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface CalculoHistoricoRepository extends JpaRepository<CalculoHistorico, UUID> {

    Page<CalculoHistorico> findAllByUsuario_Id(UUID usuarioId, Pageable pageable);

    Page<CalculoHistorico> findByNomePecaContainingIgnoreCaseAndUsuario_Id(
            String nomePeca, UUID usuarioId, Pageable pageable
    );

    Page<CalculoHistorico> findByDataCalculoGreaterThanEqualAndDataCalculoLessThanAndUsuario_Id(
            LocalDateTime inicio, LocalDateTime fim, UUID usuarioId, Pageable pageable
    );

    Page<CalculoHistorico> findByNomePecaContainingIgnoreCaseAndDataCalculoGreaterThanEqualAndDataCalculoLessThanAndUsuario_Id(
            String nomePeca, LocalDateTime inicio, LocalDateTime fim,
            UUID usuarioId, Pageable pageable
    );

    @Query(value = """
        SELECT h
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId
          AND (:nome = '' OR LOWER(h.nomePeca) LIKE LOWER(CONCAT(CONCAT('%', :nome), '%')))
          AND (:filtrarData = false OR (h.dataCalculo >= :inicio AND h.dataCalculo < :fim))
          AND (
              :plataforma = ''
              OR (:plataforma = 'SEM_PLATAFORMA' AND (h.plataformaVenda IS NULL OR h.plataformaVenda = 'NENHUMA'))
              OR h.plataformaVenda = :plataforma
          )
        """,
        countQuery = """
        SELECT COUNT(h)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId
          AND (:nome = '' OR LOWER(h.nomePeca) LIKE LOWER(CONCAT(CONCAT('%', :nome), '%')))
          AND (:filtrarData = false OR (h.dataCalculo >= :inicio AND h.dataCalculo < :fim))
          AND (
              :plataforma = ''
              OR (:plataforma = 'SEM_PLATAFORMA' AND (h.plataformaVenda IS NULL OR h.plataformaVenda = 'NENHUMA'))
              OR h.plataformaVenda = :plataforma
          )
        """)
    Page<CalculoHistorico> buscarHistoricoFiltrado(
            @Param("usuarioId") UUID usuarioId,
            @Param("nome") String nome,
            @Param("filtrarData") boolean filtrarData,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("plataforma") String plataforma,
            Pageable pageable
    );

    long countByEmbalagem_IdAndUsuario_Id(UUID embalagemId, UUID usuarioId);

    long countByImpressora_IdAndUsuario_Id(UUID impressoraId, UUID usuarioId);

    @Query("""
        SELECT COALESCE(SUM(c.tempoImpressaoMinutos), 0)
        FROM CalculoHistorico c
        WHERE c.impressora.id = :impressoraId
          AND c.usuario.id = :usuarioId
        """)
    Long somarMinutosPorImpressoraEUsuario(
            @Param("impressoraId") UUID impressoraId,
            @Param("usuarioId") UUID usuarioId
    );

    long countByPeca_IdAndUsuario_Id(UUID pecaId, UUID usuarioId);

    @Query("""
        SELECT DISTINCT c
        FROM CalculoHistorico c
        LEFT JOIN FETCH c.itensFilamento
        WHERE c.id = :id
        AND c.usuario.id = :usuarioId
        """)
    Optional<CalculoHistorico> buscarPorIdComItens(
            @Param("id") UUID id,
            @Param("usuarioId") UUID usuarioId
    );

    @Query("""
        SELECT COUNT(i)
        FROM ItemCalculoHistorico i
        WHERE i.filamento.id = :filamentoId
        AND i.calculoHistorico.usuario.id = :usuarioId
        """)
    long contarUsosDosItensPorFilamento(
            @Param("filamentoId") UUID filamentoId,
            @Param("usuarioId") UUID usuarioId
    );

    Optional<CalculoHistorico> findByIdAndUsuario_Id(UUID id, UUID usuarioId);

    boolean existsByIdAndUsuario_Id(UUID id, UUID usuarioId);
}
