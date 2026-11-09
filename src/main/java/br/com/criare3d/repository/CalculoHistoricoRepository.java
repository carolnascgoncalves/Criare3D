
package br.com.criare3d.repository;

import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.dto.FilamentoHistoricoUsoResponse;
import br.com.criare3d.dto.CalculoRecenteResponse;
import br.com.criare3d.dto.AjusteEstoqueCalculoResponse;
import br.com.criare3d.dto.HistoricoListaResponse;
import br.com.criare3d.dto.HistoricoExportacaoResponse;
import br.com.criare3d.dto.DashboardResumoProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.math.BigDecimal;

public interface CalculoHistoricoRepository extends JpaRepository<CalculoHistorico, UUID> {

    @Query(value = """
        SELECT
          COALESCE(SUM(h.unidades_hist) FILTER (WHERE h.data_hist >= :inicio), 0) AS "calculadas",
          COALESCE(SUM(h.unidades_hist) FILTER (WHERE h.produzido_hist = TRUE AND h.data_hist >= :inicio), 0) AS "produzidas",
          COALESCE(SUM(h.unidades_hist) FILTER (WHERE h.vendido_hist = TRUE
              AND h.data_venda_hist >= :inicio AND h.data_venda_hist < :fim), 0) AS "vendidas",
          COALESCE(SUM(h.lucro_liquido_hist * h.unidades_hist) FILTER (WHERE h.vendido_hist = TRUE
              AND h.data_venda_hist >= :inicio AND h.data_venda_hist < :fim), 0) AS "lucro",
          COALESCE(SUM(CASE WHEN h.minutos_maquina_lote_hist > 0 THEN h.minutos_maquina_lote_hist
              ELSE (h.temp_hist + COALESCE(h.tempo_aquecimento_hist, 0)) * h.fornadas_hist END)
              FILTER (WHERE h.vendido_hist = TRUE AND h.data_venda_hist >= :inicio AND h.data_venda_hist < :fim), 0) AS "minutosVendidos",
          COALESCE(SUM(CASE WHEN h.minutos_maquina_lote_hist > 0 THEN h.minutos_maquina_lote_hist
              ELSE (h.temp_hist + COALESCE(h.tempo_aquecimento_hist, 0)) * h.fornadas_hist END)
              FILTER (WHERE h.produzido_hist = TRUE AND h.data_hist >= :inicio), 0) AS "minutosProduzidos",
          COALESCE(SUM(h.gramas_totais_lote_hist)
              FILTER (WHERE h.produzido_hist = TRUE AND h.data_hist >= :inicio), 0) AS "gramasFilamento"
        FROM calculo_historico h
        WHERE h.tenant_id = :tenantId
          AND (h.data_hist >= :inicio
               OR (h.vendido_hist = TRUE
                   AND h.data_venda_hist >= :inicio
                   AND h.data_venda_hist < :fim))
        """, nativeQuery = true)
    DashboardResumoProjection resumirDashboard(@Param("tenantId") UUID tenantId,
                                                @Param("inicio") LocalDateTime inicio,
                                                @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT item.filamento.id, COUNT(DISTINCT item.calculoHistorico.id)
        FROM ItemCalculoHistorico item
        WHERE item.filamento IS NOT NULL
        GROUP BY item.filamento.id
        """)
    List<Object[]> contarUsosDeFilamentosEmItens();

    @Query("""
        SELECT h.filamento.id, COUNT(h)
        FROM CalculoHistorico h
        WHERE h.filamento IS NOT NULL
          AND h.itensFilamento IS EMPTY
        GROUP BY h.filamento.id
        """)
    List<Object[]> contarUsosDeFilamentosSimples();

    @Query("""
        SELECT DISTINCT CASE
            WHEN item.filamento.id IN :filamentoIds THEN item.filamento.id
            ELSE h.filamento.id
        END
        FROM CalculoHistorico h
        LEFT JOIN h.itensFilamento item
        WHERE h.produzido = true
          AND (h.filamento.id IN :filamentoIds OR item.filamento.id IN :filamentoIds)
        """)
    List<UUID> buscarIdsFilamentoComHistoricoProduzido(
            @Param("filamentoIds") List<UUID> filamentoIds
    );

    @Query("""
        SELECT DISTINCT CASE
            WHEN item.filamento.id IN :filamentoIds THEN item.filamento.id
            ELSE h.filamento.id
        END
        FROM CalculoHistorico h
        LEFT JOIN h.itensFilamento item
        WHERE (h.filamento.id IN :filamentoIds OR item.filamento.id IN :filamentoIds)
        """)
    List<UUID> buscarIdsFilamentoComHistorico(
            @Param("filamentoIds") List<UUID> filamentoIds
    );

    @Query("""
        SELECT DISTINCT h.peca.id FROM CalculoHistorico h
        WHERE h.peca.id IN :ids
        """)
    List<UUID> buscarIdsPecaComHistorico(@Param("ids") List<UUID> ids);

    @Query("""
        SELECT DISTINCT h.impressora.id FROM CalculoHistorico h
        WHERE h.impressora.id IN :ids
        """)
    List<UUID> buscarIdsImpressoraComHistorico(@Param("ids") List<UUID> ids);

    @Query("""
        SELECT DISTINCT h.embalagem.id FROM CalculoHistorico h
        WHERE h.embalagem.id IN :ids
        """)
    List<UUID> buscarIdsEmbalagemComHistorico(@Param("ids") List<UUID> ids);

    @Query("""
        SELECT new br.com.criare3d.dto.CalculoRecenteResponse(
            h.id, h.nomePeca, h.custoTotal, h.precoSugerido,
            h.plataformaVenda, h.produzido, h.vendido, h.unidades
        )
        FROM CalculoHistorico h
        ORDER BY h.dataCalculo DESC
        """)
    List<CalculoRecenteResponse> buscarCalculosRecentes(
            Pageable pageable);

    @Query("""
        SELECT new br.com.criare3d.dto.HistoricoExportacaoResponse(
            h.nomePeca, h.dataCalculo, h.plataformaVenda, h.custoTotal,
            COALESCE(h.precoVendaPlataforma, h.precoSugerido), h.lucroLiquido,
            h.produzido, h.vendido, h.unidades, h.fornadas
        )
        FROM CalculoHistorico h
        ORDER BY h.dataCalculo DESC
        """)
    List<HistoricoExportacaoResponse> buscarHistoricoParaExportacao();

    @Query("SELECT COALESCE(SUM(h.unidades), 0) FROM CalculoHistorico h WHERE h.produzido = true")
    long contarUnidadesProduzidas();

    @Query("SELECT COALESCE(SUM(h.unidades), 0) FROM CalculoHistorico h WHERE h.vendido = true")
    long contarUnidadesVendidas();

    @Query("""
        SELECT COALESCE(SUM(h.lucroLiquido * h.unidades), 0)
        FROM CalculoHistorico h
        WHERE h.vendido = true
          AND h.dataVenda >= :inicio AND h.dataVenda < :fim
          AND h.lucroLiquido IS NOT NULL
        """)
    BigDecimal somarLucroVendidoEntre(@Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT YEAR(h.dataVenda), MONTH(h.dataVenda), DAY(h.dataVenda),
               COALESCE(SUM(h.lucroLiquido * h.unidades), 0)
        FROM CalculoHistorico h
        WHERE h.vendido = true
          AND h.dataVenda >= :inicio AND h.dataVenda < :fim
          AND h.lucroLiquido IS NOT NULL
        GROUP BY YEAR(h.dataVenda), MONTH(h.dataVenda), DAY(h.dataVenda)
        ORDER BY YEAR(h.dataVenda), MONTH(h.dataVenda), DAY(h.dataVenda)
        """)
    List<Object[]> somarLucroVendidoPorDia(@Param("inicio") LocalDateTime inicio,
                                          @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT MIN(h.dataVenda)
        FROM CalculoHistorico h
        WHERE h.vendido = true
          AND h.lucroLiquido IS NOT NULL
        """)
    LocalDateTime primeiraVendaComLucro();

    @Query("""
        SELECT COALESCE(h.plataformaVenda, 'NENHUMA'), COALESCE(SUM(h.lucroLiquido * h.unidades), 0), COALESCE(SUM(h.unidades), 0)
        FROM CalculoHistorico h
        WHERE h.vendido = true
          AND h.dataVenda >= :inicio
        GROUP BY COALESCE(h.plataformaVenda, 'NENHUMA')
        """)
    List<Object[]> somarLucroPorPlataformaDesde(@Param("inicio") LocalDateTime inicio);

    @Query("""
        SELECT YEAR(h.dataVenda), MONTH(h.dataVenda), COALESCE(SUM(h.lucroLiquido * h.unidades), 0)
        FROM CalculoHistorico h
        WHERE h.vendido = true
          AND h.dataVenda >= :inicio AND h.dataVenda < :fim
          AND h.lucroLiquido IS NOT NULL
        GROUP BY YEAR(h.dataVenda), MONTH(h.dataVenda)
        ORDER BY YEAR(h.dataVenda), MONTH(h.dataVenda)
        """)
    List<Object[]> somarLucroVendidoPorMes(@Param("inicio") LocalDateTime inicio,
                                           @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT COALESCE(SUM((h.custoFilamento + h.custoPurga) * h.unidades), 0), COALESCE(SUM(h.custoEnergia * h.unidades), 0),
               COALESCE(SUM(h.custoEmbalagem * h.unidades), 0), COALESCE(SUM(h.custoMaoObra * h.unidades), 0),
               COALESCE(SUM(h.custoDepreciacao * h.unidades), 0), COALESCE(SUM(h.custoManutencao * h.unidades), 0),
               COALESCE(SUM(h.custoFalha * h.unidades), 0)
        FROM CalculoHistorico h
        WHERE h.produzido = true AND h.dataCalculo >= :inicio
        """)
    List<Object[]> somarComposicaoCustosProduzidosDesde(@Param("inicio") LocalDateTime inicio);

