
package br.com.criare3d.controller;

import br.com.criare3d.service.HistoricoCalculoService;
import br.com.criare3d.service.IndicadoresService;


import br.com.criare3d.service.interfaces.PecaService;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import br.com.criare3d.service.interfaces.EmbalagemService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;


@Controller
public class HomeController {

    private final PecaService pecaService;
    private final FilamentoService filamentoService;
    private final ImpressoraService impressoraService;
    private final HistoricoCalculoService historicoCalculoService;
    private final IndicadoresService indicadoresService;
    private final ConfiguracaoService configuracaoService;
    private final EmbalagemService embalagemService;

    public HomeController(PecaService pecaService,
                          FilamentoService filamentoService,
                          ImpressoraService impressoraService,
                          HistoricoCalculoService historicoCalculoService, IndicadoresService indicadoresService,
                          ConfiguracaoService configuracaoService,
                          EmbalagemService embalagemService) {
        this.pecaService = pecaService;
        this.filamentoService = filamentoService;
        this.impressoraService = impressoraService;
        this.historicoCalculoService = historicoCalculoService;
        this.indicadoresService = indicadoresService;
        this.configuracaoService = configuracaoService;
        this.embalagemService = embalagemService;
    }

    @GetMapping("/favicon.ico")
    @ResponseBody
    public ResponseEntity<Void> faviconSemArquivo() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        var historicosRecentes = historicoCalculoService.fetchRecentes(3);
        long totalProduzidos = indicadoresService.contarProduzidos();
        long totalVendidos = indicadoresService.contarVendidos();

        long totalPecas = pecaService.contar();
        long totalFilamentos = filamentoService.contar();
        long totalImpressoras = impressoraService.contar();
        long totalEmbalagens = embalagemService.contar();
        model.addAttribute("totalPecas", totalPecas);
        model.addAttribute("totalFilamentos", totalFilamentos);
        model.addAttribute("totalImpressoras", totalImpressoras);
        model.addAttribute("totalEmbalagens", totalEmbalagens);
        model.addAttribute("filamentoAcabando", filamentoService.fetchEstoqueCritico().orElse(null));

        model.addAttribute("totalProduzidos", totalProduzidos);
        model.addAttribute("totalVendidos", totalVendidos);
        model.addAttribute("aguardandoVenda", Math.max(0, totalProduzidos - totalVendidos));
        model.addAttribute("ultimosCalculos", historicosRecentes);

        var configuracao = configuracaoService.buscarOuCriarConfiguracao();
        boolean tarifaConfigurada = configuracao.getTarifaEnergiaKwh() != null
                && configuracao.getTarifaEnergiaKwh().signum() > 0;
        boolean markupFixoConfigurado = !Boolean.TRUE.equals(configuracao.getMarkupFixo())
                || (configuracao.getMarkupPercentual() != null
                && configuracao.getMarkupPercentual().signum() > 0);
        model.addAttribute("configuracaoBasicaOk", tarifaConfigurada && markupFixoConfigurado);
        model.addAttribute("tarifaEnergiaConfigurada", tarifaConfigurada);
        model.addAttribute("markupFixoConfigurado", markupFixoConfigurado);
        model.addAttribute("primeiroAcessoConcluido", tarifaConfigurada
                && totalPecas > 0 && totalFilamentos > 0
                && totalImpressoras > 0
                && !historicosRecentes.isEmpty());

        boolean valorHoraConfigurado = Boolean.TRUE.equals(configuracao.getMaoObraAtivada())
                && configuracao.getValorMaoObraHora() != null;
        model.addAttribute("valorHoraConfigurado", valorHoraConfigurado);

        return "index";
    }
}
