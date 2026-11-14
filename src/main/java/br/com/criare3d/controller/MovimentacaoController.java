package br.com.criare3d.controller;
import br.com.criare3d.repository.MovimentacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.*;
@Controller
public class MovimentacaoController  {
    @Autowired private MovimentacaoRepository movimentos;
    @Autowired private Clock clock;
    @GetMapping("/movimentacoes") public ModelAndView listar(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fim,@RequestParam(defaultValue="")String tipo,@RequestParam(defaultValue="")String busca,@RequestParam(defaultValue="0")int page)  {
        if(inicio==null)inicio=LocalDate.now(clock).minusDays(30);
        if(fim==null)fim=LocalDate.now(clock);
        if(fim.isBefore(inicio))throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial.");
        return new ModelAndView("movimentacoes/index").addObject("inicio",inicio).addObject("fim",fim).addObject("tipo",tipo).addObject("busca",busca)    .addObject("pagina",movimentos.buscar(inicio.atStartOfDay(),fim.plusDays(1).atStartOfDay(),tipo,busca,PageRequest.of(Math.max(0,page),30,Sort.by(Sort.Direction.DESC,"data","id"))));
    }
}
