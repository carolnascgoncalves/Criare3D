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
 public List<Linha> prever(PedidoOrcamento p) {
  Map<UUID,LocalDateTime> fila=new HashMap<>();List<Linha> linhas=new ArrayList<>();
  for(var i:p.getItens()) {
   if(i.getProduzir()==0)continue;
   if(i.getPlanejamento()!=null) {linhas.add(new Linha(i.getId(),i.getNome(),i.getPlanejamento().getInicio(),i.getPlanejamento().getFim()));continue;}
   var cursor=fila.computeIfAbsent(i.getImpressoraId(),id->inicioLivre(p,id));
   var r=entradas.ler(i.getEntradaJson(),i.getProduzir());
   // Aquecimento foi preservado na fotografia do cálculo dentro da entrada do orçamento.
   var agenda=AgendaPedido.de(p.getTurnoInicio(),p.getTurnoFim(),p.getDias());
   var previsao=agenda.prever(cursor,r.getTempoImpressaoMinutos(),i.getAquecimento(),i.getProduzir(),r.getPecasPorImpressao(),p.getIntervalo(),p.getFolga());
   fila.put(i.getImpressoraId(),previsao.fim().plusMinutes(p.getIntervalo()));linhas.add(new Linha(i.getId(),i.getNome(),previsao.inicio(),previsao.fim(),previsao.impressoes()));
  }
  return linhas;
 }
 private LocalDateTime inicioLivre(PedidoOrcamento p,UUID impressora) {
  var inicio=p.getInicioPretendido()==null?LocalDateTime.now(clock):p.getInicioPretendido();
  if(inicio.isBefore(LocalDateTime.now(clock)))inicio=LocalDateTime.now(clock);
  var ultimo=planos.findFirstByImpressora_IdAndCanceladoFalseAndConcluidoFalseOrderByFimDesc(impressora);
  return ultimo.isPresent() && ultimo.get().getFim().plusMinutes(p.getIntervalo()).isAfter(inicio)?ultimo.get().getFim().plusMinutes(p.getIntervalo()):inicio;
 }
 public void programar(PedidoOrcamento p,ItemPedido i,CalculoHistorico calculo) {
  var r=entradas.ler(i.getEntradaJson(),i.getProduzir());
  var previa=AgendaPedido.de(p.getTurnoInicio(),p.getTurnoFim(),p.getDias()).prever(inicioLivre(p,i.getImpressoraId()),r.getTempoImpressaoMinutos(),i.getAquecimento(),i.getProduzir(),r.getPecasPorImpressao(),p.getIntervalo(),p.getFolga());
  var plano=new PlanejamentoProducao();plano.setCalculo(calculo);plano.setImpressora(calculo.getImpressora());plano.setNomePeca(i.getNome());plano.setNomeImpressora(calculo.getNomeImpressora());
  plano.setUnidades(i.getProduzir());plano.setPecasPorImpressao(r.getPecasPorImpressao());plano.setIntervalo(p.getIntervalo());plano.setFolga(p.getFolga());plano.setInicio(previa.inicio());plano.setFim(previa.fim());
  i.setPlanejamento(planos.saveAndFlush(plano));
  if(p.getPrevisao()==null || previa.fim().isAfter(p.getPrevisao()))p.setPrevisao(previa.fim());
 }
 public record Linha(UUID itemId,String nome,LocalDateTime inicio,LocalDateTime fim,List<AgendaPedido.Impressao> impressoes) {
  public Linha(UUID id,String nome,LocalDateTime inicio,LocalDateTime fim){this(id,nome,inicio,fim,List.of());}
 }
}
