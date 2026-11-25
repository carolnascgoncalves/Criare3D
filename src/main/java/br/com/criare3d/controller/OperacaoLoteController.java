package br.com.criare3d.controller;

import br.com.criare3d.repository.*;
import br.com.criare3d.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.UUID;

@Controller
public class OperacaoLoteController {
    @Autowired
    private ProducaoService producaoService;
    @org.springframework.beans.factory.annotation.Autowired
    private VendaService vendaService;
    @Autowired
    private ConsultaLoteService consulta;
    @Autowired
    private FilamentoRepository filamentos;
    @Autowired
    private FalhaProducaoService falhaService;
    @Autowired
    private CustoPersonalizadoService custoService;

    @GetMapping("/vendas-estoque/lote/{id}")
    @Transactional(readOnly = true)
    public ModelAndView abrir(@PathVariable UUID id) {
        var dados=consulta.buscar(id).orElse(null);
        if(dados==null)return new ModelAndView("redirect:/vendas-estoque?erro=naoEncontrado");
        return new ModelAndView("vendas-estoque/lote").addObject("lote", dados.lote()).addObject("reservadas", dados.reservadas())
            .addObject("materiais", dados.materiais()).addObject("resultadoVendas", dados.lucroVendas()).addObject("estimadoVendas", dados.lucroPrevisto())
            .addObject("custosExtras", dados.extras()).addObject("totalExtras", dados.totalExtras()).addObject("vendas", dados.vendas())
            .addObject("falhas", dados.perdas()).addObject("perdaMaterial", dados.perdaMaterial())
            .addObject("filamentos", filamentos.findByArquivadoFalse(PageRequest.of(0, 20)).getContent());
    }

    @PostMapping("/vendas-estoque/lote/{id}/vender")
    public String vender(@PathVariable UUID id, @RequestParam Integer unidades,
                         @RequestParam(required = false) BigDecimal preco, @RequestParam(required = false) BigDecimal desconto,
                         @RequestParam(required = false) BigDecimal taxas, @RequestParam UUID chaveOperacao, RedirectAttributes flash) {
        ResultadoOperacao resultado = vendaService.registrarVenda(id, unidades, preco, desconto, taxas, chaveOperacao);
        if (resultado != ResultadoOperacao.OK) {
            var tentativa = new java.util.LinkedHashMap<String, Object>();
            tentativa.put("unidades", unidades);
            tentativa.put("preco", preco);
            tentativa.put("desconto", desconto);
            tentativa.put("taxas", taxas);
            flash.addFlashAttribute("tentativaVenda3d", tentativa);
        }
        return resposta(id, resultado, flash, "Venda registrada.");
    }

    @PostMapping("/vendas-estoque/lote/{id}/cancelar-venda")
    public String cancelar(@PathVariable UUID id, @RequestParam UUID vendaId, @RequestParam Integer unidades, RedirectAttributes flash) {
        return resposta(id, vendaService.cancelarVenda(id, vendaId, unidades), flash, "Cancelamento registrado; as unidades voltaram ao estoque de peças.");
    }

    @PostMapping("/vendas-estoque/lote/{id}/produzir")
    public String produzir(@PathVariable UUID id, @RequestParam(defaultValue = "true") boolean produzir, RedirectAttributes flash) {
        return resposta(id, producaoService.atualizarProduzido(id, produzir), flash, produzir ? "Produção registrada; material descontado." : "Produção desfeita; material devolvido.");
    }

    @PostMapping("/vendas-estoque/lote/{id}/falha")
    public String falha(@PathVariable UUID id, @RequestParam UUID filamentoId, @RequestParam int unidades,
                        @RequestParam BigDecimal gramas, @RequestParam(defaultValue = "0") long minutos, @RequestParam(defaultValue = "") String observacao, RedirectAttributes flash) {
        try {
            falhaService.registrar(id, filamentoId, unidades, gramas, minutos, observacao);
            flash.addFlashAttribute("mensagemSucesso", "Falha registrada; material perdido descontado do filamento.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/vendas-estoque/lote/" + id;
    }

    @PostMapping("/vendas-estoque/lote/{id}/cancelar-falha")
    public String cancelarFalha(@PathVariable UUID id, @RequestParam UUID falhaId, RedirectAttributes flash) {
        try {
            falhaService.cancelar(id, falhaId);
            flash.addFlashAttribute("mensagemSucesso", "Registro de falha cancelado; material devolvido.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/vendas-estoque/lote/" + id;
    }

    @PostMapping("/vendas-estoque/lote/{id}/custo")
    public String custo(@PathVariable UUID id, @RequestParam String descricao, @RequestParam BigDecimal valor, RedirectAttributes flash) {
        try {
            custoService.registrar(id, descricao, valor);
            flash.addFlashAttribute("mensagemSucesso", "Custo adicionado ao lote.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/vendas-estoque/lote/" + id;
    }

    @PostMapping("/vendas-estoque/lote/{id}/cancelar-custo")
    public String cancelarCusto(@PathVariable UUID id, @RequestParam UUID custoId, RedirectAttributes flash) {
        try {
            custoService.cancelar(id, custoId);
            flash.addFlashAttribute("mensagemSucesso", "Custo cancelado.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("mensagemErro", e.getMessage());
        }
        return "redirect:/vendas-estoque/lote/" + id;
    }

    private String resposta(UUID id, ResultadoOperacao resultado, RedirectAttributes flash, String sucesso) {
        if (ResultadoOperacao.OK.equals(resultado)) flash.addFlashAttribute("mensagemSucesso", sucesso);
        else flash.addFlashAttribute("mensagemErro", switch (resultado) {
            case RESERVA_ATIVA -> "Há unidades reservadas para um pedido. Gerencie a reserva em Orçamentos e pedidos.";
            case PEDIDO_VINCULADO ->
                "Esta venda pertence a um pedido. Cancele pelo pedido para manter os registros consistentes.";
            case PRODUCAO_NECESSARIA ->
                "Registre a produção antes de vender. A venda não produz o lote automaticamente.";
            case ESTOQUE_INSUFICIENTE -> "O estoque de filamento é insuficiente para produzir o lote.";
            case VENDA_ATIVA -> "Este lote tem vendas ativas. Cancele as vendas antes de desfazer a produção.";
            case QUANTIDADE_VENDA_ACIMA_DISPONIVEL -> "A quantidade excede as unidades disponíveis.";
            case VALOR_VENDA_INVALIDO -> "Informe valores positivos e um desconto que não ultrapasse o preço.";
            default -> "Não foi possível concluir. Confira a quantidade e atualize a página.";
        });
        return "redirect:/vendas-estoque/lote/" + id;
    }
}
