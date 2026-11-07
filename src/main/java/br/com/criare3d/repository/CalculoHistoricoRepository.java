
package br.com.criare3d.repository;

import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.dto.FilamentoHistoricoUsoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;

public interface CalculoHistoricoRepository extends JpaRepository<CalculoHistorico, UUID> {

    @Query("""
        SELECT item.filamento.id, COUNT(DISTINCT item.calculoHistorico.id)
        FROM ItemCalculoHistorico item
        WHERE item.calculoHistorico.usuario.id = :usuarioId AND item.filamento IS NOT NULL
        GROUP BY item.filamento.id
        """)
    List<Object[]> contarUsosDeFilamentosEmItens(@Param("usuarioId") UUID usuarioId);

    @Query("""
        SELECT h.filamento.id, COUNT(h)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId AND h.filamento IS NOT NULL
          AND h.itensFilamento IS EMPTY
        GROUP BY h.filamento.id
        """)
    List<Object[]> contarUsosDeFilamentosSimples(@Param("usuarioId") UUID usuarioId);

    Page<CalculoHistorico> findAllByUsuario_Id(UUID usuarioId, Pageable pageable);

    long countByProduzidoTrueAndUsuario_Id(UUID usuarioId);

    long countByVendidoTrueAndUsuario_Id(UUID usuarioId);

    long countByUsuario_IdAndDataCalculoGreaterThanEqual(UUID usuarioId, LocalDateTime inicio);

    long countByProduzidoTrueAndUsuario_IdAndDataCalculoGreaterThanEqual(UUID usuarioId, LocalDateTime inicio);

    long countByVendidoTrueAndUsuario_IdAndDataCalculoGreaterThanEqual(UUID usuarioId, LocalDateTime inicio);

    @Query("""
        SELECT COALESCE(SUM(h.lucroLiquido), 0)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId AND h.vendido = true
          AND h.dataCalculo >= :inicio AND h.lucroLiquido IS NOT NULL
        """)
    BigDecimal somarLucroVendidoDesde(@Param("usuarioId") UUID usuarioId,
                                      @Param("inicio") LocalDateTime inicio);

    @Query("""
        SELECT COALESCE(SUM(h.tempoImpressaoMinutos), 0)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId AND h.vendido = true
          AND h.dataCalculo >= :inicio AND h.dataCalculo < :fim
          AND h.lucroLiquido IS NOT NULL
        """)
    Long somarMinutosDasVendasEntre(@Param("usuarioId") UUID usuarioId,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT COALESCE(SUM(h.lucroLiquido), 0)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId AND h.vendido = true
          AND h.dataCalculo >= :inicio AND h.dataCalculo < :fim
          AND h.lucroLiquido IS NOT NULL
        """)
    BigDecimal somarLucroVendidoEntre(@Param("usuarioId") UUID usuarioId,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT YEAR(h.dataCalculo), MONTH(h.dataCalculo), DAY(h.dataCalculo),
               COALESCE(SUM(h.lucroLiquido), 0)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId AND h.vendido = true
          AND h.dataCalculo >= :inicio AND h.dataCalculo < :fim
          AND h.lucroLiquido IS NOT NULL
        GROUP BY YEAR(h.dataCalculo), MONTH(h.dataCalculo), DAY(h.dataCalculo)
        ORDER BY YEAR(h.dataCalculo), MONTH(h.dataCalculo), DAY(h.dataCalculo)
        """)
    List<Object[]> somarLucroVendidoPorDia(@Param("usuarioId") UUID usuarioId,
                                          @Param("inicio") LocalDateTime inicio,
                                          @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT MIN(h.dataCalculo)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId AND h.vendido = true
          AND h.lucroLiquido IS NOT NULL
        """)
    LocalDateTime primeiraVendaComLucro(@Param("usuarioId") UUID usuarioId);

    @Query("""
        SELECT COALESCE(SUM(h.tempoImpressaoMinutos), 0)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId AND h.produzido = true
          AND h.dataCalculo >= :inicio
        """)
    Long somarMinutosProduzidosDesde(@Param("usuarioId") UUID usuarioId,
                                     @Param("inicio") LocalDateTime inicio);

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN item.id IS NOT NULL
            THEN item.quantidadeFilamentoGramas ELSE h.quantidadeFilamentoGramas END), 0)
        FROM CalculoHistorico h
        LEFT JOIN h.itensFilamento item
        WHERE h.usuario.id = :usuarioId AND h.produzido = true
          AND h.dataCalculo >= :inicio
        """)
    BigDecimal somarFilamentoProduzidoDesde(@Param("usuarioId") UUID usuarioId,
                                            @Param("inicio") LocalDateTime inicio);

    @Query("""
        SELECT COALESCE(h.plataformaVenda, 'NENHUMA'), COALESCE(SUM(h.lucroLiquido), 0), COUNT(h)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId AND h.vendido = true
          AND h.dataCalculo >= :inicio
        GROUP BY COALESCE(h.plataformaVenda, 'NENHUMA')
        """)
    List<Object[]> somarLucroPorPlataformaDesde(@Param("usuarioId") UUID usuarioId,
                                                @Param("inicio") LocalDateTime inicio);

