package br.com.criare3d;

import br.com.criare3d.dto.DashboardResumo;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opt-in benchmark; seeds only its disposable Testcontainers PostgreSQL database.
 * Run with: ./mvnw -Dcriare.dashboard.perf=true -Dtest=DashboardPerformanceTest test
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@EnabledIfSystemProperty(named = "criare.dashboard.perf", matches = "true")
class DashboardPerformanceTest {

    private static final int TENANTS = 3;
    private static final int HISTORICOS_POR_TENANT = 20_000;
    private static final String SQL_EXPLAIN = """
            EXPLAIN (ANALYZE, BUFFERS)
            SELECT COALESCE(SUM(h.unidades_hist) FILTER (WHERE h.data_hist >= ?), 0),
                   COALESCE(SUM(h.unidades_hist) FILTER (WHERE h.produzido_hist = TRUE AND h.data_hist >= ?), 0),
                   COALESCE(SUM(h.unidades_hist) FILTER (WHERE h.vendido_hist = TRUE
                       AND h.data_venda_hist >= ? AND h.data_venda_hist < ?), 0),
                   COALESCE(SUM(h.lucro_liquido_hist * h.unidades_hist) FILTER (WHERE h.vendido_hist = TRUE
                       AND h.data_venda_hist >= ? AND h.data_venda_hist < ?), 0),
                   COALESCE(SUM((h.temp_hist + COALESCE(h.tempo_aquecimento_hist, 0)) * h.fornadas_hist)
                       FILTER (WHERE h.vendido_hist = TRUE
                       AND h.data_venda_hist >= ? AND h.data_venda_hist < ?), 0),
                   COALESCE(SUM((h.temp_hist + COALESCE(h.tempo_aquecimento_hist, 0)) * h.fornadas_hist)
                       FILTER (WHERE h.produzido_hist = TRUE AND h.data_hist >= ?), 0),
                   COALESCE(SUM(h.gramas_totais_lote_hist)
                       FILTER (WHERE h.produzido_hist = TRUE AND h.data_hist >= ?), 0)
              FROM calculo_historico h
             WHERE h.usuario_id = ? AND h.tenant_id = ?
               AND (h.data_hist >= ? OR (h.vendido_hist = TRUE
                    AND h.data_venda_hist >= ? AND h.data_venda_hist < ?))
            """;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private CalculoHistoricoRepository historicoRepository;

    private final List<UUID> tenantIds = new ArrayList<>();
    private final List<UUID> usuarioIds = new ArrayList<>();

    @Test
    void medeResumoDoDashboardComTresTenantsEDezenasDeMilharesDeRegistros() {
        criarDadosDeTeste();
        jdbc.execute("ANALYZE calculo_historico");

        LocalDateTime fim = LocalDateTime.now().withNano(0).plusDays(1);
        LocalDateTime inicio = fim.minusDays(30);
        for (int i = 0; i < TENANTS; i++) {
            UUID tenantId = tenantIds.get(i);
            UUID usuarioId = usuarioIds.get(i);

            // Aquecimento do plano/cache de páginas antes de registrar as medições.
            DashboardResumo.from(historicoRepository.resumirDashboard(usuarioId, tenantId, inicio, fim));
            long menorTempoMs = Long.MAX_VALUE;
            DashboardResumo resumo = null;
            for (int repeticao = 0; repeticao < 5; repeticao++) {
                long comeco = System.nanoTime();
                resumo = DashboardResumo.from(
                        historicoRepository.resumirDashboard(usuarioId, tenantId, inicio, fim));
                menorTempoMs = Math.min(menorTempoMs, (System.nanoTime() - comeco) / 1_000_000);
            }

            List<String> plano = jdbc.query(SQL_EXPLAIN, (rs, row) -> rs.getString(1),
                    inicio, inicio, inicio, fim, inicio, fim, inicio, fim, inicio,
                    inicio, usuarioId, tenantId, inicio, inicio, fim);
            System.out.printf("Tenant %s: 20.000 históricos, melhor resumo=%d ms%n%s%n",
                    tenantId, menorTempoMs, String.join(System.lineSeparator(), plano));
            assertThat(resumo).isNotNull();
            assertThat(resumo.calculadas()).isPositive();
        }
    }

    private void criarDadosDeTeste() {
        for (int i = 0; i < TENANTS; i++) {
            UUID tenantId = UUID.randomUUID();
            UUID usuarioId = UUID.randomUUID();
            tenantIds.add(tenantId);
            usuarioIds.add(usuarioId);

            jdbc.update("""
                    INSERT INTO tenant (id, nome, slug, ativo, cor_primaria, cor_destaque, cor_fundo, tema_versao)
                    VALUES (?, ?, ?, TRUE, '#492e1e', '#e9a695', '#f5e7da', 1)
                    """, tenantId, "Benchmark " + i, "benchmark-" + tenantId.toString().replace("-", ""));
            jdbc.update("""
                    INSERT INTO usuario (id, email, password, perfil, tenant_id)
                    VALUES (?, ?, 'nao-usada', 'USER', ?)
                    """, usuarioId, "benchmark-" + tenantId + "@teste.local", tenantId);
            jdbc.update("""
                    INSERT INTO calculo_historico (
                        id, nome_peca_hist, nome_fil_hist, preco_rolo_hist, peso_rolo_fil, qntd_fil_hist,
                        nome_imp_hist, pot_hist, temp_hist, tarifa_hist, custo_fil_hist,
                        custo_energia_hist, nome_emb_hist, custo_emb_hist, custo_total_hist,
                        markup_hist, preco_sug_hist, data_hist, usuario_id, tenant_id,
                        produzido_hist, vendido_hist, data_venda_hist, unidades_hist,
                        fornadas_hist, gramas_totais_lote_hist
                    )
                    SELECT md5(? || ':' || serie::text)::uuid, 'Peça de benchmark', 'PLA', 100, 1000, 10,
                           'Impressora', 100, 60, 0.8, 1, 0.01, 'Embalagem', 1, 2,
                           100, 4, now() - (serie % 730) * interval '1 day', ?, ?, TRUE,
                           serie % 3 = 0,
                           CASE WHEN serie % 3 = 0
                                THEN now() - (serie % 730) * interval '1 day' ELSE NULL END,
                           1, 1, 10
                      FROM generate_series(1, ?) AS serie
                    """, tenantId.toString(), usuarioId, tenantId, HISTORICOS_POR_TENANT);
        }
    }

    @AfterEach
    void limparDados() {
        for (UUID tenantId : tenantIds) {
            jdbc.update("DELETE FROM calculo_historico WHERE tenant_id = ?", tenantId);
            jdbc.update("DELETE FROM usuario WHERE tenant_id = ?", tenantId);
            jdbc.update("DELETE FROM tenant WHERE id = ?", tenantId);
        }
    }
}
