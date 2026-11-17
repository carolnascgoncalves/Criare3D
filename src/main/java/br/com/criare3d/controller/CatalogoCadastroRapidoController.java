package br.com.criare3d.controller;

import br.com.criare3d.dto.CatalogoOpcaoMapper;
import br.com.criare3d.dto.EmbalagemRequest;
import br.com.criare3d.dto.FilamentoRequest;
import br.com.criare3d.dto.ImpressoraCadastroRapidoRequest;
import br.com.criare3d.models.Impressora;
import br.com.criare3d.service.CatalogoModelosImpressora;
import br.com.criare3d.service.interfaces.EmbalagemService;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Cadastros auxiliares do site, autenticados pela sessão e protegidos por CSRF. */
@RestController
@RequestMapping("/catalogo-opcoes/cadastro")
public class CatalogoCadastroRapidoController {
    @Autowired
    private FilamentoService filamentos;
    @Autowired
    private ImpressoraService impressoras;
    @Autowired
    private EmbalagemService embalagens;
    @Autowired
    private CatalogoModelosImpressora modelosImpressora;

    public record ErrosCadastro(String mensagem, Map<String, String> campos) {
    }

    @PostMapping(value = "/filamento", consumes = "application/json")
    public ResponseEntity<?> filamento(@Valid @RequestBody FilamentoRequest dados, BindingResult erros) {
        if (erros.hasErrors()) return invalidos(erros);
        if (dados.getEstoqueGramas() == null) dados.setEstoqueGramas(BigDecimal.ZERO);
        var salvo = filamentos.create(FilamentoRequest.toEntity(dados));
        return ResponseEntity.status(201).body(CatalogoOpcaoMapper.de(salvo));
    }

    @PostMapping(value = "/impressora", consumes = "application/json")
    public ResponseEntity<?> impressora(@Valid @RequestBody ImpressoraCadastroRapidoRequest dados, BindingResult erros) {
        if (erros.hasErrors()) return invalidos(erros);
        var referencia = modelosImpressora.buscar(dados.modelo());
        var potencia = referencia.map(CatalogoModelosImpressora.Modelo::potenciaMediaPLA).orElse(dados.potenciaWatts());
        if (potencia == null) {
            return ResponseEntity.unprocessableEntity().body(new ErrosCadastro("Informe a potência para este modelo.",
                Map.of("potenciaWatts", "Não há referência para este modelo. Informe a potência média em watts.")));
        }
        var impressora = Impressora.builder().nome(dados.nome().strip())
            .modelo(referencia.map(CatalogoModelosImpressora.Modelo::nome).orElse(dados.modelo().strip()))
            .potenciaWatts(potencia).tempoAquecimentoMinutos(0).build();
        var salvo = impressoras.create(impressora);
        return ResponseEntity.status(201).body(CatalogoOpcaoMapper.de(salvo));
    }

    @PostMapping(value = "/embalagem", consumes = "application/json")
    public ResponseEntity<?> embalagem(@Valid @RequestBody EmbalagemRequest dados, BindingResult erros) {
        if (erros.hasErrors()) return invalidos(erros);
        var salvo = embalagens.create(EmbalagemRequest.toEntity(dados));
        return ResponseEntity.status(201).body(CatalogoOpcaoMapper.de(salvo));
    }

    private ResponseEntity<ErrosCadastro> invalidos(BindingResult erros) {
        var campos = new LinkedHashMap<String, String>();
        erros.getFieldErrors().forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        return ResponseEntity.unprocessableEntity().body(new ErrosCadastro("Confira os campos destacados.", campos));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrosCadastro> regra(IllegalArgumentException erro) {
        return ResponseEntity.badRequest().body(new ErrosCadastro(erro.getMessage(), Map.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrosCadastro> formato() {
        return ResponseEntity.badRequest().body(new ErrosCadastro("Confira os números e os dados informados.", Map.of()));
    }
}
