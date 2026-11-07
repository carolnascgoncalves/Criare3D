
package br.com.criare3d.controller;

import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.PecaService;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        PageRequest pagina = PageRequest.of(0, 1);
        var historicosRecentes = calculoHistoricoService.fetchAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dataCalculo"))
        );
        long totalCalculos = historicosRecentes.getTotalElements();
        long totalProduzidos = calculoHistoricoService.contarProduzidos();
        long totalVendidos = calculoHistoricoService.contarVendidos();

        model.addAttribute("totalPecas",
                pecaService.fetchAll(pagina).getTotalElements());

        model.addAttribute("totalFilamentos",
                filamentoService.fetchAll(pagina).getTotalElements());

        model.addAttribute("totalImpressoras",
                impressoraService.fetchAll(pagina).getTotalElements());

        model.addAttribute("totalCalculos", totalCalculos);
        model.addAttribute("totalProduzidos", totalProduzidos);
        model.addAttribute("totalVendidos", totalVendidos);
        model.addAttribute("percentualProduzido", percentual(totalProduzidos, totalCalculos));
        model.addAttribute("percentualVendido", percentual(totalVendidos, totalCalculos));
        model.addAttribute("ultimosCalculos", historicosRecentes.getContent());

        boolean valorHoraConfigurado = configuracaoService.buscarConfiguracao()
                .map(config -> Boolean.TRUE.equals(config.getMaoObraAtivada())
                        && config.getValorMaoObraHora() != null)
                .orElse(false);
        model.addAttribute("valorHoraConfigurado", valorHoraConfigurado);

        return "index";
    }

    private static int percentual(long quantidade, long total) {
        if (total <= 0) return 0;
        return (int) Math.round((double) quantidade * 100 / total);
    }
}
