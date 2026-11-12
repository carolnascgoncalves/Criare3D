package br.com.criare3d.controller;
import br.com.criare3d.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.data.domain.PageRequest;
import java.util.*;

@Controller
public class ArquivoCatalogoController {
    @Autowired private PecaRepository pecas;
    @Autowired private FilamentoRepository filamentos;
    @Autowired private ImpressoraRepository impressoras;
    @Autowired private EmbalagemRepository embalagens;
    public record Item(UUID id, String nome, String tipo) {
        public String getRotulo() {
            return switch (tipo) {
                case "peca" -> "Peça";
                case "filamento" -> "Filamento";
                case "impressora" -> "Impressora";
                case "embalagem" -> "Embalagem";
                default -> "Cadastro";
            };
        }
    }
    @GetMapping("/catalogo/arquivados")
    @Transactional(readOnly = true)
    public ModelAndView listar(@RequestParam(defaultValue="0") int page) {
        var pagina = PageRequest.of(Math.max(0, page), 20, org.springframework.data.domain.Sort.by("nome", "id"));
        var itens = new ArrayList<Item>();
        var p = pecas.findByArquivadoTrue(pagina); p.forEach(e -> itens.add(new Item(e.getId(),e.getNome(),"peca")));
        var f = filamentos.findByArquivadoTrue(pagina); f.forEach(e -> itens.add(new Item(e.getId(),e.getNome(),"filamento")));
        var i = impressoras.findByArquivadoTrue(pagina); i.forEach(e -> itens.add(new Item(e.getId(),e.getNome(),"impressora")));
        var b = embalagens.findByArquivadoTrue(pagina); b.forEach(e -> itens.add(new Item(e.getId(),e.getNome(),"embalagem")));
        return new ModelAndView("catalogo/arquivados").addObject("itens",itens).addObject("page",pagina.getPageNumber())
                .addObject("mais",p.hasNext() || f.hasNext() || i.hasNext() || b.hasNext());
    }
    @PostMapping("/catalogo/{tipo}/arquivar-lote")
    @Transactional
    public String arquivarSelecionados(@PathVariable String tipo,@RequestParam(required=false) List<UUID> ids,
            org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        if (!Set.of("peca","filamento","impressora","embalagem").contains(tipo)) return "redirect:/catalogo/arquivados";
        if (ids == null || ids.isEmpty()) {
            flash.addFlashAttribute("mensagemErro","Selecione pelo menos um cadastro para arquivar.");
            return "redirect:/"+tipo+"/listar";
        }
        for (UUID id : ids.stream().filter(Objects::nonNull).distinct().toList()) {
            switch (tipo) {
                case "peca" -> pecas.findById(id).ifPresent(e -> e.setArquivado(true));
                case "filamento" -> filamentos.findById(id).ifPresent(e -> e.setArquivado(true));
                case "impressora" -> impressoras.findById(id).ifPresent(e -> e.setArquivado(true));
                case "embalagem" -> embalagens.findById(id).ifPresent(e -> e.setArquivado(true));
            }
        }
        flash.addFlashAttribute("mensagemSucesso","Cadastros arquivados. Os históricos foram preservados.");
        return "redirect:/"+tipo+"/listar";
    }
    @PostMapping("/catalogo/{tipo}/{id}/arquivar")
    @Transactional
    public String alterar(@PathVariable String tipo,@PathVariable UUID id,@RequestParam(defaultValue="true") boolean arquivar, org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        boolean existe = switch(tipo) {
            case "peca" -> pecas.findById(id).map(e -> {e.setArquivado(arquivar);return true;}).orElse(false);
            case "filamento" -> filamentos.findById(id).map(e -> {e.setArquivado(arquivar);return true;}).orElse(false);
            case "impressora" -> impressoras.findById(id).map(e -> {e.setArquivado(arquivar);return true;}).orElse(false);
            case "embalagem" -> embalagens.findById(id).map(e -> {e.setArquivado(arquivar);return true;}).orElse(false);
            default -> false;
        };
        if (!existe) return "redirect:/catalogo/arquivados";
        flash.addFlashAttribute("mensagemSucesso",arquivar ? "Cadastro arquivado. Os históricos foram preservados." : "Cadastro restaurado.");
        return arquivar ? "redirect:/"+tipo+"/listar" : "redirect:/catalogo/arquivados";
    }
}
