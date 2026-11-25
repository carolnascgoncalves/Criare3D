package br.com.criare3d.controller;

import br.com.criare3d.models.CustoLoja;
import br.com.criare3d.repository.CustoLojaRepository;
import br.com.criare3d.repository.RateioCustoHistoricoRepository;
import br.com.criare3d.service.RateioCustos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Controller
@Transactional
public class CustoLojaController {
    @Autowired
    private CustoLojaRepository custos;
    @Autowired
    private RateioCustoHistoricoRepository rateios;
    @Autowired
    private Clock clock;

    @Getter
    @Setter
    @io.swagger.v3.oas.annotations.media.Schema(name="CustoLojaEntrada")
    public static class Formulario {
        @NotBlank
        @Size(min = 2, max = 150)
        private String descricao;
        @NotNull
        @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
        private LocalDate inicio;
        @NotNull
        @DecimalMin("0.01")
        @DecimalMax("9999999999.99")
        private BigDecimal meta;
        @NotNull
        private CustoLoja.Tipo tipo = CustoLoja.Tipo.VALOR_UNIDADE;
        @DecimalMin("0.01")
        @DecimalMax("9999999999.99")
        private BigDecimal valor;
        @Min(1)
        @Max(10000000)
        private Integer unidadesPrevistas;

        public CustoLoja novo() {
            var c = new CustoLoja();
            c.setDescricao(getDescricao().trim());
            c.setInicio(getInicio());
            c.setMeta(getMeta());
            c.setTipo(getTipo());
            c.setValor(getValor() == null ? BigDecimal.ZERO : getValor());
            c.setUnidadesPrevistas(getUnidadesPrevistas());
            return c;
        }
        public String validarRateio() {
            if (tipo == CustoLoja.Tipo.RATEIO_MENSAL)
                return unidadesPrevistas == null ? "Informe as unidades previstas." : null;
            if (valor == null) return "Informe o valor ou percentual por unidade.";
            return tipo == CustoLoja.Tipo.PERCENTUAL && valor.compareTo(new BigDecimal("100")) > 0
                    ? "Use um percentual de até 100%." : null;
        }
    }

    public record Resumo(CustoLoja custo, BigDecimal recuperado, BigDecimal restante, BigDecimal excedente) {
    }

    @GetMapping("/custos-loja")
    @Transactional(readOnly = true)
    public ModelAndView abrir() {
        var form = new Formulario();
        form.setInicio(LocalDate.now(clock));
        return pagina(form);
    }

    private ModelAndView pagina(Formulario form) {
        var resumos = custos.findAllByOrderByInicioDesc().stream().map(c -> {
            var recuperado = rateios.recuperado(c.getId());
            return new Resumo(c, recuperado, RateioCustos.falta(c.getMeta(), recuperado), recuperado.subtract(c.getMeta()).max(BigDecimal.ZERO));
        }).toList();
        return new ModelAndView("custos-loja/index").addObject("custo", form).addObject("resumos", resumos);
    }

    @PostMapping("/custos-loja")
    public ModelAndView criar(@Valid @ModelAttribute("custo") Formulario form, BindingResult erros) {
        var erro = form.validarRateio();
        if (erro != null)
            erros.rejectValue(form.getTipo() == CustoLoja.Tipo.RATEIO_MENSAL ? "unidadesPrevistas" : "valor", "rateio", erro);
        if (erros.hasErrors()) {
            var mv = pagina(form);
            mv.addObject("erros", erros.getAllErrors().stream().map(e -> e.getDefaultMessage()).toList());
            mv.setStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);
            return mv;
        }
        var c = form.novo();
        custos.save(c);
        return new ModelAndView("redirect:/custos-loja?criado");
    }

    @PostMapping("/custos-loja/{id}/estado")
    public String estado(@PathVariable UUID id, @RequestParam boolean ativo, RedirectAttributes flash) {
        var c = custos.findById(id).orElseThrow(() -> new IllegalArgumentException("Custo nÃ£o encontrado."));
        c.setAtivo(ativo);
        flash.addFlashAttribute("mensagemSucesso", ativo ? "AplicaÃ§Ã£o ativada para novos cÃ¡lculos." : "AplicaÃ§Ã£o pausada. Os cÃ¡lculos anteriores foram preservados.");
        return "redirect:/custos-loja";
    }

    @GetMapping("/custos-loja/{id}")
    @Transactional(readOnly = true)
    public ModelAndView detalhes(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page) {
        var c = custos.findById(id).orElseThrow(() -> new IllegalArgumentException("Custo nÃ£o encontrado."));
        var recuperado = rateios.recuperado(id);
        var itens = rateios.listarPecas(id, PageRequest.of(Math.max(0, page), 20));
        return new ModelAndView("custos-loja/detalhes").addObject("custo", c).addObject("recuperado", recuperado)
            .addObject("restante", RateioCustos.falta(c.getMeta(), recuperado)).addObject("itens", itens.stream().limit(20).toList())
            .addObject("page", Math.max(0, page)).addObject("mais", itens.size() == 20);
    }
}
