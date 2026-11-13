package br.com.criare3d.controller;

import br.com.criare3d.dto.*;
import br.com.criare3d.repository.*;
import br.com.criare3d.service.*;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.http.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.data.domain.PageRequest;
import java.util.*;

/** Consulta sem persistência, rascunho de sessão ou movimentação de estoque. */
@Controller @RequestMapping("/calculo-rapido")
public class CalculoRapidoController {
    @Autowired private CalculoService calculos;
    @Autowired private ConfiguracaoService configuracoes;
    @Autowired private FilamentoRepository filamentos;
    @Autowired private ImpressoraRepository impressoras;
    @Autowired private EmbalagemRepository embalagens;
    @ModelAttribute("consulta") public CalculoRequest consulta() {
        var c = new CalculoRequest(); c.setPecaId(UUID.randomUUID()); return c;
    }
    @InitBinder("consulta") public void limitar(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(100);
        binder.setAllowedFields("filamentos[*].filamentoId", "filamentos[*].quantidadeFilamentoGramas",
                "unidades", "pecasPorImpressao", "impressoraId", "tempoImpressaoMinutos", "tempoTrabalhoMinutos", "embalagemId", "markupPercentual");
    }
    @GetMapping
    public ModelAndView abrir(jakarta.servlet.http.HttpServletResponse resposta) {
        resposta.setHeader("Cache-Control", "no-store");
        return new ModelAndView("calculo-rapido/index")
                .addObject("configuracao",configuracoes.buscarConfiguracao().orElseGet(ConfiguracaoPadrao::criar))
                .addObject("filamentos",filamentos.findByArquivadoFalse(PageRequest.of(0,1000)).getContent())
                .addObject("impressoras",impressoras.findByArquivadoFalse(PageRequest.of(0,1000)).getContent())
                .addObject("embalagens",embalagens.findByArquivadoFalse(PageRequest.of(0,1000)).getContent());
    }
    @PostMapping @ResponseBody
    public ResponseEntity<?> calcular(@Valid @ModelAttribute("consulta") CalculoRequest consulta, BindingResult erros,
            @RequestParam(defaultValue="Consulta rápida") String nome, @RequestParam(defaultValue="1") int numeroPlacas) {
        if(erros.hasErrors()) return ResponseEntity.unprocessableEntity().cacheControl(CacheControl.noStore()).body(Map.of("erro", "Confira os campos: " + erros.getAllErrors().get(0).getDefaultMessage()));
        try {
            if(numeroPlacas<1 || numeroPlacas>100) throw new IllegalArgumentException("Informe entre 1 e 100 placas por arquivo.");
            int impressoes=Math.multiplyExact(numeroPlacas,consulta.getUnidades());
            var config=configuracoes.buscarConfiguracao().orElseGet(ConfiguracaoPadrao::criar);
            if(Boolean.TRUE.equals(config.getMarkupFixo())) consulta.setMarkupPercentual(config.getMarkupPercentual());
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(CalculoResponse.toDto(calculos.calcularAvulsoTotais(consulta,nome,impressoes)));
        } catch(IllegalArgumentException e) {
            return ResponseEntity.unprocessableEntity().cacheControl(CacheControl.noStore()).body(Map.of("erro", e.getMessage()));
        }
    }
}
