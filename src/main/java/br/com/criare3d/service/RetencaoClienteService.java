package br.com.criare3d.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;

@Service
public class RetencaoClienteService {
    public RetencaoClienteService(JdbcTemplate jdbc, Clock clock, PlatformTransactionManager transactions, TenantTemaCache cache) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc, "jdbc");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        this.transactions = java.util.Objects.requireNonNull(transactions, "transactions");
        this.cache = java.util.Objects.requireNonNull(cache, "cache");
    }

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final PlatformTransactionManager transactions;
    private final TenantTemaCache cache;
    @Value("${CRIARE_PURGA_CLIENTES_HABILITADA:false}") private boolean habilitada;
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RetencaoClienteService.class);

    @Scheduled(cron = "${criare.retencao.cron:0 0 4 * * *}", zone = "America/Sao_Paulo")
    public void executar() {
        jdbc.update("DELETE FROM recuperacao_senha WHERE expira_em < ?", java.sql.Timestamp.from(clock.instant()));
        jdbc.update("DELETE FROM foto_rascunho WHERE atualizado_em < ?", java.sql.Timestamp.from(clock.instant().minusSeconds(1800)));
        if (!habilitada) return;
        Instant limite = clock.instant().minus(Duration.ofDays(30));
        List<UUID> ids = jdbc.query("SELECT id FROM tenant WHERE excluido_em <= ?", (rs, n) -> rs.getObject(1, UUID.class), java.sql.Timestamp.from(limite));
        for (UUID id : ids) {
            try {
                new TransactionTemplate(transactions).executeWithoutResult(status -> excluirExpirado(id, limite));
                cache.invalidar(id);
            } catch (RuntimeException erro) {
                LOG.error("Expurgo de cliente revertido; id={}, tipo={}", id, erro.getClass().getSimpleName());
            }
        }
    }

    void excluirExpirado(UUID id, Instant limite) {
        var expirar = jdbc.query("SELECT id FROM tenant WHERE id = ? AND excluido_em <= ? FOR UPDATE",
                (rs, n) -> rs.getObject(1, UUID.class), id, java.sql.Timestamp.from(limite));
        if (expirar.isEmpty()) return;
        // Descobre todas as tabelas do schema, incluindo as adicionadas por migrations futuras.
        List<String> tabelas = jdbc.queryForList("SELECT table_name FROM information_schema.columns WHERE table_schema = current_schema() AND column_name = 'tenant_id'", String.class);
        Map<String, Set<String>> filhos = new LinkedHashMap<>();
        tabelas.forEach(t -> filhos.put(t, new HashSet<>()));
        jdbc.query("""
                SELECT filha.relname, pai.relname FROM pg_constraint fk
                JOIN pg_class filha ON filha.oid = fk.conrelid
                JOIN pg_namespace ns ON ns.oid = filha.relnamespace
                JOIN pg_class pai ON pai.oid = fk.confrelid
                WHERE fk.contype = 'f' AND ns.nspname = current_schema()
                """, rs -> {
            String filha = rs.getString(1), pai = rs.getString(2);
            if (!filha.equals(pai) && filhos.containsKey(filha) && filhos.containsKey(pai)) filhos.get(pai).add(filha);
        });
        for (String tabela : ordenar(filhos)) {
            if (!tabela.matches("[a-z_][a-z0-9_]*")) throw new IllegalStateException("Identificador de tabela inválido");
            jdbc.update("DELETE FROM \"" + tabela + "\" WHERE tenant_id = ?", id);
        }
        jdbc.update("DELETE FROM tenant WHERE id = ? AND excluido_em <= ?", id, java.sql.Timestamp.from(limite));
    }

    static List<String> ordenar(Map<String, Set<String>> dependencias) {
        Map<String, Set<String>> restantes = new LinkedHashMap<>();
        dependencias.forEach((t, f) -> restantes.put(t, new HashSet<>(f)));
        List<String> ordem = new ArrayList<>();
        while (!restantes.isEmpty()) {
            String proxima = restantes.entrySet().stream().filter(e -> e.getValue().isEmpty())
                    .map(Map.Entry::getKey).sorted().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Dependências cíclicas: expurgo interrompido sem apagar dados"));
            ordem.add(proxima); restantes.remove(proxima);
            restantes.values().forEach(f -> f.remove(proxima));
        }
        return ordem;
    }
}
