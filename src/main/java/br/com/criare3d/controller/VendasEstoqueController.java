package br.com.criare3d.controller;
import br.com.criare3d.service.ResultadoOperacao;

import br.com.criare3d.service.AcaoEmLoteService;
import br.com.criare3d.service.IndicadoresService;
import br.com.criare3d.service.VendaService;

import br.com.criare3d.dto.HistoricoListaResponse;
import br.com.criare3d.service.CatalogoBuscaService;

import br.com.criare3d.util.HistoricoFiltros;
import br.com.criare3d.util.OrdenacaoPaginada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;

@Controller
public class VendasEstoqueController {
    private final CatalogoBuscaService catalogoBuscaService;
    private final AcaoEmLoteService acaoEmLoteService;
    private final IndicadoresService indicadoresService;
    private final VendaService vendaService;
    private final br.com.criare3d.repository.ItemPedidoRepository reservas;

    public VendasEstoqueController(CatalogoBuscaService catalogoBuscaService,
                                   AcaoEmLoteService acaoEmLoteService, IndicadoresService indicadoresService, VendaService vendaService, br.com.criare3d.repository.ItemPedidoRepository reservas) {
        this.catalogoBuscaService = catalogoBuscaService;
        this.acaoEmLoteService = acaoEmLoteService;
        this.indicadoresService = indicadoresService;
        this.vendaService = vendaService;
        this.reservas = reservas;
    }

    @GetMapping("/vendas-estoque")
    public ModelAndView listar(@RequestParam(defaultValue = "") String busca,
                              @RequestParam(defaultValue = "") String aba,
                              @RequestParam(defaultValue = "") String erro,
                              @RequestParam(defaultValue = "") String sucesso,
                              @PageableDefault(size = 6) Pageable pageable) {
        aba = normalizarAba(aba);
        Pageable recentes = PageRequest.of(0, 6,
                Sort.by(Sort.Order.desc("dataCalculo"), Sort.Order.asc("id")));
        Page<HistoricoListaResponse> paraProduzir = buscarCategoria("PARA_PRODUZIR", "", recentes);
        Page<HistoricoListaResponse> emEstoque = buscarCategoria("ESTOQUE", "", recentes);
        Page<HistoricoListaResponse> vendidos = buscarCategoria("VENDIDOS", "", recentes);

        pageable = OrdenacaoPaginada.aplicar(pageable, "dataDesc", HistoricoFiltros.ORDENACOES);
        Page<HistoricoListaResponse> pagina = aba.isBlank()
                ? Page.empty(pageable)
                : buscarCategoria(aba, busca, pageable);

        LocalDateTime inicioMes = YearMonth.now(ZoneId.of("America/Sao_Paulo"))
                .atDay(1).atStartOfDay();
        LocalDateTime fimMes = YearMonth.from(inicioMes)
                .plusMonths(1).atDay(1).atStartOfDay();
        long unidadesProduzidas = indicadoresService.contarProduzidos();
        long unidadesVendidas = indicadoresService.contarVendidos();
        long unidadesVendidasMes = indicadoresService.contarUnidadesVendidasEntre(inicioMes, fimMes);
        long lotesVendidosMes = indicadoresService.contarLotesVendidosEntre(inicioMes, fimMes);
        BigDecimal lucroVendidoMes = indicadoresService.somarLucroVendidoEntre(inicioMes, fimMes);

        ModelAndView mv = new ModelAndView("vendas-estoque/listar");
        var saldos = new java.util.HashMap<UUID, Long>();
        java.util.stream.Stream.concat(emEstoque.stream(), pagina.stream()).forEach(l -> saldos.computeIfAbsent(l.id(), reservas::reservadas));
        mv.addObject("reservadas", saldos);
        mv.addObject("modoDetalhe", !aba.isBlank());
        mv.addObject("aba", aba);
        mv.addObject("abaTitulo", tituloAba(aba));
        mv.addObject("historicos", pagina.getContent());
        mv.addObject("pagina", pagina);
        mv.addObject("busca", busca);
        mv.addObject("tamanhoPagina", pageable.getPageSize());
        mv.addObject("paraProduzir", paraProduzir.getContent());
        mv.addObject("emEstoque", emEstoque.getContent());
        mv.addObject("vendidos", vendidos.getContent());
        mv.addObject("lotesParaProduzir", paraProduzir.getTotalElements());
        mv.addObject("lotesEmEstoque", emEstoque.getTotalElements());
        mv.addObject("lotesVendidos", vendidos.getTotalElements());
        mv.addObject("unidadesParaProduzir", indicadoresService.contarUnidadesParaProduzir());
        mv.addObject("unidadesProduzidas", unidadesProduzidas);
        mv.addObject("unidadesVendidas", unidadesVendidas);
        mv.addObject("unidadesEmEstoque", Math.max(0, unidadesProduzidas - unidadesVendidas));
        mv.addObject("valorEmEstoque", indicadoresService.somarCustoUnidadesEmEstoque());
        mv.addObject("unidadesVendidasMes", unidadesVendidasMes);
        mv.addObject("lotesVendidosMes", lotesVendidosMes);
        mv.addObject("lucroVendidoMes", lucroVendidoMes == null ? BigDecimal.ZERO : lucroVendidoMes);
        mv.addObject("erro", erro);
        mv.addObject("sucesso", sucesso);
        return mv;
    }

