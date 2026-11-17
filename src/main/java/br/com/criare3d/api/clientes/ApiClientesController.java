package br.com.criare3d.api.clientes;

import br.com.criare3d.api.*;
import br.com.criare3d.dto.SalvarComprador;
import br.com.criare3d.exception.RegraNegocioException;
import br.com.criare3d.models.Comprador;
import br.com.criare3d.service.CompradorService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clientes")
@SecurityRequirement(name = "bearerAuth")
public class ApiClientesController {
    private final CompradorService clientes;
    private final ApiDatas datas;

    public ApiClientesController(CompradorService clientes, ApiDatas datas) {
        this.clientes = clientes;
        this.datas = datas;
    }

    public record Entrada(
            @NotBlank @Size(min = 2, max = 150) String nome,
            @Size(max = 150) String telefone,
            @Email @Size(max = 150) String email,
            @Size(max = 500) String observacoes,
            @Min(0) Long versao) {}

    public record Cliente(
            UUID id,
            Long versao,
            String nome,
            String telefone,
            String email,
            String observacoes,
            boolean arquivado) {
        public static Cliente de(Comprador c) {
            return new Cliente(
                    c.getId(),
                    c.getVersao(),
                    c.getNome(),
                    c.getTelefone(),
                    c.getEmail(),
                    c.getObservacoes(),
                    c.isArquivado());
        }
    }

    public record ClienteResumo(
            Cliente cliente, long orcamentos, long emAndamento, OffsetDateTime ultimoOrcamento) {}

    public record Contadores(long ativos, long arquivados, long comAndamento, long semOrcamento) {}

    public record Listagem(PaginaResponse<ClienteResumo> clientes, Contadores resumo) {}

    public record Arquivo(@NotNull Boolean arquivado) {}

    @GetMapping
    public Listagem listar(
            @RequestParam(defaultValue = "") String busca,
            @RequestParam(defaultValue = "false") boolean arquivados,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        if (busca.length() > 150) throw new IllegalArgumentException("Busca muito longa.");
        var resultado =
                clientes.listar(
                        busca,
                        arquivados,
                        PaginaResponse.pedido(pagina, tamanho, Sort.by("nome", "id")));
        var linhas =
                resultado
                        .pagina()
                        .map(
                                l ->
                                        new ClienteResumo(
                                                Cliente.de(l.comprador()),
                                                l.total(),
                                                l.abertos(),
                                                datas.converter(l.ultimo())));
        var c = resultado.contadores();
        return new Listagem(
                PaginaResponse.de(linhas),
                new Contadores(
                        c.getAtivos(),
                        c.getArquivados(),
                        c.getComAndamento(),
                        c.getSemOrcamento()));
    }

    @GetMapping("/{id}")
    public Cliente buscar(@PathVariable UUID id) {
        return Cliente.de(clientes.buscar(id));
    }

    @PostMapping
    public ResponseEntity<Cliente> criar(@Valid @RequestBody Entrada entrada) {
        var salvo = clientes.salvar(comando(null, entrada));
        return ResponseEntity.created(URI.create("/api/v1/clientes/" + salvo.getId()))
                .body(Cliente.de(salvo));
    }

    @PutMapping("/{id}")
    public Cliente editar(@PathVariable UUID id, @Valid @RequestBody Entrada entrada) {
        if (entrada.versao() == null)
            throw new RegraNegocioException(
                    "versao_obrigatoria",
                    "Informe a versão recebida ao abrir o cliente.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        return Cliente.de(clientes.salvar(comando(id, entrada)));
    }

    @PatchMapping("/{id}/arquivo")
    public ResponseEntity<Void> arquivar(
            @PathVariable UUID id, @Valid @RequestBody Arquivo entrada) {
        clientes.arquivar(id, entrada.arquivado());
        return ResponseEntity.noContent().build();
    }

    private SalvarComprador comando(UUID id, Entrada e) {
        return new SalvarComprador(
                id, e.versao(), e.nome(), e.telefone(), e.email(), e.observacoes());
    }
}
