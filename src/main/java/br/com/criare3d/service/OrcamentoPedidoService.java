package br.com.criare3d.service;

import br.com.criare3d.dto.*;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.StatusPedido;
import br.com.criare3d.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Service @Transactional
public class OrcamentoPedidoService {
 private final PedidoOrcamentoRepository pedidos;
 private final ItemPedidoRepository itens;
 private final CalculoHistoricoRepository historicos;
 private final CalculoService calculos;
 private final EntradaPedidoService entradas;
 private final Clock clock;
 private final FotoPedidoService fotos;
 private final PlanejamentoPedidoService planejamento;
 public OrcamentoPedidoService(PedidoOrcamentoRepository pedidos,ItemPedidoRepository itens,CalculoHistoricoRepository historicos,CalculoService calculos,EntradaPedidoService entradas,Clock clock,FotoPedidoService fotos,PlanejamentoPedidoService planejamento) {
  this.planejamento=planejamento;
  this.fotos=fotos;
  this.pedidos=pedidos;this.itens=itens;this.historicos=historicos;this.calculos=calculos;this.entradas=entradas;this.clock=clock;
 }
 @Transactional(readOnly=true) public Page<PedidoOrcamento> listar(String busca,int pagina) {return listar(busca,pagina,false);}
 @Transactional(readOnly=true) public Page<PedidoOrcamento> listar(String busca,int pagina,boolean arquivados) {return listar(busca,pagina,arquivados,null);}
 @Transactional(readOnly=true) public Page<PedidoOrcamento> listar(String busca,int pagina,boolean arquivados,StatusPedido status) {
  Page<PedidoOrcamento> resultado=pedidos.buscar(busca==null?"":busca.strip(),arquivados,status,PageRequest.of(Math.max(0,pagina),12));
  resultado.forEach(p->p.getItens().forEach(i->{if(i.getLoteProducao()!=null)i.getLoteProducao().getProduzido();}));
  return resultado;
 }
 @Transactional(readOnly=true) public PedidoOrcamento buscar(UUID id) {
  var p=pedidos.findById(id).orElseThrow(()->new IllegalArgumentException("Orçamento não encontrado."));
  p.getItens().forEach(i->{ if(i.getLoteEstoque()!=null)i.getLoteEstoque().getProduzido();if(i.getLoteProducao()!=null)i.getLoteProducao().getProduzido();if(i.getPlanejamento()!=null)i.getPlanejamento().getFim(); });
  return p;
 }
 @Transactional(readOnly=true) public List<Referencia> referencias() {
  return historicos.findAll(PageRequest.of(0,100,Sort.by(Sort.Direction.DESC,"dataCalculo"))).stream()
   .filter(h->h.getPeca()!=null && h.getImpressora()!=null && !h.getPeca().isArquivado())
   .map(h->new Referencia(h.getId(),h.getNomePeca(),h.getNomeFilamento(),h.getNomeImpressora(),disponivel(h),h.getPeca().getId())).toList();
 }
 public PedidoOrcamento adicionarVarios(UUID id,SelecaoPedidoRequest d) {
  var selecionados=d.getItens().stream().filter(SelecaoPedidoRequest.Item::isSelecionado).toList();
  if(selecionados.isEmpty()||selecionados.size()>20)throw new IllegalArgumentException("Selecione de 1 a 20 peças para adicionar.");
  var p=id==null?novo():editavel(id);if(p.getItens().size()+selecionados.size()>20)throw new IllegalArgumentException("Use até 20 itens por orçamento.");
  if(id==null){p.setCliente(d.getCliente());p.setContato(d.getContato());}
  for(var i:selecionados){if(i.getReferencia()==null)throw new IllegalArgumentException("Selecione um cálculo válido.");adicionar(p,i.getReferencia(),i.getQuantidade(),i.getEstoque());}if(id==null && d.getTitulo()!=null&&!d.getTitulo().isBlank()&&!d.getTitulo().equals("Novo orçamento"))p.setTitulo(d.getTitulo().strip());return p;
 }
 public PedidoOrcamento iniciar(UUID referencia,int quantidade,int estoque) {
  var p=novo();adicionar(p,referencia,quantidade,estoque);return p;
 }
 public PedidoOrcamento doCalculo(CalculoRequest request) {
  var p=novo();String json=entradas.escrever(request);int quantidade=request.getUnidades()==null?1:request.getUnidades();
  adicionarEntrada(p,json,quantidade,0,null);pedidos.saveAndFlush(p);return p;
 }
 public ItemPedido adicionarCalculo(UUID id,CalculoRequest request) {
  var p=editavel(id);
  var i=adicionarEntrada(p,entradas.escrever(request),request.getUnidades()==null?1:request.getUnidades(),0,null);
  pedidos.saveAndFlush(p);return i;
 }
 private PedidoOrcamento novo() {
  var p=new PedidoOrcamento();p.setCriadoEm(LocalDateTime.now(clock));p.setInicioPretendido(LocalDateTime.now(clock));return pedidos.save(p);
 }
 public void adicionar(UUID id,UUID referencia,int quantidade,int estoque) { adicionar(editavel(id),referencia,quantidade,estoque); }
 private void adicionar(PedidoOrcamento p,UUID referencia,int quantidade,int estoque) {
  var h=historicos.buscarPorIdComItens(referencia).orElseThrow(()->new IllegalArgumentException("Cálculo de referência não encontrado."));
  if(estoque>disponivel(h)) throw new IllegalArgumentException("O lote não tem essa quantidade disponível. Reduza o estoque utilizado.");
  var item=adicionarEntrada(p,entradas.escrever(entradas.copiar(h)),quantidade,estoque,h);
  pedidos.saveAndFlush(p);fotos.copiar(item.getId(),h.getId());
 }
 private ItemPedido adicionarEntrada(PedidoOrcamento p,String json,int quantidade,int estoque,CalculoHistorico referencia) {
  p.setRevisao(p.getRevisao()+1);
  if(p.getItens().size()>=20)throw new IllegalArgumentException("Use até 20 itens por orçamento.");
  validarQuantidade(quantidade,estoque);
  var r=entradas.ler(json,quantidade);var h=calculos.calcular(r);
  var i=new ItemPedido();i.setPedido(p);i.setOrdem(p.getItens().stream().mapToInt(ItemPedido::getOrdem).max().orElse(-1)+1);
  i.setAquecimento(h.getTempoAquecimentoMinutos()==null?0:h.getTempoAquecimentoMinutos());i.setNome(h.getNomePeca());i.setPecaId(r.getPecaId());i.setImpressoraId(r.getImpressoraId());i.setEntradaJson(json);
  i.setMateriais(h.getItensFilamento().stream().map(m->m.getNomeFilamento()+(m.getCorFilamento()==null?"":" · "+m.getCorFilamento())).distinct().reduce((a,b)->a+" / "+b).orElse(h.getNomeFilamento()));
  if(i.getMateriais().length()>300)i.setMateriais(i.getMateriais().substring(0,300));
  i.setQuantidade(quantidade);i.setUsarEstoque(estoque);i.setLoteEstoque(referencia);
  // O cálculo selecionado pode ter preço personalizado ou condições antigas.
  // Seu preço já inclui o lucro e deve ser o ponto de partida da proposta.
  var origemPreco=referencia==null?h:referencia;
  i.setPrecoUnitario((origemPreco.getPrecoVendaPlataforma()==null?origemPreco.getPrecoSugerido():origemPreco.getPrecoVendaPlataforma()).setScale(2,RoundingMode.HALF_UP));
  if(i.getPrecoUnitario().signum()<=0)throw new IllegalArgumentException("Configure um preço maior que zero antes de adicionar a peça.");
  i.setTaxasUnitarias(h.getTaxasPlataforma()==null?BigDecimal.ZERO:h.getTaxasPlataforma());
  atualizarCusto(i);atualizarTaxas(i);p.getItens().add(i);
  if(p.getItens().size()==1)p.setTitulo(("Orçamento · "+h.getNomePeca()).substring(0,Math.min(150,12+h.getNomePeca().length())));
  return itens.saveAndFlush(i);
 }
 public void alterarItem(UUID id,UUID itemId,int quantidade,int estoque,BigDecimal preco) {
  var p=editavel(id);var i=item(p,itemId);validarQuantidade(quantidade,estoque);
  validarDinheiro(preco);if(preco.signum()==0)throw new IllegalArgumentException("Informe um preço maior que zero.");
  if(estoque>0 && (i.getLoteEstoque()==null || estoque>disponivel(i.getLoteEstoque())))throw new IllegalArgumentException("O lote não tem a quantidade disponível solicitada.");
  i.setQuantidade(quantidade);i.setUsarEstoque(estoque);i.setPrecoUnitario(preco);atualizarCusto(i);
  atualizarTaxas(i);p.setRevisao(p.getRevisao()+1);
  if(p.getDesconto().compareTo(p.getSubtotal())>0)p.setDesconto(BigDecimal.ZERO);
 }
 private void atualizarTaxas(ItemPedido i) {
  var h=calculos.calcularComPrecoFixo(entradas.ler(i.getEntradaJson(),i.getQuantidade()),i.getPrecoUnitario());
  i.setTaxasUnitarias(h.getTaxasPlataforma()==null?BigDecimal.ZERO:h.getTaxasPlataforma());
 }
 private void atualizarCusto(ItemPedido i) {
  BigDecimal custo=BigDecimal.ZERO;long minutos=0;
  if(i.getUsarEstoque()>0) custo=i.getLoteEstoque().getCustoTotal().multiply(BigDecimal.valueOf(i.getUsarEstoque()));
  if(i.getProduzir()>0) {var h=calculos.calcular(entradas.ler(i.getEntradaJson(),i.getProduzir()));custo=custo.add(h.getCustoTotal().multiply(BigDecimal.valueOf(i.getProduzir())));minutos=h.getMinutosMaquinaLote();}
  i.setCusto(custo);i.setMinutosProducao(minutos);
 }
 public void remover(UUID id,UUID itemId) { var p=editavel(id);p.getItens().remove(item(p,itemId));p.setRevisao(p.getRevisao()+1);if(p.getDesconto().compareTo(p.getSubtotal())>0)p.setDesconto(BigDecimal.ZERO); }
 public void salvarDados(UUID id,PedidoDadosRequest d) {
  var p=editavel(id);AgendaPedido.de(d.getTurnoInicio(),d.getTurnoFim(),d.getDias());
  validarDinheiro(d.getDesconto());
  var desconto=DescontoPedido.interpretar(d.getDescontoInformado()==null||d.getDescontoInformado().isBlank()?d.getDesconto().toPlainString():d.getDescontoInformado(),p.getSubtotal());
  d.setDesconto(desconto.valor());
  validarDinheiro(d.getDesconto());validarDinheiro(d.getFrete());validarDinheiro(d.getCustoFrete());
  if(d.getDesconto().compareTo(p.getSubtotal())>0)throw new IllegalArgumentException("O desconto não pode ultrapassar o valor das peças.");
  p.setTitulo(d.getTitulo().strip());p.setCliente(d.getCliente());p.setContato(d.getContato());p.setObservacoes(d.getObservacoes());
  p.setInicioPretendido(d.getInicioPretendido()==null?LocalDateTime.now(clock):d.getInicioPretendido());p.setTurnoInicio(d.getTurnoInicio());p.setTurnoFim(d.getTurnoFim());p.setDias(d.getDias());p.setIntervalo(d.getIntervalo());p.setFolga(d.getFolga());
  p.setDesconto(d.getDesconto());p.setDescontoPercentual(desconto.percentual());p.setFrete(d.getFrete());p.setCustoFrete(d.getCustoFrete());p.setRevisao(p.getRevisao()+1);
 }
 public void salvarCliente(UUID id,PedidoClienteRequest d) {
  var p=editavel(id);p.setTitulo(d.getTitulo().strip());p.setCliente(d.getCliente());p.setContato(d.getContato());p.setObservacoes(d.getObservacoes());p.setRevisao(p.getRevisao()+1);
 }
 public void aplicarFrete(UUID id,long revisao,BigDecimal preco){var p=editavel(id);if(p.getRevisao()!=revisao)throw new IllegalArgumentException("O orçamento foi alterado. Consulte o frete novamente.");validarDinheiro(preco);p.setCustoFrete(preco);p.setFrete(preco);p.setRevisao(p.getRevisao()+1);}
 public void enviar(UUID id) {var p=editavel(id);validarProposta(p);p.setPrevisao(planejamento.prever(p).stream().map(PlanejamentoPedidoService.Linha::fim).max(LocalDateTime::compareTo).orElse(LocalDateTime.now(clock)));p.setRevisao(p.getRevisao()+1);p.setStatus(StatusPedido.ENVIADO);p.setEnviadoEm(LocalDateTime.now(clock));}
 public void revisar(UUID id) {var p=pedidos.bloquear(id).orElseThrow();if(p.getStatus()!=StatusPedido.ENVIADO)throw new IllegalArgumentException("Somente propostas enviadas podem voltar à edição.");p.getItens().forEach(i->{atualizarCusto(i);atualizarTaxas(i);});p.setRevisao(p.getRevisao()+1);p.setStatus(StatusPedido.RASCUNHO);}
 public void arquivar(UUID id,boolean arquivar) {
  var p=pedidos.bloquear(id).orElseThrow(()->new IllegalArgumentException("Orçamento não encontrado."));
  if(arquivar && p.isOperacional())throw new IllegalArgumentException("Conclua ou cancele o pedido antes de arquivar, pois ele ainda pode ter reservas e produção pendente.");
  p.setArquivado(arquivar);p.setRevisao(p.getRevisao()+1);
 }
 public PedidoOrcamento editavel(UUID id) {var p=pedidos.bloquear(id).orElseThrow(()->new IllegalArgumentException("Orçamento não encontrado."));if(p.isArquivado())throw new IllegalArgumentException("Restaure o orçamento na aba Arquivados antes de editar.");if(!p.getStatus().isEditavel())throw new IllegalArgumentException("Volte a proposta para rascunho antes de editar. Pedidos aprovados preservam os valores combinados.");return p;}
 public static void validarProposta(PedidoOrcamento p) {if(p.getItens().isEmpty())throw new IllegalArgumentException("Adicione ao menos uma peça.");if(p.getCliente()==null||p.getCliente().isBlank())throw new IllegalArgumentException("Informe o nome do cliente antes de enviar ou aprovar.");}
 public static ItemPedido item(PedidoOrcamento p,UUID id) {return p.getItens().stream().filter(i->i.getId().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Item não encontrado neste orçamento."));}
 public long disponivel(CalculoHistorico h) {return Boolean.TRUE.equals(h.getProduzido())?Math.max(0,h.getUnidades()-(h.getUnidadesVendidas()==null?0:h.getUnidadesVendidas())-itens.reservadas(h.getId())):0;}
 private void validarQuantidade(int q,int e) {if(q<1||q>500||e<0||e>q)throw new IllegalArgumentException("Use de 1 a 500 unidades por item e estoque entre zero e a quantidade solicitada.");}
 public static void validarDinheiro(BigDecimal valor) {if(valor==null||valor.signum()<0||valor.compareTo(new BigDecimal("99999999"))>0||valor.stripTrailingZeros().scale()>2)throw new IllegalArgumentException("Informe valores válidos com até duas casas decimais.");}
 @Transactional(readOnly=true) public MateriaisPedido.Resumo materiais(PedidoOrcamento p) {
  List<CalculoHistorico> lotes=new ArrayList<>();
  if(p.getStatus()!=StatusPedido.CANCELADO)for(var i:p.getItens())if(i.getProduzir()>0&&!i.isPronto()) {
   // O pedido veio de outra transação: nunca percorra a coleção do lote desconectado.
   lotes.add(i.getLoteProducao()!=null
    ?historicos.buscarPorIdComItens(i.getLoteProducao().getId()).orElseThrow(()->new IllegalArgumentException("Lote de produção não encontrado."))
    :calculos.calcular(entradas.ler(i.getEntradaJson(),i.getProduzir())));
  }
  return MateriaisPedido.resumir(lotes);
 }
 public record Referencia(UUID id,String nome,String material,String impressora,long disponivel,UUID pecaId) {}
}
