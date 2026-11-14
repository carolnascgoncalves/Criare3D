package br.com.criare3d.service;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.StatusPedido;
import br.com.criare3d.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

/** Transições do pedido e suas movimentações são uma única transação. */
@Service @Transactional
public class OperacaoPedidoService {
 private final PedidoOrcamentoRepository pedidos;
 private final ItemPedidoRepository itens;
 private final CalculoHistoricoRepository historicos;
 private final ImpressoraRepository impressoras;
 private final CalculoService calculos;
 private final HistoricoCalculoService salvarCalculo;
 private final EntradaPedidoService entradas;
 private final PlanejamentoPedidoService planejamento;
 private final ProducaoService producao;
 private final VendaService vendas;
 private final Clock clock;
 private final FotoPedidoService fotos;
 public OperacaoPedidoService(PedidoOrcamentoRepository pedidos,ItemPedidoRepository itens,CalculoHistoricoRepository historicos,ImpressoraRepository impressoras,CalculoService calculos,HistoricoCalculoService salvarCalculo,EntradaPedidoService entradas,PlanejamentoPedidoService planejamento,ProducaoService producao,VendaService vendas,Clock clock,FotoPedidoService fotos) {
  this.fotos=fotos;
  this.pedidos=pedidos;this.itens=itens;this.historicos=historicos;this.impressoras=impressoras;this.calculos=calculos;this.salvarCalculo=salvarCalculo;this.entradas=entradas;this.planejamento=planejamento;this.producao=producao;this.vendas=vendas;this.clock=clock;
 }
 public void aprovar(UUID id,long revisao) {
  var p=bloquear(id,revisao);if(p.isArquivado())throw new IllegalArgumentException("Restaure o orçamento antes de aprovar o pedido.");if(p.getStatus()!=StatusPedido.ENVIADO)throw new IllegalArgumentException("Envie a proposta antes de registrar a aprovação.");
  OrcamentoPedidoService.validarProposta(p);
  // Mesma ordem de recursos em pedidos concorrentes; a fila antiga também bloqueia a impressora.
  p.getItens().stream().filter(i->i.getProduzir()>0).map(ItemPedido::getImpressoraId).distinct().sorted()
   .forEach(imp->impressoras.bloquearPorId(imp).orElseThrow(()->new IllegalArgumentException("Impressora não encontrada.")));
  bloquearLotes(p);
  p.setPrevisao(null);
  for(var i:p.getItens()) {
   if(i.getUsarEstoque()>0) {
    var h=i.getLoteEstoque();
    if(h==null || !Boolean.TRUE.equals(h.getProduzido()) || i.getUsarEstoque()>h.getUnidades()-(h.getUnidadesVendidas()==null?0:h.getUnidadesVendidas())-itens.reservadas(h.getId()))
     throw new IllegalArgumentException("O estoque de "+i.getNome()+" mudou. Volte a proposta para edição e ajuste a quantidade.");
    i.setReservaEstoque(i.getUsarEstoque());itens.flush();
   }
   if(i.getProduzir()>0) {
    var h=calculos.calcularComPrecoFixo(entradas.ler(i,i.getProduzir()),i.getPrecoUnitario());
    BigDecimal custo=h.getCustoTotal().multiply(BigDecimal.valueOf(i.getProduzir()));
    if(i.getUsarEstoque()>0)custo=custo.add(i.getLoteEstoque().getCustoTotal().multiply(BigDecimal.valueOf(i.getUsarEstoque())));
    if(custo.compareTo(i.getCusto())!=0)throw new IllegalArgumentException("Os custos de "+i.getNome()+" mudaram. Volte à edição para conferir antes de aprovar.");
    i.setAquecimento(h.getTempoAquecimentoMinutos());
    i.setLoteProducao(salvarCalculo.create(h));i.setReservaProducao(i.getProduzir());
    fotos.copiarParaCalculo(i.getId(),i.getLoteProducao().getId());
    planejamento.programar(p,i,h);
   }
  }
  if(p.getPrevisao()==null)p.setPrevisao(LocalDateTime.now(clock));
  p.setAprovadoEm(LocalDateTime.now(clock));p.setStatus(pronto(p)?StatusPedido.PRONTO:StatusPedido.APROVADO);p.setRevisao(p.getRevisao()+1);
 }
 public void produzir(UUID id,UUID itemId,long revisao) {
  var p=bloquear(id,revisao);exigirOperacional(p);var i=OrcamentoPedidoService.item(p,itemId);
  if(i.getLoteProducao()==null)throw new IllegalArgumentException("Este item será atendido pelo estoque pronto.");
  var resultado=producao.atualizarProduzido(i.getLoteProducao().getId(),true);
  if(resultado!=ResultadoOperacao.OK)throw new IllegalArgumentException(resultado==ResultadoOperacao.ESTOQUE_INSUFICIENTE?"Estoque de filamento insuficiente. Registre a compra ou ajuste o saldo antes de produzir.":"Não foi possível registrar a produção.");
  if(i.getPlanejamento()!=null)i.getPlanejamento().setConcluido(true);
  p.setStatus(pronto(p)?StatusPedido.PRONTO:StatusPedido.APROVADO);p.setRevisao(p.getRevisao()+1);
 }
 public void pagar(UUID id,long revisao) {
  var p=bloquear(id,revisao);if(!p.isOperacional() && p.getStatus()!=StatusPedido.ENTREGUE)throw new IllegalArgumentException("Aprove o pedido antes de registrar o pagamento.");
  if(p.getPagoEm()!=null)throw new IllegalArgumentException("O pagamento já foi registrado.");
  p.setPagoEm(LocalDateTime.now(clock));p.setRevisao(p.getRevisao()+1);
 }
 public void entregar(UUID id,long revisao) {
  var p=bloquear(id,revisao);exigirOperacional(p);bloquearLotes(p);
  if(!pronto(p))throw new IllegalArgumentException("Confirme a produção dos itens pendentes antes de entregar.");
  if(p.getSubtotal().signum()<=0)throw new IllegalArgumentException("Confira o valor das peças.");
  for(var i:p.getItens()) {i.setReservaEstoque(0);i.setReservaProducao(0);}itens.flush();
  for(var i:p.getItens()) {
   BigDecimal proporcao=i.getPrecoUnitario().divide(p.getSubtotal(),16,RoundingMode.HALF_UP);
   BigDecimal preco=i.getPrecoUnitario().add(p.getFrete().multiply(proporcao)).setScale(8,RoundingMode.HALF_UP);
   BigDecimal desconto=p.getDesconto().multiply(proporcao).setScale(8,RoundingMode.HALF_UP);
   BigDecimal taxas=i.getTaxasUnitarias().add(p.getCustoFrete().multiply(proporcao)).setScale(8,RoundingMode.HALF_UP);
   if(i.getUsarEstoque()>0)vender(p,i.getLoteEstoque(),i.getUsarEstoque(),preco,desconto,taxas);
   if(i.getProduzir()>0)vender(p,i.getLoteProducao(),i.getProduzir(),preco,desconto,taxas);
  }
  p.setStatus(StatusPedido.ENTREGUE);p.setEntregueEm(LocalDateTime.now(clock));p.setRevisao(p.getRevisao()+1);
 }
 private void vender(PedidoOrcamento p,CalculoHistorico h,int q,BigDecimal preco,BigDecimal desconto,BigDecimal taxas) {
  if(vendas.registrarVendaDoPedido(h.getId(),q,preco,desconto,taxas,p.getId())!=ResultadoOperacao.OK)
   throw new IllegalArgumentException("O saldo do lote mudou. Nenhuma entrega foi registrada. Atualize o pedido e confira o estoque.");
 }
 public void cancelar(UUID id,long revisao) {
  var p=bloquear(id,revisao);if(p.getStatus()==StatusPedido.CANCELADO)throw new IllegalArgumentException("Este pedido já foi cancelado.");
  bloquearLotes(p);
  if(p.getStatus()==StatusPedido.ENTREGUE)vendas.cancelarPedido(id);
  for(var i:p.getItens()) {
   i.setReservaEstoque(0);i.setReservaProducao(0);
   if(i.getPlanejamento()!=null){i.getPlanejamento().setCancelado(true);i.getPlanejamento().setConcluido(false);}
  }
  p.setStatus(StatusPedido.CANCELADO);p.setCanceladoEm(LocalDateTime.now(clock));p.setRevisao(p.getRevisao()+1);
 }
 private void bloquearLotes(PedidoOrcamento p) {
  p.getItens().stream().flatMap(i->java.util.stream.Stream.of(i.getLoteEstoque(),i.getLoteProducao())).filter(Objects::nonNull).map(CalculoHistorico::getId).distinct().sorted()
   .forEach(id->historicos.bloquearPorId(id).orElseThrow(()->new IllegalArgumentException("Lote não encontrado.")));
 }
 private PedidoOrcamento bloquear(UUID id,long revisao) {
  var p=pedidos.bloquear(id).orElseThrow(()->new IllegalArgumentException("Pedido não encontrado."));
  if(p.getRevisao()!=revisao)throw new IllegalArgumentException("Este pedido foi atualizado em outra aba. Atualize a página antes de continuar.");return p;
 }
 private void exigirOperacional(PedidoOrcamento p) {if(!p.isOperacional())throw new IllegalArgumentException("A operação exige um pedido aprovado e ainda não entregue.");}
 private boolean pronto(PedidoOrcamento p) {return p.getItens().stream().allMatch(ItemPedido::isPronto);}
}
