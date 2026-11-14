package br.com.criare3d.controller;
import br.com.criare3d.models.Comprador;
import br.com.criare3d.repository.CompradorRepository;
import br.com.criare3d.repository.PedidoOrcamentoRepository;
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
import java.time.*;
import java.util.*;
@Controller @RequestMapping("/compradores") @Transactional
public class CompradorController  {
    private static final int DIAS_SEM_ORCAMENTO=60;
    @Autowired private CompradorRepository compradores;
    @Autowired private PedidoOrcamentoRepository pedidos;
    @Autowired private Clock clock;

    /** Um cliente da lista, já com o que a tela precisa (contato formatado, link de WhatsApp, resumo dos orçamentos). */
    public static class LinhaCliente {
        private final Comprador comprador; private final long total,abertos; private final LocalDateTime ultimo;
        LinhaCliente(Comprador comprador,long total,long abertos,LocalDateTime ultimo){this.comprador=comprador;this.total=total;this.abertos=abertos;this.ultimo=ultimo;}
        public Comprador getComprador(){return comprador;}
        public long getTotal(){return total;}
        public long getAbertos(){return abertos;}
        public LocalDateTime getUltimo(){return ultimo;}
        public String getTelefoneFormatado(){return formatarTelefone(comprador.getTelefone());}
        public String getWhatsapp(){return linkWhatsapp(comprador.getTelefone());}
        public String getIniciais(){return iniciais(comprador.getNome());}
        public int getCor(){return Math.floorMod(String.valueOf(comprador.getNome()).hashCode(),4);}
    }

    static String digitos(String texto){return texto==null?"":texto.replaceAll("\\D","");}
    static String semPais(String digitos){return digitos.length()>11 && digitos.startsWith("55")?digitos.substring(2):digitos;}
    static String formatarTelefone(String telefone) {
        var d=semPais(digitos(telefone));
        if(d.length()==11)return "("+d.substring(0,2)+") "+d.substring(2,7)+"-"+d.substring(7);
        if(d.length()==10)return "("+d.substring(0,2)+") "+d.substring(2,6)+"-"+d.substring(6);
        return telefone==null?"":telefone;
    }
    static String linkWhatsapp(String telefone) {
        var d=digitos(telefone);
        if(d.length()==10 || d.length()==11)return "https://wa.me/55"+d;
        if((d.length()==12 || d.length()==13) && d.startsWith("55"))return "https://wa.me/"+d;
        return null;
    }
    static String iniciais(String nome) {
        var partes=nome==null?new String[0]:nome.strip().split("\\s+");
        if(partes.length==0 || partes[0].isEmpty())return "?";
        var primeira=partes[0].substring(0,1);
        return (partes.length>1?primeira+partes[partes.length-1].substring(0,1):primeira).toUpperCase(Locale.ROOT);
    }
    private static String vazioParaNulo(String texto){return texto==null||texto.isBlank()?null:texto.strip();}

