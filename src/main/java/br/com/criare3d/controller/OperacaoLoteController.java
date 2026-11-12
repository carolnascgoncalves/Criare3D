package br.com.criare3d.controller;
import br.com.criare3d.repository.*;
import br.com.criare3d.service.*;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;
import java.util.UUID;

@Controller
public class OperacaoLoteController {
 @Autowired private CalculoHistoricoService historicos;
 @Autowired private CalculoHistoricoRepository repository;
 @Autowired private VendaCalculoRepository vendas;
 @Autowired private FilamentoRepository filamentos;
 @Autowired private FalhaProducaoRepository falhas;
 @Autowired private FalhaProducaoService falhaService;
 @Autowired private CustoPersonalizadoRepository custos;
 @Autowired private CustoPersonalizadoService custoService;
 public record Material(String nome,BigDecimal gramas,BigDecimal saldo) {}
 @GetMapping("/vendas-estoque/lote/{id}") @Transactional(readOnly=true)
 public ModelAndView abrir(@PathVariable UUID id) {
  var c = repository.findById(id).orElse(null);
  if (c == null) return new ModelAndView("redirect:/vendas-estoque?erro=naoEncontrado");
  var materiais = new java.util.ArrayList<>(c.getItensFilamento().stream().map(i -> new Material(i.getNomeFilamento(),
   i.getQuantidadeFilamentoGramas().add(i.getPurgaGramas() == null ? BigDecimal.ZERO : i.getPurgaGramas()).multiply(BigDecimal.valueOf(c.getUnidades())),
   i.getFilamento() == null ? null : i.getFilamento().getEstoqueGramas())).toList());
  if (materiais.isEmpty()) materiais.add(new Material(c.getNomeFilamento(),
      c.getQuantidadeFilamentoGramas().multiply(BigDecimal.valueOf(c.getUnidades())),
      c.getFilamento() == null ? null : c.getFilamento().getEstoqueGramas()));
  var perdas = falhas.findByCalculo_IdOrderByDataRegistroDesc(id);
  var custosExtras = custos.findByCalculo_IdOrderByDataRegistroDesc(id);
  var totalExtras = custosExtras.stream().filter(custo -> !custo.isCancelado()).map(custo -> custo.getValor()).reduce(BigDecimal.ZERO,BigDecimal::add)
      .add(perdas.stream().filter(f -> !f.isCancelada()).map(f -> f.getCustoMaterial()).reduce(BigDecimal.ZERO,BigDecimal::add));
  var registrosVenda = vendas.findByCalculo_IdOrderByDataVendaDesc(id);
  var estimado = c.getLucroLiquido() != null ? c.getLucroLiquido() : c.getPrecoSugerido().subtract(c.getCustoTotal());
  var resultadoVendas = registrosVenda.stream().map(v -> (v.getLucroUnitario() == null ? estimado : v.getLucroUnitario()).multiply(BigDecimal.valueOf(v.getUnidadesAtivas()))).reduce(BigDecimal.ZERO,BigDecimal::add);
  return new ModelAndView("vendas-estoque/lote").addObject("lote",c).addObject("materiais",materiais)
   .addObject("resultadoVendas",resultadoVendas).addObject("estimadoVendas",estimado.multiply(BigDecimal.valueOf(c.getUnidadesVendidas())))
   .addObject("custosExtras",custosExtras).addObject("totalExtras",totalExtras)
   .addObject("vendas",registrosVenda).addObject("falhas",perdas)
   .addObject("perdaMaterial",perdas.stream().filter(f -> !f.isCancelada()).map(f -> f.getCustoMaterial()).reduce(BigDecimal.ZERO,BigDecimal::add))
   .addObject("filamentos",filamentos.findByArquivadoFalse(PageRequest.of(0,1000)).getContent());
 }
 @PostMapping("/vendas-estoque/lote/{id}/vender")
 public String vender(@PathVariable UUID id,@RequestParam Integer unidades,
  @RequestParam(required=false) BigDecimal preco,@RequestParam(required=false) BigDecimal desconto,
  @RequestParam(required=false) BigDecimal taxas,RedirectAttributes flash) {
  String resultado = historicos.registrarVenda(id,unidades,preco,desconto,taxas);
  return resposta(id,resultado,flash,"Venda registrada.");
 }
 @PostMapping("/vendas-estoque/lote/{id}/cancelar-venda")
 public String cancelar(@PathVariable UUID id,@RequestParam UUID vendaId,@RequestParam Integer unidades,RedirectAttributes flash) {
  return resposta(id,historicos.cancelarVenda(id,vendaId,unidades),flash,"Cancelamento registrado; as unidades voltaram ao estoque de peças.");
 }
 @PostMapping("/vendas-estoque/lote/{id}/produzir")
 public String produzir(@PathVariable UUID id,@RequestParam(defaultValue="true") boolean produzir,RedirectAttributes flash) {
  return resposta(id,historicos.atualizarProduzido(id,produzir),flash,produzir ? "Produção registrada; material descontado." : "Produção desfeita; material devolvido.");
 }
 @PostMapping("/vendas-estoque/lote/{id}/falha")
 public String falha(@PathVariable UUID id,@RequestParam UUID filamentoId,@RequestParam int unidades,
  @RequestParam BigDecimal gramas,@RequestParam(defaultValue="0") long minutos,@RequestParam(defaultValue="") String observacao,RedirectAttributes flash) {
  try { falhaService.registrar(id,filamentoId,unidades,gramas,minutos,observacao); flash.addFlashAttribute("mensagemSucesso","Falha registrada; material perdido descontado do filamento."); }
  catch (IllegalArgumentException e) { flash.addFlashAttribute("mensagemErro",e.getMessage()); }
  return "redirect:/vendas-estoque/lote/"+id;
 }
 @PostMapping("/vendas-estoque/lote/{id}/cancelar-falha")
 public String cancelarFalha(@PathVariable UUID id,@RequestParam UUID falhaId,RedirectAttributes flash) {
  try { falhaService.cancelar(id,falhaId);flash.addFlashAttribute("mensagemSucesso","Registro de falha cancelado; material devolvido."); }
  catch (IllegalArgumentException e) { flash.addFlashAttribute("mensagemErro",e.getMessage()); }
  return "redirect:/vendas-estoque/lote/"+id;
 }
 @PostMapping("/vendas-estoque/lote/{id}/custo")
 public String custo(@PathVariable UUID id,@RequestParam String descricao,@RequestParam BigDecimal valor,RedirectAttributes flash) {
  try { custoService.registrar(id,descricao,valor);flash.addFlashAttribute("mensagemSucesso","Custo adicionado ao lote."); }
  catch (IllegalArgumentException e) { flash.addFlashAttribute("mensagemErro",e.getMessage()); }
  return "redirect:/vendas-estoque/lote/"+id;
 }
 @PostMapping("/vendas-estoque/lote/{id}/cancelar-custo")
 public String cancelarCusto(@PathVariable UUID id,@RequestParam UUID custoId,RedirectAttributes flash) {
  try { custoService.cancelar(id,custoId);flash.addFlashAttribute("mensagemSucesso","Custo cancelado."); }
  catch (IllegalArgumentException e) { flash.addFlashAttribute("mensagemErro",e.getMessage()); }
  return "redirect:/vendas-estoque/lote/"+id;
 }
 private String resposta(UUID id,String resultado,RedirectAttributes flash,String sucesso) {
  if ("ok".equals(resultado)) flash.addFlashAttribute("mensagemSucesso",sucesso);
  else flash.addFlashAttribute("mensagemErro",switch(resultado) {
   case "producaoNecessaria" -> "Registre a produção antes de vender. A venda não produz o lote automaticamente.";
   case "estoqueInsuficiente" -> "O estoque de filamento é insuficiente para produzir o lote.";
   case "vendaAtiva" -> "Este lote tem vendas ativas. Cancele as vendas antes de desfazer a produção.";
   case "quantidadeVendaAcimaDisponivel" -> "A quantidade excede as unidades disponíveis.";
   case "valorVendaInvalido" -> "Informe valores positivos e um desconto que não ultrapasse o preço.";
   default -> "Não foi possível concluir. Confira a quantidade e atualize a página.";
  });
  return "redirect:/vendas-estoque/lote/"+id;
 }
}
