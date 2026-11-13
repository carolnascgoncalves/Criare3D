package br.com.criare3d.controller;
import br.com.criare3d.repository.*;
import br.com.criare3d.service.OutrasVendasService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
@Controller @RequestMapping("/outras-vendas")
public class OutrasVendasController {
    @Autowired private ProdutoLojaRepository produtos;
    @Autowired private VendaProdutoLojaRepository vendas;
    @Autowired private OutrasVendasService service;
    @Autowired private EmbalagemRepository embalagens;
    @Autowired private Clock clock;
    @Getter @Setter public static class ProdutoFormulario {
        private UUID id;
        @NotBlank @Size(min=2,max=150) private String nome;
        @NotNull @DecimalMin("0.00") @DecimalMax("99999999.99") @Digits(integer=8,fraction=2) private BigDecimal preco;
        @DecimalMin("0.00") @DecimalMax("99999999.99") @Digits(integer=8,fraction=2) private BigDecimal custo;
    }
    private ModelAndView pagina(ProdutoFormulario form,int paginaProdutos,int paginaVendas) {
        return new ModelAndView("outras-vendas/index").addObject("produtoFormulario",form)
            .addObject("produtos",produtos.findAllByOrderByNomeAsc(PageRequest.of(Math.max(0,paginaProdutos),10)))
            .addObject("opcoes",produtos.findByArquivadoFalseOrderByNomeAsc(PageRequest.of(0,1000)).getContent())
            .addObject("vendas",vendas.findAllByOrderByDataVendaDesc(PageRequest.of(Math.max(0,paginaVendas),20)))
            .addObject("hoje",LocalDate.now(clock))
            .addObject("embalagens",embalagens.findByArquivadoFalse(PageRequest.of(0,1000)).getContent());
    }
    @GetMapping public ModelAndView abrir(@RequestParam(required=false) UUID editar,
            @RequestParam(defaultValue="0") int paginaProdutos,@RequestParam(defaultValue="0") int paginaVendas) {
        var form=new ProdutoFormulario();
        if(editar!=null) {
            var p=produtos.findById(editar).orElseThrow(()->new IllegalArgumentException("Produto não encontrado."));
            form.setId(p.getId());form.setNome(p.getNome());form.setPreco(p.getPreco());form.setCusto(p.getCusto());
        }
        return pagina(form,paginaProdutos,paginaVendas);
    }
    @PostMapping("/produto") public ModelAndView salvar(@Valid @ModelAttribute("produtoFormulario") ProdutoFormulario form,BindingResult erros) {
        if(erros.hasErrors()) {
            var mv=pagina(form,0,0);mv.addObject("erros",erros.getAllErrors().stream().map(e->e.getDefaultMessage()).toList());mv.setStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);return mv;
        }
        service.salvar(form.getId(),form.getNome(),form.getPreco(),form.getCusto());return new ModelAndView("redirect:/outras-vendas?produtoSalvo");
    }
    @PostMapping("/produto/{id}/estado") public String estado(@PathVariable UUID id,@RequestParam boolean arquivado,RedirectAttributes flash) {
        service.arquivar(id,arquivado);flash.addFlashAttribute("mensagemSucesso",arquivado?"Produto arquivado. Vendas anteriores preservadas.":"Produto reativado.");return "redirect:/outras-vendas";
    }
    @PostMapping("/vender") public String vender(@RequestParam UUID produtoId,@RequestParam int quantidade,
            @RequestParam(required=false) BigDecimal preco,@RequestParam(required=false) BigDecimal custo,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(defaultValue="SEM") br.com.criare3d.models.VendaProdutoLoja.TipoEmbalagem tipoEmbalagem,
            @RequestParam(required=false) UUID embalagemId,@RequestParam(required=false) BigDecimal valorEmbalagem,
            RedirectAttributes flash) {
        try {service.vender(produtoId,quantidade,preco,custo,data,tipoEmbalagem,embalagemId,valorEmbalagem);flash.addFlashAttribute("mensagemSucesso","Venda registrada e incluída na dashboard.");}
        catch(IllegalArgumentException e) {flash.addFlashAttribute("mensagemErro",e.getMessage());}
        return "redirect:/outras-vendas";
    }
    @PostMapping("/venda/{id}/cancelar") public String cancelar(@PathVariable UUID id,@RequestParam int quantidade,RedirectAttributes flash) {
        try {service.cancelar(id,quantidade);flash.addFlashAttribute("mensagemSucesso","Unidades canceladas. Dashboard atualizada.");}
        catch(IllegalArgumentException e) {flash.addFlashAttribute("mensagemErro",e.getMessage());}
        return "redirect:/outras-vendas";
    }
}