    @PostMapping("/vendas-estoque/registrar")
    public ModelAndView registrar(@RequestParam(name = "ids", required = false) List<UUID> ids,
                                  @RequestParam String acao,
                                  @RequestParam(defaultValue = "") String busca,
                                  @RequestParam(defaultValue = "") String aba,
                                  @RequestParam(defaultValue = "1") Integer quantidade,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        ResultadoOperacao resultado = "VENDER".equals(acao)
                ? (ids != null && ids.size() == 1
                    ? vendaService.registrarVenda(ids.get(0), quantidade)
                    : acaoEmLoteService.registrarVendasEmLote(ids, quantidade))
                : acaoEmLoteService.atualizarStatusEmLote(ids, acao);
        String mensagemSucesso = switch (acao) {
            case "VENDER" -> "vendasRegistradas";
            case "DESFAZER_PRODUCAO" -> "producoesDesfeitas";
            case "CANCELAR_VENDA" -> "vendasCanceladas";
            default -> "producoesRegistradas";
        };
        String status = ResultadoOperacao.OK.equals(resultado)
                ? "sucesso=" + mensagemSucesso
                : "erro=" + resultado.codigo();
        String destino;
        if (ResultadoOperacao.OK.equals(resultado)) {
            destino = "/vendas-estoque?" + status;
        } else {
            destino = "/vendas-estoque?busca=" + java.net.URLEncoder.encode(
                    busca == null ? "" : busca, java.nio.charset.StandardCharsets.UTF_8)
                    + "&aba=" + normalizarAba(aba)
                    + "&page=" + Math.max(0, page) + "&size=" + Math.min(Math.max(size, 6), 50)
                    + "&" + status;
        }
        return new ModelAndView(new RedirectView(destino, true));
    }

    private Page<HistoricoListaResponse> buscarCategoria(String categoria, String busca, Pageable pageable) {
        return catalogoBuscaService.buscarHistorico(busca, null, "", "TODOS", "TODOS", categoria, pageable);
    }

    private String normalizarAba(String aba) {
        if (aba == null) return "";
        return switch (aba.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "PARA_PRODUZIR", "ESTOQUE", "VENDIDOS" -> aba.trim().toUpperCase(java.util.Locale.ROOT);
            default -> "";
        };
    }

    private String tituloAba(String aba) {
        return switch (aba) {
            case "PARA_PRODUZIR" -> "Para produzir";
            case "ESTOQUE" -> "Em estoque";
            case "VENDIDOS" -> "Vendidos";
            default -> "";
        };
    }
}
