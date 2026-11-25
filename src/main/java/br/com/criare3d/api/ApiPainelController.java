package br.com.criare3d.api;

import br.com.criare3d.models.Peca;
import br.com.criare3d.models.Embalagem;
import br.com.criare3d.dto.PecaArquivoResumo;
import br.com.criare3d.dto.PecaMaterialResumo;
import br.com.criare3d.service.PecaArquivoService;
import br.com.criare3d.service.interfaces.PecaService;
import br.com.criare3d.service.interfaces.ManutencaoImpressoraService;
import org.springframework.http.ResponseEntity;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.HistoricoCalculoService;
import br.com.criare3d.service.IndicadoresService;
import br.com.criare3d.service.interfaces.FilamentoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ApiPainelController {
    private final br.com.criare3d.service.DashboardDadosService dashboard;
    private final br.com.criare3d.repository.CompradorRepository compradores;
    private final CatalogoBuscaService catalogo;
    private final PecaArquivoService arquivos;
    private final PecaService pecas;
    private final ManutencaoImpressoraService manutencao;
    private final HistoricoCalculoService historico;
    private final IndicadoresService indicadores;
    private final br.com.criare3d.service.interfaces.ConfiguracaoService configuracoes;
    private final FilamentoService filamentos;
    private final br.com.criare3d.repository.ItemPedidoRepository reservas;

    public record PecaCatalogo(UUID id, String nome, String categoria, String gramas, Long minutos, String preco,
            Integer placaMiniatura, String modeloImpressora, List<PecaMaterialResumo> materiais) {}
    public record FotoCatalogo(String tipo, String base64) {}
    public record FilamentoCatalogo(UUID id, String nome, String cor, String estoque, String codigoCor, String pesoRolo, String marca, String precoRolo) {}
    public record ImpressoraCatalogo(UUID id, String nome, String modelo, String watts, Integer aquecimento) {}
    public record EmbalagemCatalogo(UUID id, String nome, String custo, long usos) {}
    public record LoteResumo(UUID id, String nome, Integer unidades, Integer vendidas, Long disponiveis, long reservadas, String plataforma, String preco) {}
    public record Recente(UUID id, String nome, String situacao, String custoUnitario, String precoSugerido, Integer unidades) {}
    public record InicioLoja(List<Recente> recentes, long aguardandoVenda, FilamentoCatalogo estoqueCritico, boolean valorHoraConfigurado) {}

    private static String decimal(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }

    private static org.springframework.data.domain.Pageable paginaCatalogo(int pagina) {
        return PaginaResponse.pedido(pagina, 12, Sort.by("nome", "id"));
    }

    @GetMapping("/catalogo/pecas")
    public PaginaResponse<PecaCatalogo> pecas(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "") String busca) {
        var pecas = catalogo.buscarPecas(busca.trim(), paginaCatalogo(pagina));
        var precos = historico.ultimosPrecosPecas(pecas.getContent().stream().map(Peca::getId).toList());
        var resumos = arquivos.buscarResumos(pecas.getContent());
        return PaginaResponse.de(pecas.map(p -> {
            var resumo = resumos.get(p.getId());
            Integer placa = resumo == null ? null : resumo.placas().stream()
                    .filter(PecaArquivoResumo.PlacaResumo::temImagem).map(PecaArquivoResumo.PlacaResumo::numero)
                    .findFirst().orElse(null);
            var materiais = resumo == null ? List.<PecaMaterialResumo>of() : resumo.filamentosUsados().stream()
                    .filter(material -> material.pesoGramas() > 0).toList();
            return new PecaCatalogo(p.getId(), p.getNome(), p.getCategoria(), decimal(p.getQuantidadeFilamentoGramas()),
                    p.getTempoImpressaoMinutos(), decimal(precos.get(p.getId())), placa,
                    resumo == null ? null : resumo.modeloImpressora(), materiais);
        }));
    }

    public record PlacaCatalogo(int numero, boolean temImagem, Double tempoSegundos, Double pesoGramas,
                               List<PecaMaterialResumo> materiais) {}
    public record ArquivoCatalogo(String nomeArquivo, String modeloImpressora, String fatiador, int numeroPlacas,
                                 List<PlacaCatalogo> placas) {}

    @GetMapping("/catalogo/pecas/{id}/arquivo")
    public ResponseEntity<ArquivoCatalogo> arquivo(@PathVariable UUID id) {
        var peca = pecas.fetchById(id);
        if (peca.isEmpty()) return ResponseEntity.notFound().build();
        return arquivos.buscarResumo(peca.get()).map(a -> ResponseEntity.ok(new ArquivoCatalogo(
                a.nomeArquivo(), a.modeloImpressora(), a.fatiador(), a.numeroPlacas(),
                a.placas().stream().map(p -> new PlacaCatalogo(p.numero(), p.temImagem(), p.tempoSegundos(),
                        p.pesoGramas(), p.materiais().stream().filter(m -> m.pesoGramas() > 0).toList())).toList())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    public record GastoCatalogo(UUID id, String descricao, java.time.LocalDate data, String valor) {}
    public record CustosImpressora(String depreciacaoHora, String vidaUtilHoras, String horasMensais,
            long minutosImpressos, String gastoTotal, String manutencaoHora, List<GastoCatalogo> registros) {}

    @GetMapping("/catalogo/impressoras/{id}/custos")
    public ResponseEntity<CustosImpressora> custos(@PathVariable UUID id) {
        return ResponseEntity.of(manutencao.resumo(id).map(r -> {
            var i = r.impressora();
            return new CustosImpressora(decimal(i.getDepreciacaoPorHora()), decimal(i.getVidaUtilHoras()),
                    decimal(i.getHorasMensaisUso()), r.minutosImpressos(), decimal(r.totalGasto()),
                    decimal(r.custoPorHora()), r.registros().stream().map(g ->
                            new GastoCatalogo(g.getId(), g.getDescricao(), g.getData(), decimal(g.getValor()))).toList());
        }));
    }

    @GetMapping("/catalogo/pecas/{id}/placas/{numero}/foto")
    public ResponseEntity<FotoCatalogo> foto(@PathVariable UUID id, @PathVariable int numero) {
        if (numero < 1 || numero > 8) return ResponseEntity.notFound().build();
        return arquivos.buscarImagem(id, numero).map(imagem -> ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(new FotoCatalogo(imagem.mimeType(), java.util.Base64.getEncoder().encodeToString(imagem.bytes()))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/catalogo/filamentos")
    public PaginaResponse<FilamentoCatalogo> filamentos(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "") String busca) {
        return PaginaResponse.de(catalogo.buscarFilamentos(busca.trim(), paginaCatalogo(pagina)).map(f ->
                new FilamentoCatalogo(f.getId(), f.getNome(), f.getCor(), decimal(f.getEstoqueGramas()), f.getCodigoCor(), decimal(f.getPesoRoloGramas()), f.getMarca(), decimal(f.getPrecoRolo()))));
    }

    @GetMapping("/catalogo/impressoras")
    public PaginaResponse<ImpressoraCatalogo> impressoras(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "") String busca) {
        return PaginaResponse.de(catalogo.buscarImpressoras(busca.trim(), paginaCatalogo(pagina)).map(i ->
                new ImpressoraCatalogo(i.getId(), i.getNome(), i.getModelo(), decimal(i.getPotenciaWatts()), i.getTempoAquecimentoMinutos())));
    }

    @GetMapping("/catalogo/embalagens")
    public PaginaResponse<EmbalagemCatalogo> embalagens(@RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "") String busca) {
        var embalagens = catalogo.buscarEmbalagens(busca.trim(), paginaCatalogo(pagina));
        var usos = historico.usosEmbalagens(embalagens.getContent().stream().map(Embalagem::getId).toList());
        return PaginaResponse.de(embalagens.map(e ->
                new EmbalagemCatalogo(e.getId(), e.getNome(), decimal(e.getCustoUnitario()), usos.getOrDefault(e.getId(), 0L))));
    }

    public enum GrupoVenda { PARA_PRODUZIR, ESTOQUE, VENDIDOS }

    @GetMapping("/vendas")
    public PaginaResponse<LoteResumo> vendas(@RequestParam(defaultValue = "ESTOQUE") GrupoVenda grupo,
                                               @RequestParam(defaultValue = "0") int pagina) {
        var ordem = Sort.by(Sort.Order.desc("dataCalculo"), Sort.Order.asc("id"));
        var lotes = catalogo.buscarHistorico("", null, "", "TODOS", "TODOS", grupo.name(), PaginaResponse.pedido(pagina, 12, ordem));
        return PaginaResponse.de(lotes.map(l -> {
            long reservadas = reservas.reservadas(l.id());
            int estoque = Boolean.TRUE.equals(l.produzido()) ? (l.unidades() == null ? 1 : l.unidades()) - (l.unidadesVendidas() == null ? 0 : l.unidadesVendidas()) : 0;
            return new LoteResumo(l.id(), l.nomePeca(), l.unidades(), l.unidadesVendidas(),
                    Math.max(0L, estoque - reservadas), reservadas, l.plataformaVenda(),
                    decimal(l.precoVendaPlataforma() != null ? l.precoVendaPlataforma() : l.precoSugerido()));
        }));
    }

    public record PontoLucro(String data, String valor) {}
    public record VendasPlataforma(String nome, long vendas, String lucro, String cor) {}
    public record EstoquePainel(String nome, String gramas, int restante, String cor) {}
    public record VendasCategoria(String nome, long unidades) {}
    public record CustoPainel(String nome, String valor, int percentual) {}
    public record CadastrosPainel(long pecas, long filamentos, long impressoras, long embalagens, long clientes) {}
    public record DashboardLoja(String lucro, long vendas, String horas, String filamentoKg,
            List<PontoLucro> serie, List<VendasPlataforma> plataformas, List<EstoquePainel> estoque,
            long calculadas, long produzidas, long vendidas, List<CustoPainel> custos, CadastrosPainel cadastros,
            List<VendasCategoria> categorias, String lucroPlataformas, String variacaoLucro) {}

    @GetMapping("/dashboard")
    public DashboardLoja dashboard(@RequestParam(defaultValue = "30")
            @jakarta.validation.constraints.Pattern(regexp = "7|30|90|tudo") String periodo) {
        var dados = dashboard.buscar(periodo);
        var rotulos = (List<?>) dados.get("rotulosLucro");
        var valores = (List<?>) dados.get("valoresLucro");
        var serie = java.util.stream.IntStream.range(0, rotulos.size())
                .mapToObj(i -> new PontoLucro((String) rotulos.get(i), decimal((BigDecimal) valores.get(i))))
                .toList();
        var plataformas = ((List<?>) dados.get("lucrosPorPlataforma")).stream().map(row -> {
            var item = (java.util.Map<?, ?>) row;
            return new VendasPlataforma((String) item.get("nome"), ((Number) item.get("vendas")).longValue(),
                    decimal((BigDecimal) item.get("lucro")), (String) item.get("cor"));
        }).toList();
        var estoque = ((List<?>) dados.get("estoqueFilamentos")).stream().map(row -> {
            var item = (java.util.Map<?, ?>) row;
            return new EstoquePainel((String) item.get("nome"), decimal((BigDecimal) item.get("gramas")),
                    ((Number) item.get("restante")).intValue(), (String) item.get("cor"));
        }).toList();
        var categorias = ((List<?>) dados.get("vendasCategorias")).stream().map(row -> {
            var item = (java.util.Map<?, ?>) row;
            return new VendasCategoria((String) item.get("nome"), ((Number) item.get("unidades")).longValue());
        }).toList();
        var custos = ((List<?>) dados.get("composicaoCustos")).stream().map(row -> {
            var item = (java.util.Map<?, ?>) row;
            return new CustoPainel((String) item.get("nome"), decimal((BigDecimal) item.get("valor")),
                    ((Number) item.get("percentual")).intValue());
        }).toList();
        var cadastros = new CadastrosPainel(((Number) dados.get("totalPecas")).longValue(),
                ((Number) dados.get("totalFilamentos")).longValue(), ((Number) dados.get("totalImpressoras")).longValue(),
                ((Number) dados.get("totalEmbalagens")).longValue(), compradores.countByArquivadoFalse());
        return new DashboardLoja(decimal((BigDecimal) dados.get("lucroVendido")),
                ((Number) dados.get("totalVendasLoja")).longValue(), decimal((BigDecimal) dados.get("horasProduzidas")),
                decimal((BigDecimal) dados.get("filamentoUsadoKg")), serie, plataformas, estoque,
                ((Number) dados.get("fluxoCalculados")).longValue(), ((Number) dados.get("fluxoProduzidos")).longValue(),
                ((Number) dados.get("fluxoVendidos")).longValue(), custos, cadastros, categorias,
                decimal((BigDecimal) dados.get("lucroPlataformas")), (String) dados.get("textoVariacaoLucro"));
    }

    @GetMapping("/inicio")
    public InicioLoja inicio() {
        var recentes = historico.fetchRecentes(3).stream().map(l -> new Recente(l.id(), l.nomePeca(),
                Boolean.TRUE.equals(l.vendido()) ? "Vendido" : Boolean.TRUE.equals(l.produzido()) ? "Produzido" : "Só cálculo",
                decimal(l.custoTotal()), decimal(l.precoSugerido()), l.unidades())).toList();
        var baixo = filamentos.fetchEstoqueCritico().map(f ->
                new FilamentoCatalogo(f.getId(), f.getNome(), f.getCor(), decimal(f.getEstoqueGramas()), f.getCodigoCor(), decimal(f.getPesoRoloGramas()), f.getMarca(), decimal(f.getPrecoRolo()))).orElse(null);
        var config = configuracoes.buscarOuCriarConfiguracao();
        boolean horaConfigurada = Boolean.TRUE.equals(config.getMaoObraAtivada()) && config.getValorMaoObraHora() != null;
        return new InicioLoja(recentes, Math.max(0, indicadores.contarProduzidos() - indicadores.contarVendidos()), baixo, horaConfigurada);
    }
}
