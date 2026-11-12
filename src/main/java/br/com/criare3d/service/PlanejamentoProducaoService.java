package br.com.criare3d.service;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;
@Service @Transactional
public class PlanejamentoProducaoService {
 @Autowired private CalculoHistoricoRepository historicos;
 @Autowired private ImpressoraRepository impressoras;
 @Autowired private PlanejamentoProducaoRepository planos;
 @Autowired private ItemPedidoRepository itensPedido;
 public PlanejamentoProducao salvar(UUID id,int unidades,int capacidade,LocalDateTime inicio,int intervalo,int folga) {
  if(itensPedido.existsByLoteProducao_Id(id))throw new IllegalArgumentException("Este lote já está vinculado à fila de um pedido. Acompanhe em Orçamentos e pedidos.");
  if (inicio == null || unidades < 1 || unidades > 500 || capacidade < 1 || capacidade > unidades || intervalo < 0 || intervalo > 1440 || folga < 0 || folga > 100)
   throw new IllegalArgumentException("Confira as quantidades, a data e os ajustes do planejamento.");
  var c = historicos.findById(id).orElseThrow(() -> new IllegalArgumentException("Cálculo não encontrado."));
  if (c.getUnidadesVendidas() != null && c.getUnidadesVendidas() >= c.getUnidades()) throw new IllegalArgumentException("Este lote já foi vendido. Reutilize o cálculo para planejar um novo lote.");
  if (c.getImpressora() == null) throw new IllegalArgumentException("O cálculo não tem uma impressora disponível.");
  var impressora = impressoras.bloquearPorId(c.getImpressora().getId()).orElseThrow();
  if (impressora.isArquivado()) throw new IllegalArgumentException("Restaure a impressora antes de planejar.");
  var ultimo = planos.findFirstByImpressora_IdAndCanceladoFalseAndConcluidoFalseOrderByFimDesc(impressora.getId());
  var agora = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
  if (inicio.isBefore(agora)) inicio = agora;
  if (ultimo.isPresent() && ultimo.get().getFim().isAfter(inicio)) inicio = ultimo.get().getFim();
  long minutos = duracao(c.getTempoImpressaoMinutos(),c.getTempoAquecimentoMinutos() == null ? 0 : c.getTempoAquecimentoMinutos(),unidades,capacidade,intervalo,folga);
  var p = new PlanejamentoProducao();p.setCalculo(c);p.setImpressora(impressora);p.setNomePeca(c.getNomePeca());p.setNomeImpressora(c.getNomeImpressora());
  p.setUnidades(unidades);p.setPecasPorImpressao(capacidade);p.setIntervalo(intervalo);p.setFolga(folga);p.setInicio(inicio);
  try {p.setFim(inicio.plusMinutes(minutos));}
  catch (DateTimeException e) {throw new IllegalArgumentException("O prazo calculado ultrapassa uma data válida.");}
  return planos.save(p);
 }
 public static long duracao(long tempo,int aquecimento,int unidades,int capacidade,int intervalo,int folga) {
  long total = Math.addExact(ModeloImpressao.minutosTotais(tempo,aquecimento,unidades,capacidade),
    Math.multiplyExact((long)intervalo,ModeloImpressao.quantidadeImpressoes(unidades,capacidade)-1));
  return Math.max(1, Math.addExact(total,Math.round(total * (folga / 100d))));
 }
 public void cancelar(UUID id) {
  if(itensPedido.existsByPlanejamento_Id(id))throw new IllegalArgumentException("Este planejamento pertence a um pedido. Gerencie o cancelamento em Orçamentos e pedidos.");
  var plano = planos.findById(id).orElseThrow(() -> new IllegalArgumentException("Planejamento não encontrado."));
  impressoras.bloquearPorId(plano.getImpressora().getId()).orElseThrow();
  plano.setCancelado(true);
 }
}
