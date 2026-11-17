package br.com.criare3d.controller;

import br.com.criare3d.dto.FiltroMovimentacao;
import br.com.criare3d.models.Movimentacao;
import br.com.criare3d.models.enums.TipoMovimentacao;
import br.com.criare3d.service.MovimentacaoService;
import br.com.criare3d.util.FormatoQuantidade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class MovimentacaoController {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR);
    private static final DateTimeFormatter DIA_SEMANA = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR);

    @Autowired
    private MovimentacaoService movimentos;
    @Autowired
    private Clock clock;

    public static class GrupoDia {
        private final String titulo;
        private final List<Movimentacao> itens;

        GrupoDia(String titulo, List<Movimentacao> itens) {
            this.titulo = titulo;
            this.itens = itens;
        }

        public String getTitulo() {
            return titulo;
        }

        public List<Movimentacao> getItens() {
            return itens;
        }
    }

    public static class Atalho {
        private final String rotulo;
        private final LocalDate inicio, fim;
        private final boolean ativo, tudo;

        Atalho(String rotulo, LocalDate inicio, LocalDate fim, boolean ativo, boolean tudo) {
            this.rotulo = rotulo;
            this.inicio = inicio;
            this.fim = fim;
            this.ativo = ativo;
            this.tudo = tudo;
        }

        public String getRotulo() {
            return rotulo;
        }

        public LocalDate getInicio() {
            return inicio;
        }

        public LocalDate getFim() {
            return fim;
        }

        public boolean isAtivo() {
            return ativo;
        }

        public boolean isTudo() {
            return tudo;
        }
    }

    @GetMapping("/movimentacoes")
    public ModelAndView listar(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
        @RequestParam(defaultValue = "") String tipo, @RequestParam(defaultValue = "") String direcao,
        @RequestParam(defaultValue = "") String busca, @RequestParam(required = false) UUID filamento,
        @RequestParam(defaultValue = "false") boolean saldo, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "false") boolean tudo) {
        var hoje = LocalDate.now(clock);
        var filtro = filtro(inicio, fim, tipo, direcao, busca, filamento, saldo, tudo);
        var extrato = movimentos.extrato(filtro,
            PageRequest.of(Math.max(0, page), 30, Sort.by(Sort.Direction.DESC, "data", "id")));
        var entrou = extrato.resumo().stream().filter(r -> r.getEntrou() != null)
            .map(r -> FormatoQuantidade.resumido(r.getEntrou(), r.getMedida(), true))
            .collect(Collectors.joining(" · "));
        var saiu = extrato.resumo().stream().filter(r -> "g".equals(r.getMedida()) && r.getSaiu() != null)
            .map(r -> FormatoQuantidade.resumido(r.getSaiu(), r.getMedida(), false))
            .collect(Collectors.joining(" · "));
        var mv = new ModelAndView("movimentacoes/index")
            .addObject("inicio", filtro.inicio()).addObject("fim", filtro.fim()).addObject("tipo", filtro.tipo())
            .addObject("direcao", filtro.direcao()).addObject("busca", filtro.busca()).addObject("saldo", saldo)
            .addObject("tudo", tudo).addObject("filamento", filamento)
            .addObject("filamentoSelecionado", extrato.filamentoSelecionado()).addObject("pagina", extrato.pagina())
            .addObject("grupos", agrupar(extrato.pagina().getContent(), hoje))
            .addObject("tipos", Arrays.stream(TipoMovimentacao.values()).filter(t -> t != TipoMovimentacao.OUTRO).toList())
            .addObject("pecasVendidas", extrato.pecasVendidas())
            .addObject("resumoEntrou", entrou.isEmpty() ? "—" : entrou)
            .addObject("resumoSaiu", saiu.isEmpty() ? "—" : saiu)
            .addObject("atalhos", atalhos(hoje, filtro.inicio(), filtro.fim(), tudo))
            .addObject("exportacaoLimitada", extrato.pagina().getTotalElements() > MovimentacaoService.LIMITE_CSV);
        var abertura = extrato.abertura();
        if (abertura != null && abertura.getTotal() > 0) {
            mv.addObject("aberturaTotal", abertura.getTotal())
                .addObject("aberturaGramas", FormatoQuantidade.resumido(abertura.getGramas(), "g", false));
        }
        return mv;
    }

    private List<GrupoDia> agrupar(List<Movimentacao> itens, LocalDate hoje) {
        var porDia = new LinkedHashMap<LocalDate, List<Movimentacao>>();
        for (var m : itens) porDia.computeIfAbsent(m.getData().toLocalDate(), d -> new ArrayList<>()).add(m);
        var grupos = new ArrayList<GrupoDia>();
        porDia.forEach((dia, lista) -> {
            String titulo = dia.equals(hoje) ? "Hoje · " + DIA.format(dia)
                : dia.equals(hoje.minusDays(1)) ? "Ontem · " + DIA.format(dia) : DIA_SEMANA.format(dia);
            grupos.add(new GrupoDia(titulo, lista));
        });
        return grupos;
    }

    private List<Atalho> atalhos(LocalDate hoje, LocalDate inicio, LocalDate fim, boolean tudo) {
        var lista = new ArrayList<Atalho>();
        var definicoes = new LinkedHashMap<String, LocalDate>();
        definicoes.put("7 dias", hoje.minusDays(6));
        definicoes.put("30 dias", hoje.minusDays(29));
        definicoes.put("Este mês", hoje.withDayOfMonth(1));
        definicoes.forEach((rotulo, de) -> lista.add(new Atalho(rotulo, de, hoje,
            !tudo && de.equals(inicio) && hoje.equals(fim), false)));
        lista.add(new Atalho("Tudo", hoje.minusDays(29), hoje, tudo, true));
        return lista;
    }

    @GetMapping(value = "/movimentacoes/exportar.csv", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportar(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
        @RequestParam(defaultValue = "") String tipo, @RequestParam(defaultValue = "") String direcao,
        @RequestParam(defaultValue = "") String busca, @RequestParam(required = false) UUID filamento,
        @RequestParam(defaultValue = "false") boolean saldo, @RequestParam(defaultValue = "false") boolean tudo) {
        var exportacao = movimentos.exportar(filtro(inicio, fim, tipo, direcao, busca, filamento, saldo, tudo));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"extrato-movimentacoes.csv\"")
            .header("X-Exportacao-Limitada", Boolean.toString(exportacao.limitada()))
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(ExtratoCsv.gerar(exportacao));
    }

    private FiltroMovimentacao filtro(LocalDate inicio, LocalDate fim, String tipo, String direcao, String busca,
                                      UUID filamento, boolean saldo, boolean tudo) {
        var hoje = LocalDate.now(clock);
        return new FiltroMovimentacao(inicio == null ? hoje.minusDays(29) : inicio, fim == null ? hoje : fim,
            tipo, direcao, busca, filamento, saldo, tudo);
    }
}
