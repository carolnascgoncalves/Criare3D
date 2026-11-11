package br.com.criare3d.controller;

import br.com.criare3d.dto.HistoricoExportacaoResponse;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class HistoricoExportacaoController {
    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"));

    private final CalculoHistoricoService calculoHistoricoService;

    public HistoricoExportacaoController(CalculoHistoricoService calculoHistoricoService) {
        this.calculoHistoricoService = calculoHistoricoService;
    }

    @GetMapping(value = "/historico/exportar.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportarHistoricoCsv() {
        StringBuilder csv = new StringBuilder("\uFEFFPeça;Data do cálculo;Plataforma;Custo unitário;Preço sugerido por unidade;Lucro líquido por unidade;Unidades;Impressões;Produzido;Vendido\r\n");
        for (HistoricoExportacaoResponse item : calculoHistoricoService.exportarHistorico()) {
            csv.append(campoCsv(item.nomePeca())).append(';')
                    .append(campoCsv(item.dataCalculo() == null ? "" : item.dataCalculo().format(FORMATO_DATA))).append(';')
                    .append(campoCsv(nomePlataforma(item.plataforma()))).append(';')
                    .append(numeroCsv(item.custoTotal())).append(';')
                    .append(numeroCsv(item.precoSugerido())).append(';')
                    .append(numeroCsv(item.lucroLiquido())).append(';')
                    .append(item.unidades() == null ? 1 : item.unidades()).append(';')
                    .append(item.fornadas() == null ? (item.unidades() == null ? 1 : item.unidades()) : item.fornadas()).append(';')
                    .append(Boolean.TRUE.equals(item.produzido()) ? "Sim" : "Não").append(';')
                    .append(Boolean.TRUE.equals(item.vendido()) ? "Sim" : "Não").append("\r\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"historico-calculos.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String campoCsv(String valor) {
        if (valor == null) return "\"\"";
        String seguro = valor.stripLeading().matches("^[=+@\\-].*") ? "'" + valor : valor;
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }

    private String numeroCsv(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString().replace('.', ',');
    }

    private String nomePlataforma(String plataforma) {
        if (plataforma == null || plataforma.isBlank() || "NENHUMA".equals(plataforma)) return "Sem plataforma";
        return switch (plataforma) {
            case "TIKTOK", "TIKTOK_SHOP" -> "TikTok Shop";
            case "SHOPEE" -> "Shopee";
            case "MERCADO_LIVRE" -> "Mercado Livre";
            case "AMAZON" -> "Amazon";
            default -> plataforma;
        };
    }
}