    private ModelAndView tela(Comprador comprador,String busca,int pagina,boolean arquivados,boolean abrirDialogo)  {
        busca=busca==null?"":busca.strip();
        var digitos=semPais(digitos(busca));
        if(digitos.length()<3)digitos="";
        var resultado=compradores.pesquisar(busca,digitos,arquivados,PageRequest.of(Math.max(0,pagina),20,Sort.by("nome")));
        var ids=resultado.getContent().stream().map(Comprador::getId).toList();
        Map<UUID,PedidoOrcamentoRepository.ResumoCliente> resumos=new HashMap<>();
        Map<UUID,Long> abertos=new HashMap<>();
        if(!ids.isEmpty()) {
            pedidos.resumoPorComprador(ids).forEach(r->resumos.put(r.getCompradorId(),r));
            pedidos.abertosPorComprador(ids).forEach(r->abertos.put(r.getCompradorId(),r.getAbertos()));
        }
        var linhas=resultado.getContent().stream().map(c->{var r=resumos.get(c.getId());return new LinhaCliente(c,r==null?0:r.getTotal(),abertos.getOrDefault(c.getId(),0L),r==null?null:r.getUltimo());}).toList();
        return new ModelAndView("compradores/index").addObject("comprador",comprador).addObject("busca",busca).addObject("arquivados",arquivados).addObject("pagina",resultado).addObject("linhas",linhas)
            .addObject("abrirDialogo",abrirDialogo).addObject("totalAtivos",compradores.countByArquivado(false)).addObject("totalArquivados",compradores.countByArquivado(true))
            .addObject("comAndamento",pedidos.compradoresComPedidoAberto()).addObject("semOrcamento",compradores.semOrcamentoDesde(LocalDateTime.now(clock).minusDays(DIAS_SEM_ORCAMENTO))).addObject("diasSemOrcamento",DIAS_SEM_ORCAMENTO);
    }
    @GetMapping public ModelAndView listar(@RequestParam(defaultValue="") String busca,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="false") boolean arquivados,@RequestParam(required=false) UUID editar)  {
        return tela(editar==null?new Comprador():compradores.findById(editar).orElseThrow(),busca,page,arquivados,editar!=null);
    }
    private void verificarDuplicado(Comprador dados,BindingResult erros) {
        var email=vazioParaNulo(dados.getEmail());
        if(email!=null)compradores.porEmail(email).stream().filter(o->!Objects.equals(o.getId(),dados.getId())).findFirst().ifPresent(o->erros.rejectValue("email","duplicado",mensagemDuplicado("e-mail",o)));
        var digitos=semPais(digitos(dados.getTelefone()));
        if(digitos.length()>=8)compradores.porTelefone(digitos).stream().filter(o->!Objects.equals(o.getId(),dados.getId())).findFirst().ifPresent(o->erros.rejectValue("telefone","duplicado",mensagemDuplicado("telefone",o)));
    }
    private String mensagemDuplicado(String campo,Comprador existente) {
        return "Já existe um cliente com este "+campo+": "+existente.getNome()+(existente.isArquivado()?" (arquivado; restaure em Ver arquivados).":".");
    }
    @PostMapping public ModelAndView salvar(@Valid @ModelAttribute("comprador") Comprador dados,BindingResult erros,RedirectAttributes flash)  {
        if(!erros.hasErrors())verificarDuplicado(dados,erros);
        if(erros.hasErrors()) {
            var mv=tela(dados,"",0,false,true);
            mv.addAllObjects(erros.getModel());
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            return mv;
        }
        var c=dados.getId()==null?new Comprador():compradores.findById(dados.getId()).orElseThrow();
        if(c.getId()!=null && !Objects.equals(c.getVersao(),dados.getVersao()))throw new IllegalArgumentException("O cliente foi alterado em outra tela. Reabra antes de salvar.");
        c.setNome(dados.getNome().strip());
        c.setTelefone(vazioParaNulo(dados.getTelefone()));
        var email=vazioParaNulo(dados.getEmail());
        c.setEmail(email==null?null:email.toLowerCase(Locale.ROOT));
        c.setObservacoes(vazioParaNulo(dados.getObservacoes()));
        compradores.save(c);
        flash.addFlashAttribute("mensagemSucesso","Cliente salvo. Disponível nos próximos orçamentos.");
        return new ModelAndView("redirect:/compradores");
    }
    @PostMapping("/{id}/estado") public String estado(@PathVariable UUID id,@RequestParam boolean arquivado,RedirectAttributes flash)  {
        compradores.findById(id).orElseThrow().setArquivado(arquivado);
        flash.addFlashAttribute("mensagemSucesso",arquivado?"Cliente arquivado; orçamentos preservados.":"Cliente restaurado.");
        return arquivado?"redirect:/compradores":"redirect:/compradores?arquivados=true";
    }
    @InitBinder("comprador") public void campos(org.springframework.web.bind.WebDataBinder b) {
        b.registerCustomEditor(String.class,new org.springframework.beans.propertyeditors.StringTrimmerEditor(true));
        b.setAllowedFields("id","versao","nome","telefone","email","observacoes");
    }
}
