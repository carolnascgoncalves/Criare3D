package br.com.criare3d.controller;
import br.com.criare3d.models.Comprador;
import br.com.criare3d.repository.CompradorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.BindingResult;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.*;
import jakarta.validation.Valid;
import java.util.*;
@Controller @RequestMapping("/compradores") @Transactional
public class CompradorController  {
    @Autowired private CompradorRepository compradores;
    private ModelAndView tela(Comprador comprador,String busca,int pagina,boolean arquivados)  {
        return new ModelAndView("compradores/index").addObject("comprador",comprador).addObject("busca",busca).addObject("arquivados",arquivados)    .addObject("pagina",compradores.buscar(busca,arquivados,PageRequest.of(Math.max(0,pagina),20,Sort.by("nome"))));
    }
    @GetMapping public ModelAndView listar(@RequestParam(defaultValue="") String busca,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="false") boolean arquivados,@RequestParam(required=false) UUID editar)  {
        return tela(editar==null?new Comprador():compradores.findById(editar).orElseThrow(),busca,page,arquivados);
    }
    @PostMapping public ModelAndView salvar(@Valid @ModelAttribute("comprador") Comprador dados,BindingResult erros,RedirectAttributes flash)  {
        if(erros.hasErrors()) {
            var mv=tela(dados,"",0,false);
            mv.addAllObjects(erros.getModel());
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            return mv;
        }
        var c=dados.getId()==null?new Comprador():compradores.findById(dados.getId()).orElseThrow();
        if(c.getId()!=null && !Objects.equals(c.getVersao(),dados.getVersao()))throw new IllegalArgumentException("O cliente foi alterado em outra tela. Reabra antes de salvar.");
        c.setNome(dados.getNome().strip());
        c.setTelefone(dados.getTelefone());
        c.setEmail(dados.getEmail());
        c.setObservacoes(dados.getObservacoes());
        compradores.save(c);
        flash.addFlashAttribute("mensagemSucesso","Cliente salvo. Disponível nos próximos orçamentos.");
        return new ModelAndView("redirect:/compradores");
    }
    @PostMapping("/{id}/estado") public String estado(@PathVariable UUID id,@RequestParam boolean arquivado,RedirectAttributes flash)  {
        compradores.findById(id).orElseThrow().setArquivado(arquivado);
        flash.addFlashAttribute("mensagemSucesso",arquivado?"Cliente arquivado; orçamentos preservados.":"Cliente restaurado.");
        return "redirect:/compradores";
    }
    @InitBinder("comprador") public void campos(org.springframework.web.bind.WebDataBinder b) {
        b.setAllowedFields("id","versao","nome","telefone","email","observacoes");
    }
}
