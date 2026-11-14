package br.com.criare3d.service;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.UUID;
@Service @Transactional
public class FalhaProducaoService {
 @Autowired private CalculoHistoricoRepository historicos;
 @Autowired private FilamentoRepository filamentos;
 @Autowired private MovimentacaoService movimentos;
 @Autowired private FalhaProducaoRepository falhas;
 public void registrar(UUID id,UUID filamentoId,int unidades,BigDecimal gramas,long minutos,String observacao) {
  if (unidades < 1 || unidades > 10000 || gramas == null || gramas.signum() <= 0 || gramas.compareTo(new BigDecimal("1000000")) > 0 || minutos < 0 || minutos > 10000000 || (observacao != null && observacao.length() > 500))
   throw new IllegalArgumentException("Confira unidades, material perdido, tempo e observação da falha.");
  var calculo = historicos.bloquearPorId(id).orElseThrow(() -> new IllegalArgumentException("Cálculo não encontrado."));
  var filamento = filamentos.bloquearPorId(filamentoId).orElseThrow(() -> new IllegalArgumentException("Filamento não encontrado."));
  if (filamento.getEstoqueGramas() == null || filamento.getEstoqueGramas().compareTo(gramas) < 0) throw new IllegalArgumentException("Estoque insuficiente para registrar o material perdido.");
  if (filamento.getPesoRoloGramas() == null || filamento.getPesoRoloGramas().signum() <= 0 || filamento.getPrecoRolo() == null) throw new IllegalArgumentException("Confira o peso e preço do filamento.");
  var f = new FalhaProducao();f.setCalculo(calculo);f.setFilamento(filamento);f.setUnidades(unidades);f.setGramas(gramas);f.setMinutos(minutos);
  f.setCustoMaterial(gramas.multiply(filamento.getPrecoRolo()).divide(filamento.getPesoRoloGramas(),8,RoundingMode.HALF_UP));
  f.setObservacao(observacao);f.setDataRegistro(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
  filamento.setEstoqueGramas(filamento.getEstoqueGramas().subtract(gramas));falhas.save(f);
  if(movimentos!=null)movimentos.registrar("PERDA",filamento.getNome()+" · "+filamento.getCor(),gramas.negate(),"g","lote",id,filamento.getId(),filamento.getEstoqueGramas());
 }
 public void cancelar(UUID id,UUID falhaId) {
  historicos.bloquearPorId(id).orElseThrow();
  var f = falhas.findById(falhaId).orElseThrow(() -> new IllegalArgumentException("Falha não encontrada."));
  if (!id.equals(f.getCalculo().getId())) throw new IllegalArgumentException("Falha não encontrada.");
  if (f.isCancelada()) return;
  var filamento = filamentos.bloquearPorId(f.getFilamento().getId()).orElseThrow();
  filamento.setEstoqueGramas(filamento.getEstoqueGramas().add(f.getGramas()));f.setCancelada(true);
  if(movimentos!=null)movimentos.registrar("CANCELAR_PERDA",filamento.getNome()+" · "+filamento.getCor(),f.getGramas(),"g","lote",id,filamento.getId(),filamento.getEstoqueGramas());
 }
}
