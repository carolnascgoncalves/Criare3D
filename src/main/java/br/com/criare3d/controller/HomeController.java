
package br.com.criare3d.controller;

import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.PecaService;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        PageRequest pagina = PageRequest.of(0, 1);

        model.addAttribute("totalPecas",
                pecaService.fetchAll(pagina).getTotalElements());

        model.addAttribute("totalFilamentos",
                filamentoService.fetchAll(pagina).getTotalElements());

        model.addAttribute("totalImpressoras",
                impressoraService.fetchAll(pagina).getTotalElements());

        model.addAttribute("totalCalculos",
                calculoHistoricoService.fetchAll(pagina).getTotalElements());

        return "index";
    }
}
