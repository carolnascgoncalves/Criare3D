package br.com.criare3d.controller;
import br.com.criare3d.models.Movimentacao;
import br.com.criare3d.models.enums.TipoMovimentacao;
import br.com.criare3d.repository.MovimentacaoRepository;
import br.com.criare3d.util.Csv;
import br.com.criare3d.util.FormatoQuantidade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller @Transactional(readOnly=true)
public class MovimentacaoController  {
    private static final Locale PT_BR=Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DIA=DateTimeFormatter.ofPattern("d 'de' MMMM",PT_BR);
    private static final DateTimeFormatter DIA_SEMANA=DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM",PT_BR);
    private static final DateTimeFormatter DATA_HORA=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",PT_BR);
    private static final int LIMITE_CSV=5000;

    @Autowired private MovimentacaoRepository movimentos;
    @Autowired private Clock clock;
    @Autowired private br.com.criare3d.repository.FilamentoRepository filamentos;

    /** Movimentações de um mesmo dia, para a lista mostrar um cabeçalho por data. */
    public static class GrupoDia {
        private final String titulo; private final List<Movimentacao> itens;
        GrupoDia(String titulo,List<Movimentacao> itens){this.titulo=titulo;this.itens=itens;}
        public String getTitulo(){return titulo;}
        public List<Movimentacao> getItens(){return itens;}
    }
    /** Atalho de período ("7 dias", "Este mês"...). */
    public static class Atalho {
        private final String rotulo; private final LocalDate inicio,fim; private final boolean ativo;
        Atalho(String rotulo,LocalDate inicio,LocalDate fim,boolean ativo){this.rotulo=rotulo;this.inicio=inicio;this.fim=fim;this.ativo=ativo;}
        public String getRotulo(){return rotulo;}
        public LocalDate getInicio(){return inicio;}
        public LocalDate getFim(){return fim;}
        public boolean isAtivo(){return ativo;}
    }

    private String tipoValido(String tipo){return TipoMovimentacao.valido(tipo)?tipo:"";}
    private String direcaoValida(String direcao){return "entrada".equals(direcao)||"saida".equals(direcao)?direcao:"";}

    @GetMapping("/movimentacoes") public ModelAndView listar(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fim,@RequestParam(defaultValue="")String tipo,@RequestParam(defaultValue="")String direcao,@RequestParam(defaultValue="")String busca,@RequestParam(required=false)UUID filamento,@RequestParam(defaultValue="false")boolean saldo,@RequestParam(defaultValue="0")int page)  {
        var hoje=LocalDate.now(clock);
        if(inicio==null)inicio=hoje.minusDays(29);
        if(fim==null)fim=hoje;
        if(fim.isBefore(inicio))throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");
        tipo=tipoValido(tipo);direcao=direcaoValida(direcao);busca=busca.strip();
        var de=inicio.atStartOfDay();var ate=fim.plusDays(1).atStartOfDay();
        var pagina=movimentos.extrato(de,ate,tipo,direcao,saldo,busca,filamento,PageRequest.of(Math.max(0,page),30,Sort.by(Sort.Direction.DESC,"data","id")));
        var resumo=movimentos.resumo(de,ate,tipo,direcao,saldo,busca,filamento);
        var entrou=resumo.stream().filter(r->r.getEntrou()!=null).map(r->FormatoQuantidade.resumido(r.getEntrou(),r.getMedida(),true)).collect(Collectors.joining(" · "));
        var saiu=resumo.stream().filter(r->r.getSaiu()!=null).map(r->FormatoQuantidade.resumido(r.getSaiu(),r.getMedida(),false)).collect(Collectors.joining(" · "));
        var mv=new ModelAndView("movimentacoes/index").addObject("inicio",inicio).addObject("fim",fim).addObject("tipo",tipo).addObject("direcao",direcao).addObject("busca",busca).addObject("saldo",saldo)
            .addObject("filamento",filamento).addObject("filamentoSelecionado",filamento==null?null:filamentos.findById(filamento).orElseThrow()).addObject("pagina",pagina).addObject("grupos",agrupar(pagina.getContent(),hoje)).addObject("tipos",Arrays.stream(TipoMovimentacao.values()).filter(t->t!=TipoMovimentacao.OUTRO).toList())
            .addObject("resumoEntrou",entrou.isEmpty()?"—":entrou).addObject("resumoSaiu",saiu.isEmpty()?"—":saiu).addObject("atalhos",atalhos(hoje,inicio,fim));
        if(!saldo) {
            var abertura=movimentos.saldoAbertura(de,ate,filamento,busca);
            if(abertura!=null && abertura.getTotal()>0)mv.addObject("aberturaTotal",abertura.getTotal()).addObject("aberturaGramas",FormatoQuantidade.resumido(abertura.getGramas(),"g",false));
        }
        return mv;
    }

