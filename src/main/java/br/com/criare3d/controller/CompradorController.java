package br.com.criare3d.controller;

import br.com.criare3d.dto.SalvarComprador;
import br.com.criare3d.models.Comprador;
import br.com.criare3d.service.CompradorDuplicadoException;
import br.com.criare3d.service.CompradorService;
import br.com.criare3d.util.ContatoCliente;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequestMapping("/compradores")
public class CompradorController {
    @Autowired
    private CompradorService clientes;

    /** Contato e identidade visual pertencem à apresentação web. */
    public static class LinhaCliente {
        private final Comprador comprador;
        private final long total, abertos;
        private final LocalDateTime ultimo;

        LinhaCliente(Comprador comprador, long total, long abertos, LocalDateTime ultimo) {
            this.comprador = comprador;
            this.total = total;
            this.abertos = abertos;
            this.ultimo = ultimo;
        }

        public Comprador getComprador() {
            return comprador;
        }

        public long getTotal() {
            return total;
        }

        public long getAbertos() {
            return abertos;
        }

        public LocalDateTime getUltimo() {
            return ultimo;
        }

        public String getTelefoneFormatado() {
            return ContatoCliente.formatarTelefone(comprador.getTelefone());
        }

        public String getWhatsapp() {
            return ContatoCliente.linkWhatsapp(comprador.getTelefone());
        }

        public String getIniciais() {
            return ContatoCliente.iniciais(comprador.getNome());
        }

        public int getCor() {
            return Math.floorMod(String.valueOf(comprador.getNome()).hashCode(), 4);
        }
    }

    private ModelAndView tela(Comprador comprador, String busca, int pagina, boolean arquivados, boolean abrirDialogo) {
        busca = busca == null ? "" : busca.strip();
        var resultado = clientes.listar(busca, arquivados,
            PageRequest.of(Math.max(0, pagina), 20, Sort.by("nome")));
        var linhas = resultado.pagina().getContent().stream()
            .map(c -> new LinhaCliente(c.comprador(), c.total(), c.abertos(), c.ultimo())).toList();
        var contadores = resultado.contadores();
        return new ModelAndView("compradores/index")
            .addObject("comprador", comprador).addObject("busca", busca).addObject("arquivados", arquivados)
            .addObject("pagina", resultado.pagina()).addObject("linhas", linhas)
            .addObject("abrirDialogo", abrirDialogo).addObject("totalAtivos", contadores.getAtivos())
            .addObject("totalArquivados", contadores.getArquivados())
            .addObject("comAndamento", contadores.getComAndamento())
            .addObject("semOrcamento", contadores.getSemOrcamento())
            .addObject("diasSemOrcamento", CompradorService.DIAS_SEM_ORCAMENTO);
    }

    @GetMapping
    public ModelAndView listar(@RequestParam(defaultValue = "") String busca,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "false") boolean arquivados,
                              @RequestParam(required = false) UUID editar) {
        return tela(editar == null ? new Comprador() : clientes.buscar(editar), busca, page, arquivados, editar != null);
    }

    @PostMapping
    public ModelAndView salvar(@Valid @ModelAttribute("comprador") Comprador dados,
                              BindingResult erros, RedirectAttributes flash) {
        if (!erros.hasErrors()) {
            try {
                clientes.salvar(new SalvarComprador(dados.getId(), dados.getVersao(), dados.getNome(),
                    dados.getTelefone(), dados.getEmail(), dados.getObservacoes()));
            } catch (CompradorDuplicadoException e) {
                e.getCampos().forEach((campo, mensagem) -> erros.rejectValue(campo, "duplicado", mensagem));
            }
        }
        if (erros.hasErrors()) {
            var mv = tela(dados, "", 0, false, true);
            mv.addAllObjects(erros.getModel());
            mv.setStatus(HttpStatus.UNPROCESSABLE_ENTITY);
            return mv;
        }
        flash.addFlashAttribute("mensagemSucesso", "Cliente salvo. Disponível nos próximos orçamentos.");
        return new ModelAndView("redirect:/compradores");
    }

    @PostMapping("/{id}/estado")
    public String estado(@PathVariable UUID id, @RequestParam boolean arquivado, RedirectAttributes flash) {
        clientes.arquivar(id, arquivado);
        flash.addFlashAttribute("mensagemSucesso",
            arquivado ? "Cliente arquivado; orçamentos preservados." : "Cliente restaurado.");
        return arquivado ? "redirect:/compradores" : "redirect:/compradores?arquivados=true";
    }

    @InitBinder("comprador")
    public void campos(WebDataBinder b) {
        b.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        b.setAllowedFields("id", "versao", "nome", "telefone", "email", "observacoes");
    }
}