    @Query("""
        SELECT h.nomeImpressora, COALESCE(SUM(CASE WHEN h.minutosMaquinaLote > 0 THEN h.minutosMaquinaLote
            ELSE (h.tempoImpressaoMinutos + h.tempoAquecimentoMinutos) * h.fornadas END), 0), COALESCE(SUM(h.unidades), 0)
        FROM CalculoHistorico h
        WHERE h.produzido = true AND h.dataCalculo >= :inicio
        GROUP BY h.nomeImpressora
        ORDER BY SUM(CASE WHEN h.minutosMaquinaLote > 0 THEN h.minutosMaquinaLote
            ELSE (h.tempoImpressaoMinutos + h.tempoAquecimentoMinutos) * h.fornadas END) DESC
        """)
    List<Object[]> somarHorasPorImpressoraDesde(@Param("inicio") LocalDateTime inicio);

    @Query(value = """
    SELECT new br.com.criare3d.dto.HistoricoListaResponse(
        h.id, h.nomePeca, h.dataCalculo, h.plataformaVenda,
        h.precoVendaPlataforma, h.precoSugerido, h.produzido, h.vendido,
        h.temFoto,
        h.unidades
    )
    FROM CalculoHistorico h
    WHERE (:nome = '' OR LOWER(h.nomePeca) LIKE LOWER(CONCAT(CONCAT('%', :nome), '%')))
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
    WHERE (:nome = '' OR LOWER(h.nomePeca) LIKE LOWER(CONCAT(CONCAT('%', :nome), '%')))
      AND (:filtrarData = false OR (h.dataCalculo >= :inicio AND h.dataCalculo < :fim))
      AND (
          :plataforma = ''
          OR (:plataforma = 'SEM_PLATAFORMA' AND (h.plataformaVenda IS NULL OR h.plataformaVenda = 'NENHUMA'))
          OR h.plataformaVenda = :plataforma
      )
      AND (:filtrarProduzido = false OR h.produzido = :valorProduzido)
      AND (:filtrarVendido = false OR h.vendido = :valorVendido)
    """)
    Page<HistoricoListaResponse> buscarHistoricoFiltrado(
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

    long countByEmbalagem_Id(UUID embalagemId);

    long countByImpressora_Id(UUID impressoraId);

    @Query("""
    SELECT COALESCE(SUM(CASE WHEN c.minutosMaquinaLote > 0 THEN c.minutosMaquinaLote
        ELSE (c.tempoImpressaoMinutos + c.tempoAquecimentoMinutos) * c.fornadas END), 0)
    FROM CalculoHistorico c
    WHERE c.impressora.id = :impressoraId
      AND c.produzido = true
    """)
    Long somarMinutosPorImpressoraEUsuario(@Param("impressoraId") UUID impressoraId);

    long countByPeca_Id(UUID pecaId);

    @Query("""
        SELECT DISTINCT c
        FROM CalculoHistorico c
        LEFT JOIN FETCH c.itensFilamento item
        LEFT JOIN FETCH item.filamento
        WHERE c.id = :id
        """)
    Optional<CalculoHistorico> buscarPorIdComItens(@Param("id") UUID id);

    @Query("""
        SELECT COUNT(DISTINCT h.id)
        FROM CalculoHistorico h
        LEFT JOIN h.itensFilamento item
        WHERE (h.filamento.id = :filamentoId OR item.filamento.id = :filamentoId)
        """)
    long contarUsosDosItensPorFilamento(@Param("filamentoId") UUID filamentoId);

    @Query("""
        SELECT new br.com.criare3d.dto.FilamentoHistoricoUsoResponse(
            h.id,
            h.nomePeca,
            h.dataCalculo,
            COALESCE(SUM(item.quantidadeFilamentoGramas + item.purgaGramas), h.quantidadeFilamentoGramas) * h.unidades,
            COALESCE(item.corFilamento, h.filamento.cor),
            COALESCE(item.codigoCor, h.filamento.codigoCor),
            COALESCE(item.pesoRoloGramas, h.pesoRoloGramas),
            h.vendido
        )
        FROM CalculoHistorico h
        LEFT JOIN h.itensFilamento item ON item.filamento.id = :filamentoId
        WHERE h.produzido = true
          AND (item.id IS NOT NULL OR h.filamento.id = :filamentoId)
        GROUP BY h.id, h.nomePeca, h.dataCalculo, h.vendido, h.unidades, h.quantidadeFilamentoGramas,
                 item.corFilamento, item.codigoCor, item.pesoRoloGramas,
                 h.filamento.cor, h.filamento.codigoCor, h.pesoRoloGramas
        ORDER BY h.dataCalculo DESC
        """)
    List<FilamentoHistoricoUsoResponse> buscarUsosDoFilamento(@Param("filamentoId") UUID filamentoId);

    Optional<CalculoHistorico> findById(UUID id);

    boolean existsById(UUID id);

    @Query("""
        SELECT new br.com.criare3d.dto.AjusteEstoqueCalculoResponse(
            h.id, h.produzido, h.vendido, item.filamento.id,
            (item.quantidadeFilamentoGramas + item.purgaGramas) * h.unidades, h.filamento.id, h.quantidadeFilamentoGramas * h.unidades
        )
        FROM CalculoHistorico h
        LEFT JOIN h.itensFilamento item
        WHERE h.id = :id
        """)
    List<AjusteEstoqueCalculoResponse> buscarDadosParaAtualizarStatus(@Param("id") UUID id);

    @Query("""
        SELECT new br.com.criare3d.dto.AjusteEstoqueCalculoResponse(
            h.id, h.produzido, h.vendido, item.filamento.id,
            (item.quantidadeFilamentoGramas + item.purgaGramas) * h.unidades, h.filamento.id, h.quantidadeFilamentoGramas * h.unidades
        )
        FROM CalculoHistorico h
        LEFT JOIN h.itensFilamento item
        WHERE h.id IN :ids
        """)
    List<AjusteEstoqueCalculoResponse> buscarDadosParaExclusaoEmLote(@Param("ids") Collection<UUID> ids);

    @Modifying
    @Query("""
        UPDATE CalculoHistorico h
        SET h.produzido = :produzido, h.vendido = :vendido
        WHERE h.id = :id
        """)
    int atualizarStatus(@Param("id") UUID id,
                        @Param("produzido") boolean produzido, @Param("vendido") boolean vendido);

    @Modifying
    @Query("""
        UPDATE CalculoHistorico h
        SET h.dataVenda = :dataVenda
        WHERE h.id = :id
        """)
    int atualizarDataVenda(@Param("id") UUID id, @Param("dataVenda") LocalDateTime dataVenda);
}
