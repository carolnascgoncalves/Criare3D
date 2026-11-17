package br.com.criare3d.service;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.RascunhoCalculo;
import br.com.criare3d.dto.RascunhoFormulario;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.DataBinder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Service
public class CalculoRascunhoService {
    public static final Duration RETENCAO = Duration.ofDays(30);
    private final JdbcTemplate jdbc;
    private final UsuarioLogadoService usuario;
    private final Clock clock;
    private final JsonMapper json = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    public CalculoRascunhoService(JdbcTemplate jdbc, UsuarioLogadoService usuario, Clock clock) {
        this.jdbc = jdbc;
        this.usuario = usuario;
        this.clock = clock;
    }

    public RascunhoCalculo abrir(UUID id, boolean novo) {
        if (novo) id = null;
        else if (id == null) {
            id = jdbc.query("""
                SELECT id FROM calculo_rascunho WHERE tenant_id=? AND usuario_id=? AND atualizado_em>?
                ORDER BY atualizado_em DESC, id DESC LIMIT 1
                """, (rs, n) -> rs.getObject(1, UUID.class), usuario.buscarTenantIdAtual(),
                usuario.buscarIdUsuarioAtual(), limite()).stream().findFirst().orElse(null);
        }
        if (id == null) {
            id = UUID.randomUUID();
            jdbc.update("INSERT INTO calculo_rascunho(id,tenant_id,usuario_id,json,atualizado_em) VALUES (?,?,?,'{}',?)",
                id, usuario.buscarTenantIdAtual(), usuario.buscarIdUsuarioAtual(), Timestamp.from(clock.instant()));
        }
        UUID referencia = id;
        var dados = jdbc.query("""
            SELECT json::text FROM calculo_rascunho WHERE id=? AND tenant_id=? AND usuario_id=? AND atualizado_em>?
            """, (rs, n) -> json.readValue(rs.getString(1), RascunhoCalculo.Dados.class), referencia,
            usuario.buscarTenantIdAtual(), usuario.buscarIdUsuarioAtual(), limite()).stream().findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Rascunho não encontrado ou expirado."));
        return new RascunhoCalculo(referencia, dados, (chave, valor) -> salvar(referencia, chave, valor));
    }

    private Timestamp limite() {
        return Timestamp.from(clock.instant().minus(RETENCAO));
    }

    private void salvar(UUID id, String chave, Object valor) {
        int alterados = valor == null
            ? jdbc.update("""
                UPDATE calculo_rascunho SET json=json-cast(? as text), atualizado_em=?, versao=versao+1
                WHERE id=? AND tenant_id=? AND usuario_id=? AND atualizado_em>?
                """, chave, Timestamp.from(clock.instant()), id, usuario.buscarTenantIdAtual(),
                usuario.buscarIdUsuarioAtual(), limite())
            : jdbc.update("""
                UPDATE calculo_rascunho SET json=jsonb_set(json,ARRAY[cast(? as text)],cast(? as jsonb),true),
                    atualizado_em=?, versao=versao+1
                WHERE id=? AND tenant_id=? AND usuario_id=? AND atualizado_em>?
                """, chave, json.writeValueAsString(valor), Timestamp.from(clock.instant()), id,
                usuario.buscarTenantIdAtual(), usuario.buscarIdUsuarioAtual(), limite());
        if (alterados != 1) throw new IllegalArgumentException("Rascunho não encontrado ou expirado.");
    }

    public String formularioJson(RascunhoCalculo rascunho) {
        return json.writeValueAsString(rascunho.getDados().getFormulario())
            .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026");
    }

    public static CalculoRequest formulario(RascunhoFormulario formulario) {
        var calculo = new CalculoRequest();
        var binder = new DataBinder(calculo);
        binder.setAutoGrowCollectionLimit(100);
        // Rascunhos só restauram entradas editáveis; nunca preços históricos ou fotos fornecidos pelo cliente.
        binder.setDisallowedFields("referenciaCustosHistoricos", "custosPreservados", "custosPreservados.*",
            "precosAtuaisNoOrcamento", "fotoPecaBytes", "fotoPecaContentType", "class.*", "class");
        var valores = new MutablePropertyValues(formulario.campos());
        for (int i = 0; i < formulario.filamentos().size(); i++) {
            var item = formulario.filamentos().get(i);
            valores.add("filamentos[" + i + "].filamentoId", item.filamentoId());
            valores.add("filamentos[" + i + "].quantidadeFilamentoGramas", item.quantidade());
        }
        binder.bind(valores);
        return calculo;
    }
}
