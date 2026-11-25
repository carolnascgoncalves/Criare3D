package br.com.criare3d.api;

import br.com.criare3d.dto.*;
import br.com.criare3d.service.interfaces.*;
import br.com.criare3d.service.IdempotenciaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import br.com.criare3d.service.PecaArquivoService;
import br.com.criare3d.service.Modelo3mfParser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalogo")
@SecurityRequirement(name="bearerAuth")
@RequiredArgsConstructor
public class ApiCatalogoEdicaoController {
    private final PecaArquivoService arquivos;
    private final Modelo3mfParser parser;
    private final PecaService pecas;

    private final ImpressoraService impressoras;
    private final FilamentoService filamentos;
    private final EmbalagemService embalagens;
    private final IdempotenciaService operacoes;

    private final tools.jackson.databind.ObjectMapper json;

    private final br.com.criare3d.service.CatalogoModelosImpressora modelos;
    private final br.com.criare3d.service.MercadoLivreDepreciacaoService referencias;
    private final br.com.criare3d.service.EstimativaUsoImpressoraService estimativas;

    @GetMapping("/impressoras/modelos")
    public java.util.List<br.com.criare3d.service.CatalogoModelosImpressora.Modelo> modelos() {
        return modelos.listar();
    }
    @GetMapping("/impressoras/uso")
    public br.com.criare3d.service.EstimativaUsoImpressoraService.Estimativa uso(@RequestParam String modelo) {
        return estimativas.estimar(modelo,null);
    }
    @GetMapping("/impressoras/referencia")
    public EstimativaDepreciacaoResponse referencia(@RequestParam String modelo, @RequestParam(required=false) UUID id) {
        return referencias.estimar(modelo,id);
    }

    public record SelecaoCatalogo(@jakarta.validation.constraints.NotEmpty java.util.List<@NotNull UUID> ids) {}

    @PostMapping("/{categoria}/remover-lote")
    @Transactional
    public java.util.List<UUID> removerSelecionados(@PathVariable String categoria, @Valid @RequestBody SelecaoCatalogo selecao) {
        var ids = selecao.ids().stream().distinct().sorted().toList();
        return switch (categoria) {
            case "pecas" -> pecas.deleteUnused(ids);
            case "filamentos" -> filamentos.deleteUnused(ids);
            case "impressoras" -> impressoras.deleteUnused(ids);
            case "embalagens" -> embalagens.deleteUnused(ids);
            default -> throw new NoSuchElementException();
        };
    }

    private void cadastrar(UUID chave, String categoria, Object dados, Runnable salvar) {
        operacoes.executar(chave, "CADASTRAR_" + categoria, json.writeValueAsString(dados), () -> {
            salvar.run();
            return "OK";
        });
    }

    @PostMapping("/pecas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cadastrarPeca(@RequestParam UUID chave, @Valid @RequestBody PecaRequest dados) {
        cadastrar(chave, "PECA", dados, () -> {
            var arquivo = arquivos.preparar(dados.getDadosArquivo3mf());
            dados.setDadosArquivo3mf(arquivo.map(PecaArquivoService.Preparacao::json).orElse(null));
            var peca = pecas.create(PecaRequest.toEntity(dados));
            arquivo.ifPresent(a -> arquivos.salvar(peca.getId(), a));
        });
    }

    @PostMapping("/filamentos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cadastrarFilamento(@RequestParam UUID chave, @Valid @RequestBody FilamentoRequest dados) {
        cadastrar(chave, "FILAMENTO", dados, () -> filamentos.create(FilamentoRequest.toEntity(dados)));
    }

    @PostMapping("/impressoras")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cadastrarImpressora(@RequestParam UUID chave, @Valid @RequestBody ImpressoraRequest dados) {
        cadastrar(chave, "IMPRESSORA", dados, () -> impressoras.create(ImpressoraRequest.toEntity(dados)));
    }

    @PostMapping("/embalagens")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cadastrarEmbalagem(@RequestParam UUID chave, @Valid @RequestBody EmbalagemRequest dados) {
        cadastrar(chave, "EMBALAGEM", dados, () -> embalagens.create(EmbalagemRequest.toEntity(dados)));
    }

    @PostMapping(value="/pecas/ler-arquivo", consumes="multipart/form-data")
    public PecaModeloImportacaoRequest ler(@RequestParam("arquivo3mf") MultipartFile arquivo) throws IOException {
        return new PecaModeloImportacaoRequest(arquivo.getOriginalFilename(), parser.analisar(arquivo));
    }

    @GetMapping("/pecas/{id}/edicao")
    public PecaRequest peca(@PathVariable UUID id) {
        var dados = PecaRequest.toDto(pecas.fetchById(id).orElseThrow());
        dados.setDadosArquivo3mf(null);
        return dados;
    }

    @PutMapping("/pecas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void editarPeca(@PathVariable UUID id, @Valid @RequestBody PecaRequest dados) {
        var arquivo = arquivos.preparar(dados.getDadosArquivo3mf());
        dados.setDadosArquivo3mf(arquivo.map(PecaArquivoService.Preparacao::json).orElse(null));
        pecas.update(id, PecaRequest.toEntity(dados)).orElseThrow();
        arquivo.ifPresent(a -> arquivos.salvar(id, a));
    }

    @GetMapping("/filamentos/{id}/edicao")
    public FilamentoDadosRequest filamento(@PathVariable UUID id) {
        return FilamentoDadosRequest.toDto(filamentos.fetchById(id).orElseThrow());
    }

    @PutMapping("/filamentos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editarFilamento(@PathVariable UUID id, @Valid @RequestBody FilamentoDadosRequest dados) {
        filamentos.update(id, FilamentoDadosRequest.toEntity(dados)).orElseThrow();
    }

    @GetMapping("/impressoras/{id}/edicao")
    public ImpressoraRequest impressora(@PathVariable UUID id) {
        return ImpressoraRequest.toDto(impressoras.fetchById(id).orElseThrow());
    }

    @PutMapping("/impressoras/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editarImpressora(@PathVariable UUID id, @Valid @RequestBody ImpressoraRequest dados) {
        impressoras.update(id, ImpressoraRequest.toEntity(dados)).orElseThrow();
    }

    @GetMapping("/embalagens/{id}/edicao")
    public EmbalagemDadosRequest embalagem(@PathVariable UUID id) {
        return EmbalagemDadosRequest.toDto(embalagens.fetchById(id).orElseThrow());
    }

    @PutMapping("/embalagens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void editarEmbalagem(@PathVariable UUID id, @Valid @RequestBody EmbalagemDadosRequest dados) {
        embalagens.update(id, EmbalagemDadosRequest.toEntity(dados)).orElseThrow();
    }
    public record CompraCatalogo(@NotNull UUID chave, @NotNull @Valid CompraFilamentoRequest compra) {}

    @PostMapping("/filamentos/{id}/compras")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void comprar(@PathVariable UUID id, @Valid @RequestBody CompraCatalogo entrada) {
        var compra = entrada.compra();
        var assinatura = java.util.Arrays.asList(id, compra.getUnidadesCompra(), compra.getGramasCompradas(),
                compra.getPesoRoloGramas(), compra.getPrecoRolo()).toString();
        operacoes.executar(entrada.chave(), "COMPRA_FILAMENTO", assinatura, () -> {
            if (!filamentos.registrarCompra(id, compra)) throw new NoSuchElementException();
            return "OK";
        });
    }
}
