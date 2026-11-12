package br.com.criare3d.controller;
import br.com.criare3d.service.*;
import br.com.criare3d.dto.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import java.security.*;
import java.time.*;
import java.util.*;

@Controller
public class MelhorEnvioController {
 private final MelhorEnvioService envio;private final OrcamentoPedidoService pedidos;private final UsuarioLogadoService usuario;private final Clock clock;
 public MelhorEnvioController(MelhorEnvioService envio,OrcamentoPedidoService pedidos,UsuarioLogadoService usuario,Clock clock){this.envio=envio;this.pedidos=pedidos;this.usuario=usuario;this.clock=clock;}
 @GetMapping("/integracoes/melhor-envio") public ModelAndView configurar(){return new ModelAndView("orcamentos/melhor-envio").addObject("paginaAtiva","orcamentos").addObject("configurado",envio.configurado()).addObject("conectado",envio.conectado()).addObject("ambiente",envio.ambiente()).addObject("callback",envio.callback());}
 @PostMapping("/integracoes/melhor-envio/conectar") public String conectar(HttpSession sessao,RedirectAttributes flash){try{byte[] b=new byte[32];new SecureRandom().nextBytes(b);String state=Base64.getUrlEncoder().withoutPadding().encodeToString(b);String url=envio.autorizar(state);sessao.setAttribute("oauthMelhorEnvio",new Pendente(state,usuario.buscarTenantIdAtual(),usuario.buscarIdUsuarioAtual(),envio.ambiente(),clock.instant().plusSeconds(600)));return "redirect:"+url;}catch(IllegalArgumentException e){flash.addFlashAttribute("mensagemErro",e.getMessage());return "redirect:/integracoes/melhor-envio";}}
 @GetMapping("/integracoes/melhor-envio/callback") public String callback(@RequestParam(required=false)String state,@RequestParam(required=false)String code,@RequestParam(required=false)String error,HttpSession sessao,HttpServletResponse response,RedirectAttributes flash){
  response.setHeader("Referrer-Policy","no-referrer");response.setHeader("Cache-Control","no-store");
  Object valor=sessao.getAttribute("oauthMelhorEnvio");sessao.removeAttribute("oauthMelhorEnvio");
  try{if(!(valor instanceof Pendente p)||state==null||!MessageDigest.isEqual(p.state().getBytes(java.nio.charset.StandardCharsets.UTF_8),state.getBytes(java.nio.charset.StandardCharsets.UTF_8))||!p.tenant().equals(usuario.buscarTenantIdAtual())||!p.usuario().equals(usuario.buscarIdUsuarioAtual())||!p.ambiente().equals(envio.ambiente())||p.expira().isBefore(clock.instant()))throw new IllegalArgumentException("A autorização expirou ou foi iniciada em outro navegador/servidor. Clique em Conectar novamente.");
   if(error!=null||code==null||code.length()>4096)throw new IllegalArgumentException("A autorização não foi concluída. Tente conectar novamente.");envio.conectar(code);flash.addFlashAttribute("mensagemSucesso","Melhor Envio conectado para esta empresa.");
  }catch(IllegalArgumentException e){flash.addFlashAttribute("mensagemErro",e.getMessage());}return "redirect:/integracoes/melhor-envio";
 }
 @PostMapping("/integracoes/melhor-envio/desconectar")public String desconectar(RedirectAttributes flash){envio.desconectar();flash.addFlashAttribute("mensagemSucesso","Conexão local removida. Você também pode revogar a autorização no Melhor Envio.");return "redirect:/integracoes/melhor-envio";}
 @GetMapping("/orcamentos/{id}/frete")public ModelAndView frete(@PathVariable UUID id){return tela(id,new FreteRequest());}
 private ModelAndView tela(UUID id,FreteRequest dados){var p=pedidos.buscar(id);if(!p.getStatus().isEditavel()||p.isArquivado())throw new IllegalArgumentException("Volte o orçamento para edição antes de alterar o frete.");return new ModelAndView("orcamentos/frete").addObject("pedido",p).addObject("freteDados",dados).addObject("conectado",envio.conectado()).addObject("ambiente",envio.ambiente());}
 @PostMapping("/orcamentos/{id}/frete")public ModelAndView cotar(@PathVariable UUID id,@Valid @ModelAttribute("freteDados")FreteRequest dados,BindingResult erros,HttpSession sessao){
  var mv=tela(id,dados);if(!erros.hasErrors())try{var p=pedidos.buscar(id);var opcoes=envio.cotar(dados,p.getSubtotal().subtract(p.getDesconto()));sessao.setAttribute("frete:"+id,new Cotacao(usuario.buscarTenantIdAtual(),p.getRevisao(),envio.ambiente(),clock.instant().plusSeconds(600),opcoes));return mv.addObject("opcoes",opcoes);}catch(IllegalArgumentException e){mv.addObject("mensagemErro",e.getMessage());}
  if(erros.hasErrors()){mv.addAllObjects(erros.getModel());mv.addObject("mensagemErro","Confira CEPs, peso e medidas do pacote.");}mv.setStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY);return mv;
 }
 @PostMapping("/orcamentos/{id}/frete/aplicar")public String aplicar(@PathVariable UUID id,@RequestParam int opcao,HttpSession sessao,RedirectAttributes flash){
  try{Object valor=sessao.getAttribute("frete:"+id);if(!(valor instanceof Cotacao c)||!c.tenant().equals(usuario.buscarTenantIdAtual())||!c.ambiente().equals(envio.ambiente())||c.expira().isBefore(clock.instant())||opcao<0||opcao>=c.opcoes().size())throw new IllegalArgumentException("A cotação expirou. Consulte o frete novamente.");pedidos.aplicarFrete(id,c.revisao(),c.opcoes().get(opcao).preco());sessao.removeAttribute("frete:"+id);flash.addFlashAttribute("mensagemSucesso","Frete aplicado ao valor cobrado e ao custo de envio. Você pode ajustar o que cobra do cliente.");return "redirect:/orcamentos/"+id+"?etapa=3";}catch(IllegalArgumentException e){flash.addFlashAttribute("mensagemErro",e.getMessage());return "redirect:/orcamentos/"+id+"/frete";}
 }
 private record Pendente(String state,UUID tenant,UUID usuario,String ambiente,Instant expira){}
 private record Cotacao(UUID tenant,long revisao,String ambiente,Instant expira,List<MelhorEnvioService.Opcao> opcoes){}
}
