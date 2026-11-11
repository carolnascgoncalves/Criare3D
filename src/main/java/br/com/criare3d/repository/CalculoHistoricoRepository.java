
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

    @Query("SELECT COALESCE(SUM(h.minutosMaquinaLote), 0) FROM CalculoHistorico h " +
            "WHERE h.produzido = true AND h.dataCalculo >= :inicio")
    Long somarMinutosMaquinaProduzidosDesde(@Param("inicio") LocalDateTime inicio);

    @Query("SELECT COALESCE(SUM(h.minutosMaquinaLote), 0) FROM CalculoHistorico h " +
            "WHERE h.produzido = true AND h.dataCalculo >= :inicio AND h.impressora.id = :impressoraId")
    Long somarMinutosMaquinaProduzidosDaImpressoraDesde(
            @Param("inicio") LocalDateTime inicio, @Param("impressoraId") UUID impressoraId);

    @Query(value = """
        SELECT
          COALESCE(SUM(h.unidades_hist) FILTER (WHERE h.data_hist >= :inicio), 0) AS "calculadas",
          COALESCE(SUM(h.unidades_hist) FILTER (WHERE h.produzido_hist = TRUE AND h.data_hist >= :inicio), 0) AS "produzidas",
          COALESCE((SELECT SUM(v.unidades) FROM venda_calculo v WHERE v.tenant_id = :tenantId
              AND v.data_venda >= :inicio AND v.data_venda < :fim), 0) AS "vendidas",
          COALESCE((SELECT SUM(ch.lucro_liquido_hist * v.unidades) FROM venda_calculo v
              JOIN calculo_historico ch ON ch.tenant_id = v.tenant_id AND ch.id = v.calculo_id
              WHERE v.tenant_id = :tenantId AND v.data_venda >= :inicio AND v.data_venda < :fim), 0) AS "lucro",
          COALESCE((SELECT SUM((CASE WHEN ch.minutos_maquina_lote_hist > 0 THEN ch.minutos_maquina_lote_hist
              ELSE (ch.temp_hist + COALESCE(ch.tempo_aquecimento_hist, 0)) * ch.fornadas_hist END)
              * v.unidades::numeric / NULLIF(ch.unidades_hist, 0)) FROM venda_calculo v
              JOIN calculo_historico ch ON ch.tenant_id = v.tenant_id AND ch.id = v.calculo_id
              WHERE v.tenant_id = :tenantId AND v.data_venda >= :inicio AND v.data_venda < :fim), 0) AS "minutosVendidos",
          COALESCE(SUM(CASE WHEN h.minutos_maquina_lote_hist > 0 THEN h.minutos_maquina_lote_hist
              ELSE (h.temp_hist + COALESCE(h.tempo_aquecimento_hist, 0)) * h.fornadas_hist END)
              FILTER (WHERE h.produzido_hist = TRUE AND h.data_hist >= :inicio), 0) AS "minutosProduzidos",
          COALESCE(SUM(h.gramas_totais_lote_hist)
              FILTER (WHERE h.produzido_hist = TRUE AND h.data_hist >= :inicio), 0) AS "gramasFilamento"
        FROM calculo_historico h
        WHERE h.tenant_id = :tenantId
          AND (h.data_hist >= :inicio OR EXISTS (SELECT 1 FROM venda_calculo v
               WHERE v.tenant_id = h.tenant_id AND v.calculo_id = h.id
                 AND v.data_venda >= :inicio AND v.data_venda < :fim))
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

    @Query("SELECT COALESCE(SUM(h.unidadesVendidas), 0) FROM CalculoHistorico h")
    long contarUnidadesVendidas();

    @Query("SELECT COALESCE(SUM(h.unidadesVendidas), 0) FROM CalculoHistorico h")
    long somarUnidadesVendidas();

    @Query("SELECT COALESCE(SUM(h.unidades - h.unidadesVendidas), 0) FROM CalculoHistorico h WHERE h.produzido = true")
    long contarUnidadesEmEstoque();

    @Query("SELECT COALESCE(SUM(h.unidades), 0) FROM CalculoHistorico h WHERE h.produzido = false AND h.vendido = false")
    long contarUnidadesParaProduzir();

    @Query("SELECT COALESCE(SUM(h.custoTotal * (h.unidades - h.unidadesVendidas)), 0) FROM CalculoHistorico h WHERE h.produzido = true AND h.unidadesVendidas < h.unidades")
    BigDecimal somarCustoUnidadesEmEstoque();

    @Query(value = "SELECT COALESCE(SUM(v.unidades), 0) FROM venda_calculo v WHERE v.tenant_id = :tenantId AND v.data_venda >= :inicio AND v.data_venda < :fim", nativeQuery = true)
    long contarUnidadesVendidasEntre(@Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim,
                                     @Param("tenantId") UUID tenantId);

    @Query(value = "SELECT COUNT(DISTINCT v.calculo_id) FROM venda_calculo v WHERE v.tenant_id = :tenantId AND v.data_venda >= :inicio AND v.data_venda < :fim", nativeQuery = true)
    long contarLotesVendidosEntre(@Param("inicio") LocalDateTime inicio,
                                  @Param("fim") LocalDateTime fim,
                                  @Param("tenantId") UUID tenantId);

    @Query("""
        SELECT COALESCE(SUM(h.lucroLiquido * v.unidades), 0)
        FROM VendaCalculo v JOIN v.calculo h
        WHERE v.dataVenda >= :inicio AND v.dataVenda < :fim
          AND h.lucroLiquido IS NOT NULL
        """)
    BigDecimal somarLucroVendidoEntre(@Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT YEAR(v.dataVenda), MONTH(v.dataVenda), DAY(v.dataVenda),
               COALESCE(SUM(h.lucroLiquido * v.unidades), 0)
        FROM VendaCalculo v JOIN v.calculo h
        WHERE v.dataVenda >= :inicio AND v.dataVenda < :fim AND h.lucroLiquido IS NOT NULL
        GROUP BY YEAR(v.dataVenda), MONTH(v.dataVenda), DAY(v.dataVenda)
        ORDER BY YEAR(v.dataVenda), MONTH(v.dataVenda), DAY(v.dataVenda)
        """)
    List<Object[]> somarLucroVendidoPorDia(@Param("inicio") LocalDateTime inicio,
                                          @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT MIN(v.dataVenda)
        FROM VendaCalculo v JOIN v.calculo h
        WHERE h.lucroLiquido IS NOT NULL
        """)
    LocalDateTime primeiraVendaComLucro();

    @Query("""
        SELECT COALESCE(h.plataformaVenda, 'NENHUMA'), COALESCE(SUM(h.lucroLiquido * v.unidades), 0), COALESCE(SUM(v.unidades), 0)
        FROM VendaCalculo v JOIN v.calculo h
        WHERE v.dataVenda >= :inicio
        GROUP BY COALESCE(h.plataformaVenda, 'NENHUMA')
        """)
    List<Object[]> somarLucroPorPlataformaDesde(@Param("inicio") LocalDateTime inicio);

    @Query(value = """
        WITH por_categoria AS (
            SELECT COALESCE(NULLIF(BTRIM(ch.categoria_peca_hist), ''), 'Sem categoria') AS categoria,
                   COALESCE(SUM(v.unidades), 0) AS unidades,
                   COALESCE(SUM(ch.lucro_liquido_hist * v.unidades), 0) AS lucro
            FROM venda_calculo v
            JOIN calculo_historico ch ON ch.tenant_id = v.tenant_id AND ch.id = v.calculo_id
            WHERE v.tenant_id = :tenantId
              AND v.data_venda >= :inicio AND v.data_venda < :fim
            GROUP BY COALESCE(NULLIF(BTRIM(ch.categoria_peca_hist), ''), 'Sem categoria')
        ), ranqueadas AS (
            SELECT categoria, unidades, lucro,
                   ROW_NUMBER() OVER (ORDER BY unidades DESC, categoria) AS posicao
            FROM por_categoria
        ), resumo AS (
            SELECT categoria, unidades, lucro FROM ranqueadas WHERE posicao <= 5
            UNION ALL
            SELECT 'Outras' AS categoria, SUM(unidades) AS unidades, SUM(lucro) AS lucro
            FROM ranqueadas WHERE posicao > 5 HAVING COUNT(*) > 0
        )
        SELECT categoria, unidades, lucro FROM resumo ORDER BY unidades DESC, categoria
        """, nativeQuery = true)
    List<Object[]> somarVendasPorCategoria(@Param("tenantId") UUID tenantId,
                                           @Param("inicio") LocalDateTime inicio,
                                           @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT YEAR(v.dataVenda), MONTH(v.dataVenda), COALESCE(SUM(h.lucroLiquido * v.unidades), 0)
        FROM VendaCalculo v JOIN v.calculo h
        WHERE v.dataVenda >= :inicio AND v.dataVenda < :fim AND h.lucroLiquido IS NOT NULL
        GROUP BY YEAR(v.dataVenda), MONTH(v.dataVenda)
        ORDER BY YEAR(v.dataVenda), MONTH(v.dataVenda)
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
        h.temFoto, h.unidades, h.custoTotal, h.lucroLiquido, h.dataVenda, h.unidadesVendidas
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
      AND (:filtrarVendido = false OR (:valorVendido = true AND h.unidadesVendidas > 0) OR (:valorVendido = false AND h.unidadesVendidas = 0))
      AND (:statusLote = '' OR
           (:statusLote = 'PARA_PRODUZIR' AND h.produzido = false AND h.unidadesVendidas = 0) OR
           (:statusLote = 'ESTOQUE' AND h.produzido = true AND h.unidadesVendidas < h.unidades) OR
           (:statusLote = 'VENDIDOS' AND h.unidadesVendidas > 0))
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
      AND (:filtrarVendido = false OR (:valorVendido = true AND h.unidadesVendidas > 0) OR (:valorVendido = false AND h.unidadesVendidas = 0))
      AND (:statusLote = '' OR
           (:statusLote = 'PARA_PRODUZIR' AND h.produzido = false AND h.unidadesVendidas = 0) OR
           (:statusLote = 'ESTOQUE' AND h.produzido = true AND h.unidadesVendidas < h.unidades) OR
           (:statusLote = 'VENDIDOS' AND h.unidadesVendidas > 0))
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
            @Param("statusLote") String statusLote,
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
    Long somarMinutosPorImpressora(@Param("impressoraId") UUID impressoraId);

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
