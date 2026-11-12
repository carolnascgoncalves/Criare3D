package br.com.criare3d.controller;
import br.com.criare3d.repository.PlanejamentoProducaoRepository;
import br.com.criare3d.service.PlanejamentoProducaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.UUID;
@Controller
public class PlanejamentoProducaoController {
 @Autowired private PlanejamentoProducaoRepository planos;
 @Autowired private PlanejamentoProducaoService service;
 @GetMapping("/simulacao/planejamentos")
 public ModelAndView listar(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="false") boolean concluidos) {
  return new ModelAndView("simulacao/planejamentos").addObject("pagina",concluidos ? planos.findByCanceladoFalseAndConcluidoTrueOrderByInicioDesc(PageRequest.of(Math.max(0,page),20)) : planos.findByCanceladoFalseAndConcluidoFalseOrderByInicioAsc(PageRequest.of(Math.max(0,page),20))).addObject("concluidos",concluidos);
 }
 @PostMapping("/simulacao/planejamentos")
 public String salvar(@RequestParam UUID calculoId,@RequestParam int unidades,@RequestParam int capacidade,
  @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
  @RequestParam(defaultValue="10") int intervalo,@RequestParam(defaultValue="10") int folga,RedirectAttributes flash) {
  try {service.salvar(calculoId,unidades,capacidade,inicio,intervalo,folga);flash.addFlashAttribute("mensagemSucesso","Planejamento salvo. O início considera os trabalhos anteriores da mesma impressora.");}
  catch(IllegalArgumentException | ArithmeticException e){flash.addFlashAttribute("mensagemErro",e.getMessage());return "redirect:/simulacao";}
  return "redirect:/simulacao/planejamentos";
 }
 @PostMapping("/simulacao/planejamentos/{id}/cancelar")
 public String cancelar(@PathVariable UUID id,RedirectAttributes flash) {
  try {service.cancelar(id);flash.addFlashAttribute("mensagemSucesso","Planejamento cancelado. Os demais horários foram preservados.");}
  catch(IllegalArgumentException e){flash.addFlashAttribute("mensagemErro",e.getMessage());}
  return "redirect:/simulacao/planejamentos";
 }
}
