package br.com.criare3d.service;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class PlanejamentoPedidoService {
 private final PlanejamentoProducaoRepository planos;
 private final EntradaPedidoService entradas;
 private final Clock clock;
 public PlanejamentoPedidoService(PlanejamentoProducaoRepository planos,EntradaPedidoService entradas,Clock clock) {this.planos=planos;this.entradas=entradas;this.clock=clock;}
 public record Ocupacao(LocalDateTime inicio,LocalDateTime fim,String nome,UUID impressoraId,String impressora) {}
 public List<Ocupacao> ocupacoes(PedidoOrcamento pedido) {
  return pedido.getItens().stream().filter(i->i.getProduzir()>0).map(ItemPedido::getImpressoraId).filter(Objects::nonNull).distinct()
   .flatMap(id->planos.buscarPendentesDaImpressora(id,org.springframework.data.domain.Pageable.unpaged()).stream())
   .filter(p->p.getInicio()!=null && p.getFim()!=null)
   .map(p->new Ocupacao(p.getInicio(),p.getFim(),p.getNomeImpressora()+" · "+p.getNomePeca(),p.getImpressora().getId(),p.getNomeImpressora())).toList();
 }
 public List<Linha> prever(PedidoOrcamento p) {
  Map<UUID,LocalDateTime> fila=new HashMap<>();List<Linha> linhas=new ArrayList<>();
  for(var i:p.getItens()) {
   if(i.getProduzir()==0)continue;
   if(i.getPlanejamento()!=null) {linhas.add(new Linha(i.getId(),i.getNome(),i.getPlanejamento().getInicio(),i.getPlanejamento().getFim()));continue;}
   boolean anterior=fila.containsKey(i.getImpressoraId());
   var livre=anterior?new InicioLivre(fila.get(i.getImpressoraId()),""):inicioLivre(p,i.getImpressoraId());
   var cursor=anterior?fila.get(i.getImpressoraId()):livre.horario();
   String motivo=anterior?"Após a impressão anterior deste orçamento.":livre.motivo();
   var r=entradas.ler(i,i.getProduzir());
   // Aquecimento foi preservado na fotografia do cálculo dentro da entrada do orçamento.
   var agenda=AgendaPedido.de(p.getTurnoInicio(),p.getTurnoFim(),p.getDias());
   var previsao=agenda.prever(cursor,r.getTempoImpressaoMinutos(),i.getAquecimento(),i.getProduzir(),r.getPecasPorImpressao(),p.getIntervalo(),p.getFolga());
   fila.put(i.getImpressoraId(),previsao.fim().plusMinutes(p.getIntervalo()));linhas.add(new Linha(i.getId(),i.getNome(),previsao.inicio(),previsao.fim(),previsao.impressoes(),motivo+(previsao.inicio().isAfter(cursor)?" Início ajustado ao expediente e aos dias de trabalho.":"")));
  }
  return linhas;
 }
 private InicioLivre inicioLivre(PedidoOrcamento p,UUID impressora) {
  var agora=LocalDateTime.now(clock);
  var inicio=p.getInicioPretendido()==null?agora:p.getInicioPretendido();
  String motivo="Sem impressão anterior na fila desta impressora.";
  if(inicio.isBefore(agora)){inicio=agora;motivo="A data escolhida já passou; previsão a partir de agora.";}
  var ultimo=planos.findFirstByImpressora_IdAndCanceladoFalseAndConcluidoFalseOrderByFimDesc(impressora);
  if(ultimo.isPresent() && ultimo.get().getFim().plusMinutes(p.getIntervalo()).isAfter(inicio)){
   inicio=ultimo.get().getFim().plusMinutes(p.getIntervalo());
   motivo="Aguardando planejamento pendente desta impressora até "+ultimo.get().getFim().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))+" e intervalo de "+p.getIntervalo()+" min.";
  }
  return new InicioLivre(inicio,motivo);
 }
 private record InicioLivre(LocalDateTime horario,String motivo) {}
 public void programar(PedidoOrcamento p,ItemPedido i,CalculoHistorico calculo) {
  var r=entradas.ler(i,i.getProduzir());
  var previa=AgendaPedido.de(p.getTurnoInicio(),p.getTurnoFim(),p.getDias()).prever(inicioLivre(p,i.getImpressoraId()).horario(),r.getTempoImpressaoMinutos(),i.getAquecimento(),i.getProduzir(),r.getPecasPorImpressao(),p.getIntervalo(),p.getFolga());
  var plano=new PlanejamentoProducao();plano.setCalculo(calculo);plano.setImpressora(calculo.getImpressora());plano.setNomePeca(i.getNome());plano.setNomeImpressora(calculo.getNomeImpressora());
  plano.setUnidades(i.getProduzir());plano.setPecasPorImpressao(r.getPecasPorImpressao());plano.setIntervalo(p.getIntervalo());plano.setFolga(p.getFolga());plano.setInicio(previa.inicio());plano.setFim(previa.fim());
  i.setPlanejamento(planos.saveAndFlush(plano));
  if(p.getPrevisao()==null || previa.fim().isAfter(p.getPrevisao()))p.setPrevisao(previa.fim());
 }
 public record Linha(UUID itemId,String nome,LocalDateTime inicio,LocalDateTime fim,List<AgendaPedido.Impressao> impressoes,String observacao) {
  public Linha(UUID id,String nome,LocalDateTime inicio,LocalDateTime fim,List<AgendaPedido.Impressao> impressoes){this(id,nome,inicio,fim,impressoes,"");}
  public Linha(UUID id,String nome,LocalDateTime inicio,LocalDateTime fim){this(id,nome,inicio,fim,List.of(),"");}
 }
}
