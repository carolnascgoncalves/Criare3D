package br.com.criare3d.service;
import br.com.criare3d.dto.FotoConteudo;
import br.com.criare3d.models.FotoItemPedido;
import br.com.criare3d.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Transactional
public class FotoPedidoService {
 private final FotoItemPedidoRepository fotos;
 private final ItemPedidoRepository itens;
 private final FotoCalculoService fotosCalculo;
 private final PecaArquivoService arquivos;
 public FotoPedidoService(FotoItemPedidoRepository fotos,ItemPedidoRepository itens,FotoCalculoService fotosCalculo,PecaArquivoService arquivos){this.fotos=fotos;this.itens=itens;this.fotosCalculo=fotosCalculo;this.arquivos=arquivos;}
 public void salvar(UUID itemId,FotoConteudo conteudo) {
  if(!itens.existsById(itemId))throw new IllegalArgumentException("Item não encontrado.");
  if(conteudo==null || conteudo.bytes()==null)return;
  if(conteudo.bytes().length>3145728 || !Set.of("image/jpeg","image/png","image/webp").contains(conteudo.tipo()))throw new IllegalArgumentException("Foto inválida ou maior que 3 MB.");
  var f=new FotoItemPedido();f.setItemId(itemId);f.setTipo(conteudo.tipo());f.setBytes(conteudo.bytes());fotos.save(f);
 }
 public void copiar(UUID itemId,UUID calculoId) {fotosCalculo.buscarConteudoFoto(calculoId,false).ifPresent(f->salvar(itemId,f));}
 public void copiarParaCalculo(UUID itemId,UUID calculoId) {buscar(itemId).ifPresent(f->fotosCalculo.salvar(calculoId,f.tipo(),f.bytes()));}
 @Transactional(readOnly=true) public Optional<FotoConteudo> buscar(UUID itemId) {
  var item=itens.findById(itemId).orElse(null);if(item==null)return Optional.empty();
  var foto=fotos.findById(itemId).map(f->new FotoConteudo(f.getTipo(),f.getBytes()));
  if(foto.isPresent()||item.getPecaId()==null)return foto;
  return arquivos.buscarImagem(item.getPecaId(),0).or(()->arquivos.buscarImagem(item.getPecaId(),1)).map(f->new FotoConteudo(f.mimeType(),f.bytes()));
 }
}
