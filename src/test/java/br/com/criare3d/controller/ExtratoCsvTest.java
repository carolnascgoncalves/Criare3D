package br.com.criare3d.controller;

import br.com.criare3d.models.Movimentacao;
import br.com.criare3d.service.MovimentacaoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExtratoCsvTest {
    @Test
    void escreveNomeECorEmUtf8PreservandoEscapeEProtecaoContraFormulas() {
        var id = UUID.randomUUID();
        var movimento = new Movimentacao();
        movimento.setFilamentoId(id);
        movimento.setData(LocalDateTime.of(2026, 11, 17, 10, 30));
        movimento.setTipo("PRODUCAO");
        movimento.setDescricao("=HYPERLINK(\"teste\")\nPeça");
        movimento.setQuantidade(new BigDecimal("-12.500000"));
        movimento.setMedida("g");
        movimento.setSaldoApos(new BigDecimal("987.500000"));
        var arquivo = ExtratoCsv.gerar(new MovimentacaoService.Exportacao(List.of(movimento),
            Map.of(id, "PLA · Amarelo"), false));
        assertThat(new String(arquivo, StandardCharsets.UTF_8))
            .startsWith("\uFEFFData;")
            .contains("17/11/2026 10:30", "Produção", "PLA · Amarelo", "-12,5", "987,500000",
                "\"'=HYPERLINK(\"\"teste\"\") Peça\"")
            .doesNotContain(id.toString());
    }

    @Test
    void filamentoRemovidoNaoVazaIdentificadorNoArquivo() {
        var movimento = new Movimentacao();
        var id = UUID.randomUUID();
        movimento.setFilamentoId(id);
        movimento.setData(LocalDateTime.of(2026, 11, 17, 10, 30));
        movimento.setTipo("COMPRA");
        movimento.setQuantidade(BigDecimal.ONE);
        movimento.setMedida("g");
        assertThat(new String(ExtratoCsv.gerar(new MovimentacaoService.Exportacao(
            List.of(movimento), Map.of(), false)), StandardCharsets.UTF_8))
            .contains("Filamento removido").doesNotContain(id.toString());
    }
}
