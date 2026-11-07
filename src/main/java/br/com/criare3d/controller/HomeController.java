
package br.com.criare3d.controller;

import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.PecaService;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import br.com.criare3d.service.interfaces.EmbalagemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class HomeController {

    @Autowired
    private PecaService pecaService;

    @Autowired
    private FilamentoService filamentoService;

    @Autowired
    private ImpressoraService impressoraService;

    @Autowired
    private CalculoHistoricoService calculoHistoricoService;

    @Autowired
    private ConfiguracaoService configuracaoService;

    @Autowired
    private EmbalagemService embalagemService;

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        var historicosRecentes = calculoHistoricoService.fetchRecentes(3);
        long totalProduzidos = calculoHistoricoService.contarProduzidos();
        long totalVendidos = calculoHistoricoService.contarVendidos();

        model.addAttribute("totalPecas", pecaService.contar());
        model.addAttribute("totalFilamentos", filamentoService.contar());
        model.addAttribute("totalImpressoras", impressoraService.contar());
        model.addAttribute("totalEmbalagens", embalagemService.contar());
        model.addAttribute("filamentoAcabando", filamentoService.fetchEstoqueCritico().orElse(null));

        model.addAttribute("totalProduzidos", totalProduzidos);
        model.addAttribute("totalVendidos", totalVendidos);
        model.addAttribute("aguardandoVenda", Math.max(0, totalProduzidos - totalVendidos));
        model.addAttribute("ultimosCalculos", historicosRecentes);

        boolean valorHoraConfigurado = configuracaoService.buscarConfiguracao()
                .map(config -> Boolean.TRUE.equals(config.getMaoObraAtivada())
                        && config.getValorMaoObraHora() != null)
                .orElse(false);
        model.addAttribute("valorHoraConfigurado", valorHoraConfigurado);

        return "index";
    }
}
