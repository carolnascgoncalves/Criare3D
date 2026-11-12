package br.com.criare3d.controller;
import br.com.criare3d.dto.*;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.enums.PlataformaVenda;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import jakarta.servlet.http.HttpSession;
import br.com.criare3d.service.CalculoRascunhoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.UUID;
@Controller
public class ReutilizarCalculoController {
 @Autowired private CalculoHistoricoRepository historicos;
 @GetMapping("/historico/reutilizar") @Transactional(readOnly=true)
 public String ultimo(@RequestParam UUID pecaId,HttpSession session,RedirectAttributes flash) {
  return carregar(historicos.findFirstByPeca_IdOrderByDataCalculoDesc(pecaId).orElse(null),session,flash);
 }
 @GetMapping("/historico/{id}/reutilizar") @Transactional(readOnly=true)
 public String especifico(@PathVariable UUID id,HttpSession session,RedirectAttributes flash) {
  return carregar(historicos.findById(id).orElse(null),session,flash);
 }
 private String carregar(CalculoHistorico c,HttpSession session,RedirectAttributes flash) {
  if(c == null){flash.addFlashAttribute("mensagemErro","Ainda não há cálculo para reutilizar desta peça.");return "redirect:/historico/novo";}
  if(c.getPeca() == null || c.getPeca().isArquivado() || c.getImpressora() == null || c.getImpressora().isArquivado()) {
   flash.addFlashAttribute("mensagemErro","Restaure a peça e a impressora deste cálculo antes de reutilizar.");return "redirect:/historico/novo";
  }
  CalculoRequest request = CalculoRascunhoMapper.copiar(c);
  request.setPecaId(c.getPeca().getId());request.setImpressoraId(c.getImpressora().getId());
  if(c.getEmbalagem() != null && !c.getEmbalagem().isArquivado()) request.setEmbalagemId(c.getEmbalagem().getId());
  try {request.setPlataformaVenda(PlataformaVenda.valueOf(c.getPlataformaVenda() == null ? "NENHUMA" : c.getPlataformaVenda()));} catch(IllegalArgumentException ignored){request.setPlataformaVenda(PlataformaVenda.NENHUMA);}
  for(var item : c.getItensFilamento()) {
   if(item.getFilamento() == null || item.getFilamento().isArquivado()) continue;
   var novo = new ItemCalculoRequest();novo.setFilamentoId(item.getFilamento().getId());novo.setQuantidadeFilamentoGramas(item.getQuantidadeFilamentoGramas());request.getFilamentos().add(novo);
  }
  if(request.getFilamentos().isEmpty() && c.getFilamento() != null && !c.getFilamento().isArquivado()) {
   var item = new ItemCalculoRequest();item.setFilamentoId(c.getFilamento().getId());item.setQuantidadeFilamentoGramas(c.getQuantidadeFilamentoGramas());request.getFilamentos().add(item);
  }
  session.removeAttribute("fotoCalculoBytes");session.removeAttribute("fotoCalculoTipo");session.removeAttribute("resultadoCalculo");session.removeAttribute("requestCalculo");
  session.setAttribute("calculoParaEditar",request);session.setAttribute("custoReferencia",c.getCustoTotal());session.setAttribute("pecaReferencia",c.getPeca().getId());
  flash.addFlashAttribute("mensagemSucesso","Dados reutilizados. Confira os filamentos e as opções antes de calcular com os custos atuais.");
  return "redirect:/historico/novo?editar=true";
 }
}
