package br.com.criare3d.service;
import br.com.criare3d.models.Movimentacao;
import br.com.criare3d.repository.MovimentacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
@Service @Transactional
public class MovimentacaoService {
 @Autowired private MovimentacaoRepository movimentos;
 @Autowired private UsuarioLogadoService usuario;
 @Autowired private Clock clock;
 public void registrar(String tipo,String descricao,BigDecimal quantidade,String medida,String origem,UUID referencia) {
  registrar(tipo,descricao,quantidade,medida,origem,referencia,null,null);
 }
 public void registrar(String tipo,String descricao,BigDecimal quantidade,String medida,String origem,UUID referencia,UUID filamentoId,BigDecimal saldoApos) {
  if(quantidade==null || quantidade.signum()==0)return;
  var m=new Movimentacao();m.setTipo(tipo);m.setDescricao(descricao.substring(0,Math.min(180,descricao.length())));m.setQuantidade(quantidade);m.setMedida(medida);m.setOrigem(origem);m.setReferencia(referencia);m.setData(LocalDateTime.now(clock));m.setUsuarioId(usuario.buscarIdUsuarioAtual());m.setFilamentoId(filamentoId);m.setSaldoApos(saldoApos);movimentos.save(m);
 }
}
