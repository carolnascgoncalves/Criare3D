package br.com.criare3d.controller;
import br.com.criare3d.dto.*;
import br.com.criare3d.models.*;
import br.com.criare3d.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import org.springframework.http.*;
import java.math.BigDecimal;
import java.util.*;

@Controller @RequestMapping("/orcamentos")
public class PedidoOrcamentoController {
 private final OrcamentoPedidoService service;
 private final OperacaoPedidoService operacoes;
 private final PlanejamentoPedidoService planejamento;
 private final FotoPedidoService fotos;
 private final FotoRascunhoService rascunho;
 public PedidoOrcamentoController(OrcamentoPedidoService service,OperacaoPedidoService operacoes,PlanejamentoPedidoService planejamento,FotoPedidoService fotos,FotoRascunhoService rascunho){this.service=service;this.operacoes=operacoes;this.planejamento=planejamento;this.fotos=fotos;this.rascunho=rascunho;}
 @GetMapping public ModelAndView listar(@RequestParam(defaultValue="")String busca,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="false")boolean arquivados,@RequestParam(required=false)br.com.criare3d.models.enums.StatusPedido status){return new ModelAndView("orcamentos/listar").addObject("pagina",service.listar(busca,page,arquivados,status)).addObject("busca",busca).addObject("arquivados",arquivados).addObject("statusFiltro",status).addObject("statusOpcoes",br.com.criare3d.models.enums.StatusPedido.values());}
 @GetMapping("/novo") public ModelAndView novo(@RequestParam(required=false)UUID calculo){var p=new PedidoOrcamento();return tela(p,1,null).addObject("referenciaSelecionada",calculo);}
 @PostMapping({"/iniciar-varios","/{id}/itens-varios"}) public String varios(@PathVariable(required=false)UUID id,@Valid @ModelAttribute SelecaoPedidoRequest dados,BindingResult erros,RedirectAttributes flash){try{if(erros.hasErrors())throw new IllegalArgumentException("Confira as peças selecionadas.");var p=service.adicionarVarios(id,dados);return "redirect:/orcamentos/"+p.getId()+"?etapa=1";}catch(IllegalArgumentException e){flash.addFlashAttribute("mensagemErro",e.getMessage());return id==null?"redirect:/orcamentos/novo":"redirect:/orcamentos/"+id+"?etapa=1";}}
 @PostMapping("/iniciar") public String iniciar(@RequestParam UUID referencia,@RequestParam(defaultValue="1")int quantidade,@RequestParam(defaultValue="0")int estoque,RedirectAttributes flash){
  try{return "redirect:/orcamentos/"+service.iniciar(referencia,quantidade,estoque).getId()+"?etapa=1";}
  catch(IllegalArgumentException e){flash.addFlashAttribute("mensagemErro",e.getMessage());return "redirect:/orcamentos/novo";}
 }
 @PostMapping("/do-calculo") public String doCalculo(HttpSession session,@RequestParam(required=false)UUID pedido,RedirectAttributes flash){
  var entrada=(CalculoRequest)session.getAttribute("requestCalculo");
  if(entrada==null){flash.addFlashAttribute("mensagemErro","O cálculo expirou. Calcule a peça novamente.");return "redirect:/historico/novo";}
  try {
   UUID destino;UUID itemId;
   if(pedido==null){var p=service.doCalculo(entrada);destino=p.getId();itemId=p.getItens().getFirst().getId();}
   else{var i=service.adicionarCalculo(pedido,entrada);destino=pedido;itemId=i.getId();}
   rascunho.buscar(session.getAttribute("fotoCalculoId")).ifPresent(f->fotos.salvar(itemId,f));
   session.removeAttribute("orcamentoDestino");
   return "redirect:/orcamentos/"+destino+"?etapa=1";
  } catch(IllegalArgumentException e){flash.addFlashAttribute("mensagemErro",e.getMessage());return "redirect:/historico/resultado";}
 }
 @GetMapping("/{id}") public ModelAndView abrir(@PathVariable UUID id,@RequestParam(defaultValue="1")int etapa,@RequestParam(defaultValue="false")boolean simular){
  var p=service.buscar(id);return tela(p,etapa,null).addObject("iniciarSimulacao",simular);
 }
 private ModelAndView tela(PedidoOrcamento p,int etapa,PedidoDadosRequest dados){
  if(p.getItens().isEmpty())etapa=1;
  var mv=new ModelAndView(p.getStatus().isEditavel() && !p.isArquivado()?"orcamentos/editar":"orcamentos/detalhes");
  mv.addObject("pedido",p);mv.addObject("etapa",Math.min(4,Math.max(1,etapa)));mv.addObject("referencias",p.getStatus().isEditavel()?service.referencias():List.of());
  if(dados==null){dados=new PedidoDadosRequest();dados.setTitulo(p.getTitulo());dados.setCliente(p.getCliente());dados.setContato(p.getContato());dados.setObservacoes(p.getObservacoes());dados.setInicioPretendido(p.getInicioPretendido());dados.setTurnoInicio(p.getTurnoInicio());dados.setTurnoFim(p.getTurnoFim());dados.setDias(p.getDias());dados.setIntervalo(p.getIntervalo());dados.setFolga(p.getFolga());dados.setDesconto(p.getDesconto());dados.setDescontoInformado(p.getDescontoPercentual()==null?p.getDesconto().setScale(2).toPlainString():p.getDescontoPercentual().stripTrailingZeros().toPlainString()+"%");dados.setFrete(p.getFrete());dados.setCustoFrete(p.getCustoFrete());}
  mv.addObject("dados",dados);
  var saldos=new HashMap<UUID,Long>();p.getItens().forEach(i->saldos.put(i.getId(),i.getLoteEstoque()==null?0:service.disponivel(i.getLoteEstoque())));mv.addObject("saldosEstoque",saldos);
  try{var linhas=planejamento.prever(p);mv.addObject("previsoes",linhas);mv.addObject("previsaoAtual",linhas.stream().map(PlanejamentoPedidoService.Linha::fim).max(java.time.LocalDateTime::compareTo).orElse(null));}
  catch(IllegalArgumentException e){mv.addObject("erroPrevisao",e.getMessage());mv.addObject("previsoes",List.of());}
  try{mv.addObject("materiaisPedido",service.materiais(p));}catch(IllegalArgumentException e){mv.addObject("erroMateriais",e.getMessage());}
  return mv;
 }
 @PostMapping("/{id}/itens") public String adicionar(@PathVariable UUID id,@RequestParam UUID referencia,@RequestParam int quantidade,@RequestParam(defaultValue="0")int estoque,RedirectAttributes flash){return executar(id,flash,()->service.adicionar(id,referencia,quantidade,estoque),"Peça adicionada.",1);}
 @PostMapping("/{id}/itens/{itemId}") public String alterar(@PathVariable UUID id,@PathVariable UUID itemId,@RequestParam int quantidade,@RequestParam(defaultValue="0")int estoque,@RequestParam BigDecimal preco,@RequestParam(defaultValue="1")int etapa,RedirectAttributes flash){return executar(id,flash,()->service.alterarItem(id,itemId,quantidade,estoque,preco),"Item atualizado.",etapa);}
 @PostMapping("/{id}/itens/{itemId}/remover") public String remover(@PathVariable UUID id,@PathVariable UUID itemId,RedirectAttributes flash){return executar(id,flash,()->service.remover(id,itemId),"Item removido.",1);}
 @PostMapping("/{id}/dados") public ModelAndView salvar(@PathVariable UUID id,@Valid @ModelAttribute("dados")PedidoDadosRequest dados,BindingResult erros,@RequestParam(defaultValue="2")int etapa,@RequestParam(defaultValue="3")int proxima,@RequestParam(defaultValue="false")boolean enviar,@RequestParam(defaultValue="false")boolean simular,@RequestParam(defaultValue="false")boolean cotarFrete,RedirectAttributes flash){
  if(enviar && (dados.getCliente()==null || dados.getCliente().isBlank()))erros.rejectValue("cliente","obrigatorio","Informe o nome do cliente antes de enviar.");
  if(!erros.hasErrors())try{service.salvarDados(id,dados);if(cotarFrete)return new ModelAndView("redirect:/orcamentos/"+id+"/frete");if(enviar)service.enviar(id);return new ModelAndView("redirect:/orcamentos/"+id+"?etapa="+Math.min(4,Math.max(1,simular?2:proxima))+(simular?"&simular=true":""));}catch(IllegalArgumentException e){erros.reject("pedido.invalido",e.getMessage());}
  var mv=tela(service.buscar(id),etapa,dados);mv.addAllObjects(erros.getModel());mv.addObject("mensagemErro",erros.getGlobalError()==null?"Confira os campos destacados antes de continuar.":erros.getGlobalError().getDefaultMessage());mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);return mv;
 }
 @PostMapping("/{id}/arquivar") public String arquivar(@PathVariable UUID id,@RequestParam(defaultValue="true")boolean arquivar,RedirectAttributes flash){try{service.arquivar(id,arquivar);flash.addFlashAttribute("mensagemSucesso",arquivar?"Orçamento arquivado. Nenhum saldo foi alterado.":"Orçamento restaurado.");}catch(IllegalArgumentException e){flash.addFlashAttribute("mensagemErro",e.getMessage());}return "redirect:/orcamentos?arquivados="+!arquivar;}
 @PostMapping("/{id}/rascunho") public ModelAndView rascunho(@PathVariable UUID id,@Valid @ModelAttribute("clienteDados")PedidoClienteRequest dados,BindingResult erros,@RequestParam(defaultValue="false")boolean enviar,RedirectAttributes flash) {
  if(enviar && (dados.getCliente()==null || dados.getCliente().isBlank()))erros.rejectValue("cliente","obrigatorio","Informe o nome do cliente antes de enviar.");
  if(!erros.hasErrors())try{service.salvarCliente(id,dados);if(enviar)service.enviar(id);flash.addFlashAttribute("mensagemSucesso",enviar?"Proposta marcada como enviada.":"Rascunho salvo.");return new ModelAndView("redirect:/orcamentos/"+id+"?etapa=4");}catch(IllegalArgumentException e){erros.reject("pedido.invalido",e.getMessage());}
  var dto=new PedidoDadosRequest();dto.setTitulo(dados.getTitulo());dto.setCliente(dados.getCliente());dto.setContato(dados.getContato());dto.setObservacoes(dados.getObservacoes());
  var mv=tela(service.buscar(id),4,null);var existente=(PedidoDadosRequest)mv.getModel().get("dados");existente.setTitulo(dto.getTitulo());existente.setCliente(dto.getCliente());existente.setContato(dto.getContato());existente.setObservacoes(dto.getObservacoes());
  var binding=new org.springframework.validation.BeanPropertyBindingResult(existente,"dados");for(var erro:erros.getFieldErrors())binding.rejectValue(erro.getField(),"invalido",erro.getDefaultMessage());mv.addAllObjects(binding.getModel());
  mv.addObject("mensagemErro",erros.getGlobalError()==null?"Confira os campos destacados.":erros.getGlobalError().getDefaultMessage());mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);return mv;
 }
 @PostMapping("/{id}/enviar") public String enviar(@PathVariable UUID id,RedirectAttributes flash){return executar(id,flash,()->service.enviar(id),"Proposta marcada como enviada. Nenhum estoque foi movimentado.",4);}
 @PostMapping("/{id}/revisar") public String revisar(@PathVariable UUID id,RedirectAttributes flash){return executar(id,flash,()->service.revisar(id),"Proposta em edição. Os custos foram conferidos novamente; revise antes de reenviar.",1);}
 @PostMapping("/{id}/aprovar") public String aprovar(@PathVariable UUID id,@RequestParam long revisao,RedirectAttributes flash){return executar(id,flash,()->operacoes.aprovar(id,revisao),"Pedido aprovado. Estoque reservado e produção planejada.",4);}
 @PostMapping("/{id}/produzir/{itemId}") public String produzir(@PathVariable UUID id,@PathVariable UUID itemId,@RequestParam long revisao,RedirectAttributes flash){return executar(id,flash,()->operacoes.produzir(id,itemId,revisao),"Produção confirmada e filamento descontado.",4);}
 @PostMapping("/{id}/pagar") public String pagar(@PathVariable UUID id,@RequestParam long revisao,RedirectAttributes flash){return executar(id,flash,()->operacoes.pagar(id,revisao),"Pagamento integral registrado.",4);}
 @PostMapping("/{id}/entregar") public String entregar(@PathVariable UUID id,@RequestParam long revisao,RedirectAttributes flash){return executar(id,flash,()->operacoes.entregar(id,revisao),"Entrega e vendas registradas.",4);}
 @PostMapping("/{id}/cancelar") public String cancelar(@PathVariable UUID id,@RequestParam long revisao,RedirectAttributes flash){return executar(id,flash,()->operacoes.cancelar(id,revisao),"Cancelado. Reservas liberadas; peças já produzidas permanecem no estoque. Confira eventual reembolso ao cliente.",4);}
 @GetMapping("/{id}/proposta") public ModelAndView proposta(@PathVariable UUID id){var p=service.buscar(id);if(p.getStatus().isEditavel())p.setPrevisao(planejamento.prever(p).stream().map(PlanejamentoPedidoService.Linha::fim).max(java.time.LocalDateTime::compareTo).orElse(null));return new ModelAndView("orcamentos/proposta").addObject("pedido",p);}
 @GetMapping("/itens/{itemId}/foto") @ResponseBody public ResponseEntity<byte[]> foto(@PathVariable UUID itemId){return fotos.buscar(itemId).map(f->ResponseEntity.ok().header("Cache-Control","private, no-store").contentType(MediaType.parseMediaType(f.tipo())).body(f.bytes())).orElseGet(()->ResponseEntity.notFound().build());}
 private String executar(UUID id,RedirectAttributes flash,Runnable acao,String mensagem,int etapa){try{acao.run();flash.addFlashAttribute("mensagemSucesso",mensagem);}catch(IllegalArgumentException e){flash.addFlashAttribute("mensagemErro",e.getMessage());}return "redirect:/orcamentos/"+id+"?etapa="+etapa;}
}
