package br.com.criare3d.controller;

import br.com.criare3d.models.Filamento;
import br.com.criare3d.models.Impressora;
import br.com.criare3d.models.Peca;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import br.com.criare3d.service.interfaces.PecaService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class CatalogoExportacaoController {
    private final PecaService pecaService;
    private final FilamentoService filamentoService;
    private final ImpressoraService impressoraService;

    public CatalogoExportacaoController(PecaService pecaService,
                                       FilamentoService filamentoService,
                                       ImpressoraService impressoraService) {
        this.pecaService = pecaService;
        this.filamentoService = filamentoService;
        this.impressoraService = impressoraService;
    }

    @GetMapping(value = "/peca/exportar.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportarPecas() {
        List<Peca> itens = pecaService.fetchAll(Pageable.unpaged()).getContent();
        StringBuilder csv = new StringBuilder("\uFEFFNome da peça;Filamento por unidade (g);Tempo por unidade (min)\r\n");
        itens.forEach(item -> csv.append(campo(item.getNome())).append(';')
                .append(numero(item.getQuantidadeFilamentoGramas())).append(';')
                .append(item.getTempoImpressaoMinutos() == null ? "" : item.getTempoImpressaoMinutos()).append("\r\n"));
        return resposta("pecas.csv", csv);
    }

    @GetMapping(value = "/filamento/exportar.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportarFilamentos() {
        List<Filamento> itens = filamentoService.fetchAll(Pageable.unpaged()).getContent();
        StringBuilder csv = new StringBuilder("\uFEFFNome;Marca;Cor;Código da cor;Peso do rolo (g);Estoque (g);Preço do rolo\r\n");
        itens.forEach(item -> csv.append(campo(item.getNome())).append(';')
                .append(campo(item.getMarca())).append(';')
                .append(campo(item.getCor())).append(';')
                .append(campo(item.getCodigoCor())).append(';')
                .append(numero(item.getPesoRoloGramas())).append(';')
                .append(numero(item.getEstoqueGramas())).append(';')
                .append(numero(item.getPrecoRolo())).append("\r\n"));
        return resposta("filamentos.csv", csv);
    }

    @GetMapping(value = "/impressora/exportar.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportarImpressoras() {
        List<Impressora> itens = impressoraService.fetchAll(Pageable.unpaged()).getContent();
        StringBuilder csv = new StringBuilder("\uFEFFNome;Modelo;Potência (W);Aquecimento por impressão (min);Valor de compra;Vida útil (h);Manutenção por hora\r\n");
        itens.forEach(item -> csv.append(campo(item.getNome())).append(';')
                .append(campo(item.getModelo())).append(';')
                .append(numero(item.getPotenciaWatts())).append(';')
                .append(item.getTempoAquecimentoMinutos() == null ? "" : item.getTempoAquecimentoMinutos()).append(';')
                .append(numero(item.getValorCompra())).append(';')
                .append(numero(item.getVidaUtilHoras())).append(';')
                .append(numero(item.getManutencaoPorHora())).append("\r\n"));
        return resposta("impressoras.csv", csv);
    }

    private ResponseEntity<byte[]> resposta(String nome, StringBuilder csv) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String campo(String valor) {
        if (valor == null) return "\"\"";
        String seguro = valor.stripLeading().matches("^[=+@\\-].*") ? "'" + valor : valor;
        return "\"" + seguro.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\"";
    }

    private String numero(java.math.BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString().replace('.', ',');
    }
}
