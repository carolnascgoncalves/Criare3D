package br.com.criare3d.controller;

import br.com.criare3d.service.DashboardDadosService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DashboardController {
    private final DashboardDadosService dashboard;

    public DashboardController(DashboardDadosService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "30") String periodo, Model model) {
        model.addAllAttributes(dashboard.buscar(periodo));
        return "dashboard/index";
    }
}