    private List<GrupoDia> agrupar(List<Movimentacao> itens,LocalDate hoje) {
        var porDia=new LinkedHashMap<LocalDate,List<Movimentacao>>();
        for(var m:itens)porDia.computeIfAbsent(m.getData().toLocalDate(),d->new ArrayList<>()).add(m);
        var grupos=new ArrayList<GrupoDia>();
        porDia.forEach((dia,lista)->{
            String titulo=dia.equals(hoje)?"Hoje · "+DIA.format(dia):dia.equals(hoje.minusDays(1))?"Ontem · "+DIA.format(dia):DIA_SEMANA.format(dia);
            grupos.add(new GrupoDia(titulo,lista));
        });
        return grupos;
    }

    private List<Atalho> atalhos(LocalDate hoje,LocalDate inicio,LocalDate fim) {
        var primeira=movimentos.primeiraData();
        var desde=primeira==null?hoje.minusDays(29):primeira.toLocalDate();
        var lista=new ArrayList<Atalho>();
        var definicoes=new LinkedHashMap<String,LocalDate>();
        definicoes.put("7 dias",hoje.minusDays(6));definicoes.put("30 dias",hoje.minusDays(29));definicoes.put("Este mês",hoje.withDayOfMonth(1));definicoes.put("Tudo",desde);
        definicoes.forEach((rotulo,de)->lista.add(new Atalho(rotulo,de,hoje,de.equals(inicio)&&hoje.equals(fim))));
        return lista;
    }

    @GetMapping(value="/movimentacoes/exportar.csv",produces="text/csv") @ResponseBody
    public ResponseEntity<byte[]> exportar(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fim,@RequestParam(defaultValue="")String tipo,@RequestParam(defaultValue="")String direcao,@RequestParam(defaultValue="")String busca,@RequestParam(required=false)UUID filamento,@RequestParam(defaultValue="false")boolean saldo)  {
        var hoje=LocalDate.now(clock);
        if(inicio==null)inicio=hoje.minusDays(29);
        if(fim==null)fim=hoje;
        if(fim.isBefore(inicio))throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");
        var linhas=movimentos.extrato(inicio.atStartOfDay(),fim.plusDays(1).atStartOfDay(),tipoValido(tipo),direcaoValida(direcao),saldo,busca.strip(),filamento,PageRequest.of(0,LIMITE_CSV,Sort.by(Sort.Direction.DESC,"data","id"))).getContent();
        var csv=new StringBuilder("\uFEFFData;Tipo;Descrição;Quantidade;Unidade;Filamento;Saldo após (g)\r\n");
        for(var m:linhas)csv.append(Csv.campo(DATA_HORA.format(m.getData()))).append(';').append(Csv.campo(m.getTipoInfo().getRotulo())).append(';').append(Csv.campo(m.getDescricao())).append(';')
            .append(Csv.numero(m.getQuantidade().stripTrailingZeros())).append(';').append(Csv.campo(m.getMedida())).append(';').append(Csv.campo(m.getFilamentoId()==null?"":m.getFilamentoId().toString())).append(';').append(Csv.numero(m.getSaldoApos())).append("\r\n");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"extrato-movimentacoes.csv\"")
            .contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
