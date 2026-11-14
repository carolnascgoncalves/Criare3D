package br.com.criare3d.service;
import br.com.criare3d.dto.*;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

/** Preserva preços sem congelar quantidade ou tempo da nova produção. */
@Service @Transactional(readOnly=true)
public class CustosHistoricosService {
 @Autowired private CalculoHistoricoRepository historicos;
 public record Base(Configuracao configuracao,Impressora impressora,Map<UUID,Filamento> filamentos,BigDecimal manutencao,List<CustoLoja> extras,String embalagem) {}
 public Base carregar(CalculoRequest request,Impressora atual,Map<UUID,Filamento> materiais) {
  var foto=request.getCustosPreservados();
  if(foto==null) {
   var h=historicos.buscarPorIdComItens(request.getReferenciaCustosHistoricos()).orElseThrow(()->new IllegalArgumentException("Cálculo de origem indisponível."));
   if(h.getPeca()==null||!h.getPeca().getId().equals(request.getPecaId())||h.getImpressora()==null||!h.getImpressora().getId().equals(request.getImpressoraId()))throw new IllegalArgumentException("Origem diferente da peça e da impressora.");
   foto=CustosPreservados.de(h);
  }
  var c=ConfiguracaoPadrao.criar();c.setFormularioAvancado(true);c.setTarifaEnergiaKwh(foto.tarifa());c.setEmbalagemFixa(true);c.setCustoEmbalagemFixa(zero(foto.embalagem()));
  c.setMaoObraAtivada(Boolean.TRUE.equals(foto.maoObra()));c.setMaoObraTempoFixo(false);c.setValorMaoObraHora(zero(foto.valorHora()));
  c.setDepreciacaoAtivada(Boolean.TRUE.equals(foto.depreciacao()));c.setTaxaFalhaAtivada(Boolean.TRUE.equals(foto.falhas()));c.setPercentualFalha(zero(foto.percentualFalhas()));c.setPurgaGramasPorImpressao(zero(foto.purga()));
  var impressora=new Impressora();BeanUtils.copyProperties(atual,impressora);impressora.setPotenciaWatts(foto.potencia());impressora.setTempoAquecimentoMinutos(foto.aquecimento());impressora.setValorCompra(foto.compra());impressora.setVidaUtilHoras(foto.vidaUtil());impressora.setDepreciacaoPorHora(foto.depreciacaoHora());
  var copias=new HashMap<UUID,Filamento>();
  for(var material:foto.materiais()) {var filamento=materiais.get(material.id());if(filamento==null)continue;var copia=new Filamento();BeanUtils.copyProperties(filamento,copia);copia.setPrecoRolo(material.preco());copia.setPesoRoloGramas(material.peso());copias.put(copia.getId(),copia);}
  if(request.getFilamentos().stream().anyMatch(i->!copias.containsKey(i.getFilamentoId())))throw new IllegalArgumentException("Materiais diferentes da origem: faça um novo cálculo com preços atuais.");
  var extras=foto.extras().stream().map(extra->{var custo=new CustoLoja();custo.setId(extra.id());custo.setDescricao(extra.descricao());custo.setTipo(CustoLoja.Tipo.VALOR_UNIDADE);custo.setValor(extra.valor());return custo;}).toList();
  return new Base(c,impressora,copias,zero(foto.manutencao()),extras,foto.nomeEmbalagem());
 }
 private static BigDecimal zero(BigDecimal valor){return valor==null?BigDecimal.ZERO:valor;}
}
