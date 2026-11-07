
package br.com.criare3d.controller;

import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.PecaService;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import br.com.criare3d.service.interfaces.EmbalagemService;
import br.com.criare3d.models.Filamento;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;

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
        PageRequest pagina = PageRequest.of(0, 1);
        var historicosRecentes = calculoHistoricoService.fetchAll(
                PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "dataCalculo"))
        );
        long totalProduzidos = calculoHistoricoService.contarProduzidos();
        long totalVendidos = calculoHistoricoService.contarVendidos();

        model.addAttribute("totalPecas",
                pecaService.fetchAll(pagina).getTotalElements());

        model.addAttribute("totalFilamentos",
                filamentoService.fetchAll(pagina).getTotalElements());

        model.addAttribute("totalImpressoras",
                impressoraService.fetchAll(pagina).getTotalElements());
        model.addAttribute("totalEmbalagens",
                embalagemService.fetchAll(pagina).getTotalElements());

        Filamento filamentoAcabando = filamentoService.fetchAll(PageRequest.of(
                        0, 200, Sort.by(Sort.Direction.ASC, "estoqueGramas")))
                .getContent().stream()
                .filter(f -> f.getEstoqueGramas() != null
                        && f.getPesoRoloGramas() != null
                        && f.getPesoRoloGramas().compareTo(BigDecimal.ZERO) > 0
                        && f.getEstoqueGramas().multiply(BigDecimal.valueOf(100))
                        .compareTo(f.getPesoRoloGramas().multiply(BigDecimal.TEN)) <= 0)
                .findFirst().orElse(null);
        model.addAttribute("filamentoAcabando", filamentoAcabando);

        model.addAttribute("totalProduzidos", totalProduzidos);
        model.addAttribute("totalVendidos", totalVendidos);
        model.addAttribute("aguardandoVenda", Math.max(0, totalProduzidos - totalVendidos));
        model.addAttribute("ultimosCalculos", historicosRecentes.getContent());

        boolean valorHoraConfigurado = configuracaoService.buscarConfiguracao()
                .map(config -> Boolean.TRUE.equals(config.getMaoObraAtivada())
                        && config.getValorMaoObraHora() != null)
                .orElse(false);
        model.addAttribute("valorHoraConfigurado", valorHoraConfigurado);

        return "index";
    }
}
