package br.com.criare3d.controller;

import br.com.criare3d.dto.ClienteAdminRequest;
import br.com.criare3d.service.ClienteAdminService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/clientes")
public class ClienteAdminController {
    private final ClienteAdminService clienteAdminService;

    public ClienteAdminController(ClienteAdminService clienteAdminService) {
        this.clienteAdminService = clienteAdminService;
    }

    @GetMapping({"", "/listar"})
    public String listar(Model model) {
        model.addAttribute("clientes", clienteAdminService.listar());
        return "cliente/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("cliente", new ClienteAdminRequest());
        model.addAttribute("clienteId", null);
        model.addAttribute("acaoSalvar", "/clientes/salvar");
        model.addAttribute("temLogoExistente", false);
        model.addAttribute("modoEdicao", false);
        return "cliente/formulario";
    }

    @PostMapping("/salvar")
    public String criar(@Valid @ModelAttribute("cliente") ClienteAdminRequest request,
                        BindingResult resultado,
                        @RequestParam(value = "logo", required = false) MultipartFile logo,
                        Model model,
                        RedirectAttributes redirectAttributes,
                        jakarta.servlet.http.HttpServletResponse response) {
        if (resultado.hasErrors()) {
            prepararFormularioNovo(model);
            response.setStatus(422);
            return "cliente/formulario";
        }
        try {
            ClienteAdminService.ClienteCriado criado = clienteAdminService.criar(request, logo);
            redirectAttributes.addFlashAttribute("clienteCriado", true);
            redirectAttributes.addFlashAttribute("senhaTemporaria", criado.senhaTemporaria());
            return "redirect:/clientes/listar";
        } catch (IllegalArgumentException erro) {
            resultado.reject("cliente.invalido", erro.getMessage());
            prepararFormularioNovo(model);
            response.setStatus(422);
            return "cliente/formulario";
        } catch (DataIntegrityViolationException erro) {
            resultado.reject("cliente.duplicado", "O identificador ou e-mail já está sendo usado por outra conta.");
            prepararFormularioNovo(model);
            response.setStatus(422);
            return "cliente/formulario";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable UUID id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("cliente", clienteAdminService.carregarFormulario(id));
            model.addAttribute("clienteId", id);
            model.addAttribute("acaoSalvar", "/clientes/" + id + "/salvar");
            model.addAttribute("temLogoExistente", clienteAdminService.temLogo(id));
            model.addAttribute("modoEdicao", true);
            return "cliente/formulario";
        } catch (IllegalArgumentException | IllegalStateException erro) {
            redirectAttributes.addFlashAttribute("erroCliente", erro.getMessage());
            return "redirect:/clientes/listar";
        }
    }

    @PostMapping("/{id}/salvar")
    public String atualizar(@PathVariable UUID id,
                            @Valid @ModelAttribute("cliente") ClienteAdminRequest request,
                            BindingResult resultado,
                            @RequestParam(value = "logo", required = false) MultipartFile logo,
                            @RequestParam(value = "removerLogo", defaultValue = "false") boolean removerLogo,
                            Model model,
                            RedirectAttributes redirectAttributes,
                            jakarta.servlet.http.HttpServletResponse response) {
        if (resultado.hasErrors()) {
            prepararFormularioEdicao(id, model, resultado, response);
            return "cliente/formulario";
        }
        try {
            clienteAdminService.atualizar(id, request, logo, removerLogo);
            redirectAttributes.addFlashAttribute("clienteSalvo", true);
            redirectAttributes.addFlashAttribute("avisoTema", "O cliente foi atualizado. As alterações visuais aparecem após um novo login da conta cliente.");
            return "redirect:/clientes/listar";
        } catch (IllegalArgumentException | IllegalStateException erro) {
            resultado.reject("cliente.invalido", erro.getMessage());
            prepararFormularioEdicao(id, model, resultado, response);
            response.setStatus(422);
            return "cliente/formulario";
        } catch (DataIntegrityViolationException erro) {
            resultado.reject("cliente.duplicado", "O identificador ou e-mail já está sendo usado por outra conta.");
            prepararFormularioEdicao(id, model, resultado, response);
            response.setStatus(422);
            return "cliente/formulario";
        }
    }

    @PostMapping("/{id}/alternar-status")
    public String alternarStatus(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            boolean ativo = clienteAdminService.alternarAtivo(id);
            redirectAttributes.addFlashAttribute("statusCliente", ativo ? "Cliente ativado." : "Cliente desativado.");
        } catch (IllegalArgumentException erro) {
            redirectAttributes.addFlashAttribute("erroCliente", erro.getMessage());
        }
        return "redirect:/clientes/listar";
    }

    @PostMapping("/{id}/gerar-senha")
    public String gerarSenha(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            String senha = clienteAdminService.gerarNovaSenhaTemporaria(id);
            redirectAttributes.addFlashAttribute("senhaTemporaria", senha);
            redirectAttributes.addFlashAttribute("senhaDoCliente", id);
        } catch (IllegalArgumentException | IllegalStateException erro) {
            redirectAttributes.addFlashAttribute("erroCliente", erro.getMessage());
        }
        return "redirect:/clientes/" + id + "/editar";
    }

    @GetMapping(value = "/{id}/logo", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> logo(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(clienteAdminService.buscarLogo(id));
        } catch (IllegalArgumentException erro) {
            return ResponseEntity.notFound().build();
        }
    }

    private void prepararFormularioNovo(Model model) {
        model.addAttribute("clienteId", null);
        model.addAttribute("acaoSalvar", "/clientes/salvar");
        model.addAttribute("temLogoExistente", false);
        model.addAttribute("modoEdicao", false);
    }

    private void prepararFormularioEdicao(UUID id, Model model, BindingResult resultado,
                                          jakarta.servlet.http.HttpServletResponse response) {
        model.addAttribute("clienteId", id);
        model.addAttribute("acaoSalvar", "/clientes/" + id + "/salvar");
        model.addAttribute("modoEdicao", true);
        try {
            model.addAttribute("temLogoExistente", clienteAdminService.temLogo(id));
        } catch (IllegalArgumentException erro) {
            resultado.reject("cliente.invalido", erro.getMessage());
            model.addAttribute("temLogoExistente", false);
        }
        response.setStatus(422);
    }
}
