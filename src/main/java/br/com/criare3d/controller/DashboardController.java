package br.com.criare3d.controller;

import br.com.criare3d.models.Filamento;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.service.UsuarioLogadoService;
import br.com.criare3d.service.interfaces.EmbalagemService;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import br.com.criare3d.service.interfaces.PecaService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
public class DashboardController {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final CalculoHistoricoRepository historicoRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final PecaService pecaService;
    private final FilamentoService filamentoService;
    private final ImpressoraService impressoraService;
    private final EmbalagemService embalagemService;

    public DashboardController(
            CalculoHistoricoRepository historicoRepository,
            UsuarioLogadoService usuarioLogadoService,
            PecaService pecaService,
            FilamentoService filamentoService,
            ImpressoraService impressoraService,
            EmbalagemService embalagemService) {
        this.historicoRepository = historicoRepository;
        this.usuarioLogadoService = usuarioLogadoService;
        this.pecaService = pecaService;
        this.filamentoService = filamentoService;
        this.impressoraService = impressoraService;
        this.embalagemService = embalagemService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "30") String periodo, Model model) {
        if (!List.of("30", "90", "tudo").contains(periodo)) {
            periodo = "30";
        }

        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        LocalDateTime agora = LocalDateTime.now(fuso);
        LocalDate hoje = agora.toLocalDate();
        LocalDateTime inicio = switch (periodo) {
            case "90" -> hoje.minusDays(89).atStartOfDay();
            case "tudo" -> LocalDateTime.of(1900, 1, 1, 0, 0);
            default -> hoje.minusDays(29).atStartOfDay();
        };
        LocalDateTime fim = hoje.plusDays(1).atStartOfDay();
        var usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        long calculos = historicoRepository.countByUsuario_IdAndDataCalculoGreaterThanEqual(usuarioId, inicio);
        long produzidos = historicoRepository.countByProduzidoTrueAndUsuario_IdAndDataCalculoGreaterThanEqual(usuarioId, inicio);
        long vendidos = historicoRepository.countByVendidoTrueAndUsuario_IdAndDataCalculoGreaterThanEqual(usuarioId, inicio);
        BigDecimal lucroVendido = valor(historicoRepository.somarLucroVendidoEntre(usuarioId, inicio, fim));
        long minutosVendidos = numero(historicoRepository.somarMinutosDasVendasEntre(usuarioId, inicio, fim));
        BigDecimal gramasUsadas = valor(historicoRepository.somarFilamentoProduzidoDesde(usuarioId, inicio));
        BigDecimal lucroPorHora = minutosVendidos <= 0
                ? ZERO
                : lucroVendido.multiply(BigDecimal.valueOf(60))
                .divide(BigDecimal.valueOf(minutosVendidos), 2, RoundingMode.HALF_UP);

        model.addAttribute("periodo", periodo);
        model.addAttribute("totalCalculos", calculos);
        model.addAttribute("totalProduzidos", produzidos);
        model.addAttribute("totalVendidos", vendidos);
        model.addAttribute("aguardandoVenda", Math.max(0, produzidos - vendidos));
        model.addAttribute("percentualProduzido", percentual(produzidos, calculos));
        model.addAttribute("percentualVendido", percentual(vendidos, calculos));
        model.addAttribute("percentualVendidoProduzido", percentual(vendidos, produzidos));
        model.addAttribute("lucroVendido", lucroVendido);
        model.addAttribute("lucroPorHora", lucroPorHora);
        long minutosProduzidos = numero(historicoRepository.somarMinutosProduzidosDesde(usuarioId, inicio));
        model.addAttribute("horasProduzidas", BigDecimal.valueOf(minutosProduzidos)
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP));
        model.addAttribute("filamentoUsadoKg", gramasUsadas
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));

        List<Map<String, Object>> plataformas = montarPlataformas(
                historicoRepository.somarLucroPorPlataformaDesde(usuarioId, inicio));
        model.addAttribute("lucrosPorPlataforma", plataformas);
        BigDecimal lucroPlataformas = plataformas.stream()
                .map(item -> (BigDecimal) item.get("lucro"))
                .reduce(ZERO, BigDecimal::add);
        long vendasPlataformas = plataformas.stream()
                .mapToLong(item -> (long) item.get("vendas")).sum();
        model.addAttribute("lucroPlataformas", lucroPlataformas);
        model.addAttribute("vendasPlataformas", vendasPlataformas);
        model.addAttribute("lucroPlataformaNomes", plataformas.stream().map(item -> item.get("nome")).toList());
        model.addAttribute("lucroPlataformaValores", plataformas.stream().map(item -> item.get("lucro")).toList());
        model.addAttribute("lucroPlataformaCores", plataformas.stream().map(item -> item.get("cor")).toList());
        adicionarSerieLucro(model, usuarioId, periodo, hoje, inicio, fim);
        List<Map<String, Object>> composicao = montarComposicao(
                historicoRepository.somarComposicaoCustosProduzidosDesde(usuarioId, inicio));
        model.addAttribute("composicaoCustos", composicao);
        model.addAttribute("totalCustos", composicao.stream()
                .map(item -> (BigDecimal) item.get("valor"))
                .reduce(ZERO, BigDecimal::add));
        List<Object[]> totaisPorImpressora = historicoRepository.somarHorasPorImpressoraDesde(usuarioId, inicio);
        model.addAttribute("horasPorImpressora", montarImpressoras(totaisPorImpressora));

        model.addAttribute("estoqueFilamentos", montarEstoque(usuarioId));
        model.addAttribute("totalPecas", pecaService.fetchAll(PageRequest.of(0, 1)).getTotalElements());
        model.addAttribute("totalFilamentos", filamentoService.fetchAll(PageRequest.of(0, 1)).getTotalElements());
        long totalImpressoras = impressoraService.fetchAll(PageRequest.of(0, 1)).getTotalElements();
        model.addAttribute("totalImpressoras", totalImpressoras);
        model.addAttribute("totalEmbalagens", embalagemService.fetchAll(PageRequest.of(0, 1)).getTotalElements());
        model.addAttribute("impressorasSemProducao", Math.max(0, totalImpressoras - totaisPorImpressora.size()));
        model.addAttribute("calculosRecentes", historicoRepository.findAllByUsuario_Id(
                usuarioId, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dataCalculo"))).getContent());

        return "dashboard/index";
    }

    private List<Map<String, Object>> montarPlataformas(List<Object[]> linhas) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object[] linha : linhas) {
            BigDecimal lucro = valor(linha[1]);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nome", nomePlataforma((String) linha[0]));
            item.put("lucro", lucro);
            item.put("vendas", numero(linha[2]));
            item.put("cor", corPlataforma((String) linha[0]));
            resultado.add(item);
        }
        resultado.sort(Comparator.comparingInt(item -> ordemPlataforma((String) item.get("nome"))));
        BigDecimal maior = resultado.stream().map(item -> (BigDecimal) item.get("lucro"))
                .max(BigDecimal::compareTo).orElse(ZERO);
        resultado.forEach(item -> item.put("largura", percentual((BigDecimal) item.get("lucro"), maior)));
        return resultado;
    }

    private void adicionarSerieLucro(Model model, UUID usuarioId, String periodo, LocalDate hoje,
                                     LocalDateTime inicio, LocalDateTime fim) {
        List<String> rotulos = new ArrayList<>();
        List<BigDecimal> valores = new ArrayList<>();
        BigDecimal lucroAtual;
        BigDecimal lucroAnterior;
        String comparacao;

        if (!"tudo".equals(periodo)) {
            int dias = "90".equals(periodo) ? 90 : 30;
            Map<LocalDate, BigDecimal> lucroPorDia = new LinkedHashMap<>();
            for (Object[] linha : historicoRepository.somarLucroVendidoPorDia(usuarioId, inicio, fim)) {
                LocalDate data = LocalDate.of((int) numero(linha[0]), (int) numero(linha[1]), (int) numero(linha[2]));
                lucroPorDia.put(data, valor(linha[3]));
            }
            DateTimeFormatter formatoDia = DateTimeFormatter.ofPattern("dd/MM");
            LocalDate primeiroDia = inicio.toLocalDate();
            for (int i = 0; i < dias; i++) {
                LocalDate dia = primeiroDia.plusDays(i);
                rotulos.add(dia.format(formatoDia));
                valores.add(lucroPorDia.getOrDefault(dia, ZERO));
            }
            lucroAtual = valor(historicoRepository.somarLucroVendidoEntre(usuarioId, inicio, fim));
            lucroAnterior = valor(historicoRepository.somarLucroVendidoEntre(
                    usuarioId, inicio.minusDays(dias), inicio));
            comparacao = "vs período anterior";
        } else {
            YearMonth mesAtual = YearMonth.from(hoje);
            LocalDateTime primeiraVenda = historicoRepository.primeiraVendaComLucro(usuarioId);
            YearMonth primeiroMes = primeiraVenda == null
                    ? mesAtual.minusMonths(5)
                    : YearMonth.from(primeiraVenda);
            Map<Integer, BigDecimal> lucroPorMes = new LinkedHashMap<>();
            var linhas = historicoRepository.somarLucroVendidoPorMes(
                    usuarioId, primeiroMes.atDay(1).atStartOfDay(), mesAtual.plusMonths(1).atDay(1).atStartOfDay());
            for (Object[] linha : linhas) {
                int chave = Math.toIntExact(numero(linha[0]) * 100 + numero(linha[1]));
                lucroPorMes.put(chave, valor(linha[2]));
            }

            DateTimeFormatter formatoMes = DateTimeFormatter.ofPattern("MMM/yy", Locale.forLanguageTag("pt-BR"));
            for (YearMonth mes = primeiroMes; !mes.isAfter(mesAtual); mes = mes.plusMonths(1)) {
                String rotulo = mes.format(formatoMes).replace(".", "");
                rotulos.add(rotulo.substring(0, 1).toUpperCase(Locale.ROOT) + rotulo.substring(1));
                valores.add(lucroPorMes.getOrDefault(mes.getYear() * 100 + mes.getMonthValue(), ZERO));
            }
            LocalDateTime inicioMesAtual = mesAtual.atDay(1).atStartOfDay();
            LocalDateTime inicioMesAnterior = mesAtual.minusMonths(1).atDay(1).atStartOfDay();
            lucroAtual = valor(historicoRepository.somarLucroVendidoEntre(usuarioId, inicioMesAtual, fim));
            lucroAnterior = valor(historicoRepository.somarLucroVendidoEntre(usuarioId, inicioMesAnterior, inicioMesAtual));
            comparacao = "vs mês anterior";
        }

        int variacao = lucroAnterior.signum() == 0 ? 0 : lucroAtual.subtract(lucroAnterior)
                .multiply(BigDecimal.valueOf(100)).divide(lucroAnterior, 0, RoundingMode.HALF_UP).intValue();
        String textoVariacao = lucroAnterior.signum() == 0
                ? (lucroAtual.signum() > 0 ? "Novas vendas no período" : "Sem vendas no período anterior")
                : (variacao >= 0 ? "+" : "") + variacao + "% " + comparacao;
        model.addAttribute("rotulosLucro", rotulos);
        model.addAttribute("valoresLucro", valores);
        model.addAttribute("variacaoLucro", variacao);
        model.addAttribute("textoVariacaoLucro", textoVariacao);
        model.addAttribute("descricaoPeriodoGrafico", "tudo".equals(periodo)
                ? "Lucro estimado por mês em todo o histórico"
                : "Lucro estimado por dia no período selecionado");
    }

    private String corPlataforma(String codigo) {
        if (codigo == null) return "#a98a76";
        return switch (codigo) {
            case "TIKTOK", "TIKTOK_SHOP" -> "#492e1e";
            case "SHOPEE" -> "#e9a695";
            case "MERCADO_LIVRE" -> "#a98a76";
            case "NENHUMA" -> "#d9b69c";
            default -> "#816252";
        };
    }

    private int ordemPlataforma(String nome) {
        return switch (nome) {
            case "TikTok Shop" -> 0;
            case "Shopee" -> 1;
            case "Mercado Livre" -> 2;
            case "Sem plataforma" -> 3;
            default -> 3;
        };
    }

    private List<Map<String, Object>> montarComposicao(List<Object[]> consultas) {
        String[] nomes = {"Filamento", "Falhas", "Energia", "Depreciação", "Embalagem", "Mão de obra", "Manutenção"};
        String[] cores = {"#4b2d1d", "#f4d0c5", "#d9b69c", "#c97460", "#eda895", "#816252", "#b18e7b"};
        List<Map<String, Object>> resultado = new ArrayList<>();
        Object[] totais = consultas.isEmpty() ? new Object[0] : consultas.getFirst();
        BigDecimal total = ZERO;
        for (int i = 0; i < nomes.length; i++) {
            total = total.add(valor(i < totais.length ? totais[i] : null));
        }
        for (int i = 0; i < nomes.length; i++) {
            BigDecimal custo = valor(i < totais.length ? totais[i] : null);
            int percentual = total.signum() == 0 ? 0 : custo.multiply(BigDecimal.valueOf(100))
                    .divide(total, 0, RoundingMode.HALF_UP).intValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nome", nomes[i]);
            item.put("cor", cores[i]);
            item.put("valor", custo);
            item.put("percentual", percentual);
            resultado.add(item);
        }
        return resultado;
    }

    private List<Map<String, Object>> montarImpressoras(List<Object[]> linhas) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Object[] linha : linhas) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nome", linha[0] == null ? "Impressora sem nome" : linha[0]);
            item.put("horas", BigDecimal.valueOf(numero(linha[1]))
                    .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP));
            item.put("pecas", numero(linha[2]));
            resultado.add(item);
        }
        return resultado;
    }

    private List<Map<String, Object>> montarEstoque(UUID usuarioId) {
        Map<UUID, Long> usosPorFilamento = new HashMap<>();
        acumularUsos(usosPorFilamento, historicoRepository.contarUsosDeFilamentosEmItens(usuarioId));
        acumularUsos(usosPorFilamento, historicoRepository.contarUsosDeFilamentosSimples(usuarioId));

        List<Filamento> maisUsados = new ArrayList<>(filamentoService.fetchByIds(usosPorFilamento.keySet()));
        maisUsados.sort(Comparator
                .comparingLong((Filamento filamento) -> usosPorFilamento.getOrDefault(filamento.getId(), 0L)).reversed()
                .thenComparing(Filamento::getDataCadastro, Comparator.reverseOrder())
                .thenComparing(Filamento::getNome, String.CASE_INSENSITIVE_ORDER));

        List<Filamento> selecionados = new ArrayList<>(maisUsados.stream().limit(8).toList());
        Set<UUID> idsSelecionados = new HashSet<>();
        selecionados.forEach(filamento -> idsSelecionados.add(filamento.getId()));

        if (selecionados.size() < 8) {
            for (Filamento filamento : filamentoService.fetchRecentes(8)) {
                if (idsSelecionados.add(filamento.getId())) {
                    selecionados.add(filamento);
                    if (selecionados.size() == 8) break;
                }
            }
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Filamento filamento : selecionados) {
            BigDecimal estoque = valor(filamento.getEstoqueGramas());
            BigDecimal pesoRolo = valor(filamento.getPesoRoloGramas());
            int restante = pesoRolo.signum() <= 0 ? 0 : estoque.multiply(BigDecimal.valueOf(100))
                    .divide(pesoRolo, 0, RoundingMode.HALF_UP).intValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nome", filamento.getCor());
            String codigoCor = filamento.getCodigoCor();
            item.put("cor", codigoCor != null && codigoCor.matches("#[0-9A-Fa-f]{6}")
                    ? codigoCor : "#b18e7b");
            item.put("anelCor", restante <= 34 ? "#c4514b" : (restante <= 64 ? "#d7a43b" : "#668d70"));
            item.put("gramas", estoque);
            item.put("restante", Math.max(0, Math.min(100, restante)));
            resultado.add(item);
        }
        return resultado;
    }

    private void acumularUsos(Map<UUID, Long> usos, List<Object[]> linhas) {
        for (Object[] linha : linhas) {
            if (linha[0] instanceof UUID id) {
                usos.merge(id, numero(linha[1]), Long::sum);
            }
        }
    }

    private String nomePlataforma(String codigo) {
        if (codigo == null || codigo.isBlank()) return "Sem plataforma";
        return switch (codigo) {
            case "NENHUMA" -> "Sem plataforma";
            case "SHOPEE" -> "Shopee";
            case "TIKTOK", "TIKTOK_SHOP" -> "TikTok Shop";
            case "MERCADO_LIVRE" -> "Mercado Livre";
            default -> codigo;
        };
    }

    private static BigDecimal valor(Object valor) {
        return valor instanceof BigDecimal decimal ? decimal
                : valor instanceof Number numero ? new BigDecimal(numero.toString())
                : ZERO;
    }

    private static long numero(Object valor) {
        return valor instanceof Number number ? number.longValue() : 0;
    }

    private static int percentual(long quantidade, long total) {
        return total <= 0 ? 0 : (int) Math.round((double) quantidade * 100 / total);
    }

    private static int percentual(BigDecimal quantidade, BigDecimal total) {
        if (total.signum() <= 0) return 0;
        int percentual = quantidade.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP).intValue();
        return Math.max(0, Math.min(100, percentual));
    }
}