    @Query("""
        SELECT YEAR(h.dataCalculo), MONTH(h.dataCalculo), COALESCE(SUM(h.lucroLiquido), 0)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId AND h.vendido = true
          AND h.dataCalculo >= :inicio AND h.dataCalculo < :fim
          AND h.lucroLiquido IS NOT NULL
        GROUP BY YEAR(h.dataCalculo), MONTH(h.dataCalculo)
        ORDER BY YEAR(h.dataCalculo), MONTH(h.dataCalculo)
        """)
    List<Object[]> somarLucroVendidoPorMes(@Param("usuarioId") UUID usuarioId,
                                           @Param("inicio") LocalDateTime inicio,
                                           @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT COALESCE(SUM(h.custoFilamento), 0), COALESCE(SUM(h.custoEnergia), 0),
               COALESCE(SUM(h.custoEmbalagem), 0), COALESCE(SUM(h.custoMaoObra), 0),
               COALESCE(SUM(h.custoDepreciacao), 0), COALESCE(SUM(h.custoManutencao), 0),
               COALESCE(SUM(h.custoFalha), 0)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId AND h.produzido = true AND h.dataCalculo >= :inicio
        """)
    List<Object[]> somarComposicaoCustosProduzidosDesde(@Param("usuarioId") UUID usuarioId,
                                                        @Param("inicio") LocalDateTime inicio);

    @Query("""
        SELECT h.nomeImpressora, COALESCE(SUM(h.tempoImpressaoMinutos), 0), COUNT(h)
        FROM CalculoHistorico h
        WHERE h.usuario.id = :usuarioId AND h.produzido = true AND h.dataCalculo >= :inicio
        GROUP BY h.nomeImpressora ORDER BY SUM(h.tempoImpressaoMinutos) DESC
        """)
    List<Object[]> somarHorasPorImpressoraDesde(@Param("usuarioId") UUID usuarioId,
                                                @Param("inicio") LocalDateTime inicio);

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
      AND (:filtrarProduzido = false OR h.produzido = :valorProduzido)
      AND (:filtrarVendido = false OR h.vendido = :valorVendido)
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
      AND (:filtrarProduzido = false OR h.produzido = :valorProduzido)
      AND (:filtrarVendido = false OR h.vendido = :valorVendido)
    """)
    Page<CalculoHistorico> buscarHistoricoFiltrado(
            @Param("usuarioId") UUID usuarioId,
            @Param("nome") String nome,
            @Param("filtrarData") boolean filtrarData,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("plataforma") String plataforma,
            @Param("filtrarProduzido") boolean filtrarProduzido,
            @Param("valorProduzido") boolean valorProduzido,
            @Param("filtrarVendido") boolean filtrarVendido,
            @Param("valorVendido") boolean valorVendido,
            Pageable pageable
    );

    long countByEmbalagem_IdAndUsuario_Id(UUID embalagemId, UUID usuarioId);

    long countByImpressora_IdAndUsuario_Id(UUID impressoraId, UUID usuarioId);

    @Query("""
    SELECT COALESCE(SUM(c.tempoImpressaoMinutos), 0)
    FROM CalculoHistorico c
    WHERE c.impressora.id = :impressoraId
      AND c.usuario.id = :usuarioId
      AND c.produzido = true
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
        SELECT COUNT(DISTINCT h.id)
        FROM CalculoHistorico h
        LEFT JOIN h.itensFilamento item
        WHERE h.usuario.id = :usuarioId
          AND (h.filamento.id = :filamentoId OR item.filamento.id = :filamentoId)
        """)
    long contarUsosDosItensPorFilamento(
            @Param("filamentoId") UUID filamentoId,
            @Param("usuarioId") UUID usuarioId
    );

    @Query("""
        SELECT new br.com.criare3d.dto.FilamentoHistoricoUsoResponse(
            h.id,
            h.nomePeca,
            h.dataCalculo,
            COALESCE(SUM(item.quantidadeFilamentoGramas), h.quantidadeFilamentoGramas),
            COALESCE(item.corFilamento, h.filamento.cor),
            COALESCE(item.codigoCor, h.filamento.codigoCor),
            COALESCE(item.pesoRoloGramas, h.pesoRoloGramas),
            h.vendido
        )
        FROM CalculoHistorico h
        LEFT JOIN h.itensFilamento item ON item.filamento.id = :filamentoId
        WHERE h.usuario.id = :usuarioId
          AND h.produzido = true
          AND (item.id IS NOT NULL OR h.filamento.id = :filamentoId)
        GROUP BY h.id, h.nomePeca, h.dataCalculo, h.vendido, h.quantidadeFilamentoGramas,
                 item.corFilamento, item.codigoCor, item.pesoRoloGramas,
                 h.filamento.cor, h.filamento.codigoCor, h.pesoRoloGramas
        ORDER BY h.dataCalculo DESC
        """)
    List<FilamentoHistoricoUsoResponse> buscarUsosDoFilamento(
            @Param("filamentoId") UUID filamentoId,
            @Param("usuarioId") UUID usuarioId
    );

    Optional<CalculoHistorico> findByIdAndUsuario_Id(UUID id, UUID usuarioId);

    boolean existsByIdAndUsuario_Id(UUID id, UUID usuarioId);
}
