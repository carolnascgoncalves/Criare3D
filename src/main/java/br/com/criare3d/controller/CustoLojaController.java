package br.com.criare3d.controller;

import br.com.criare3d.models.CustoLoja;
import br.com.criare3d.repository.*;
import br.com.criare3d.service.RateioCustos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Controller @Transactional
public class CustoLojaController {
    @Autowired private CustoLojaRepository custos;
    @Autowired private RateioCustoHistoricoRepository rateios;
    @Autowired private Clock clock;
    @Getter @Setter
    public static class Formulario {
        @NotBlank @Size(min=2,max=150) private String descricao;
        @NotNull @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) private LocalDate inicio;
        @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") private BigDecimal meta;
        @NotNull private CustoLoja.Tipo tipo = CustoLoja.Tipo.VALOR_UNIDADE;
        @DecimalMin("0.01") @DecimalMax("9999999999.99") private BigDecimal valor;
        @Min(1) @Max(10000000) private Integer unidadesPrevistas;
    }
    public record Resumo(CustoLoja custo, BigDecimal recuperado, BigDecimal restante, BigDecimal excedente) {}
    @GetMapping("/custos-loja") @Transactional(readOnly=true)
    public ModelAndView abrir() {
        var form = new Formulario(); form.setInicio(LocalDate.now(clock));
        return pagina(form);
    }
    private ModelAndView pagina(Formulario form) {
        var resumos = custos.findAllByOrderByInicioDesc().stream().map(c -> {
            var recuperado = rateios.recuperado(c.getId());
            return new Resumo(c, recuperado, RateioCustos.falta(c.getMeta(), recuperado), recuperado.subtract(c.getMeta()).max(BigDecimal.ZERO));
        }).toList();
        return new ModelAndView("custos-loja/index").addObject("custo",form).addObject("resumos",resumos);
    }
    @PostMapping("/custos-loja")
    public ModelAndView criar(@Valid @ModelAttribute("custo") Formulario form, BindingResult erros) {
        if (form.getTipo() == CustoLoja.Tipo.RATEIO_MENSAL) {
            if(form.getUnidadesPrevistas()==null) erros.rejectValue("unidadesPrevistas","obrigatorio","Informe as unidades previstas.");
        } else if (form.getValor()==null) erros.rejectValue("valor","obrigatorio","Informe o valor ou percentual por unidade.");
        if (form.getTipo()==CustoLoja.Tipo.PERCENTUAL && form.getValor()!=null && form.getValor().compareTo(new BigDecimal("100"))>0)
            erros.rejectValue("valor","limite","Use um percentual de atÃ© 100%.");
        if(erros.hasErrors()) {
            var mv=pagina(form); mv.addObject("erros",erros.getAllErrors().stream().map(e -> e.getDefaultMessage()).toList()); mv.setStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY); return mv;
        }
        var c = new CustoLoja(); c.setDescricao(form.getDescricao().trim());c.setInicio(form.getInicio());
        c.setMeta(form.getMeta());c.setTipo(form.getTipo());c.setValor(form.getValor()==null?BigDecimal.ZERO:form.getValor());
        c.setUnidadesPrevistas(form.getUnidadesPrevistas());custos.save(c);
        return new ModelAndView("redirect:/custos-loja?criado");
    }
    @PostMapping("/custos-loja/{id}/estado")
    public String estado(@PathVariable UUID id, @RequestParam boolean ativo, RedirectAttributes flash) {
        var c=custos.findById(id).orElseThrow(() -> new IllegalArgumentException("Custo nÃ£o encontrado."));
        c.setAtivo(ativo);flash.addFlashAttribute("mensagemSucesso",ativo?"AplicaÃ§Ã£o ativada para novos cÃ¡lculos.":"AplicaÃ§Ã£o pausada. Os cÃ¡lculos anteriores foram preservados.");
        return "redirect:/custos-loja";
    }
    @GetMapping("/custos-loja/{id}") @Transactional(readOnly=true)
    public ModelAndView detalhes(@PathVariable UUID id,@RequestParam(defaultValue="0") int page) {
        var c=custos.findById(id).orElseThrow(() -> new IllegalArgumentException("Custo nÃ£o encontrado."));
        var recuperado=rateios.recuperado(id);
        var itens=rateios.listarPecas(id,PageRequest.of(Math.max(0,page),20));
        return new ModelAndView("custos-loja/detalhes").addObject("custo",c).addObject("recuperado",recuperado)
            .addObject("restante",RateioCustos.falta(c.getMeta(),recuperado)).addObject("itens",itens.stream().limit(20).toList())
            .addObject("page",Math.max(0,page)).addObject("mais",itens.size()==20);
    }
}
