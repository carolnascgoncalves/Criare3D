package br.com.criare3d.api;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.dto.HistoricoListaResponse;
import br.com.criare3d.service.CalculoService;
import br.com.criare3d.service.CatalogoBuscaService;
import br.com.criare3d.service.ConfiguracaoPadrao;
import br.com.criare3d.service.HistoricoCalculoService;
import br.com.criare3d.exception.RegraNegocioException;
import org.springframework.http.HttpStatus;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calculos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ApiCalculosController {
    private final CalculoService calculos;
    private final CatalogoBuscaService catalogo;
    private final HistoricoCalculoService historico;
    private final ConfiguracaoService configuracoes;

    public record EntradaCalculo(UUID pecaId, @NotNull UUID impressoraId,
            @NotEmpty @Size(max=100) List<@NotNull @Valid ItemCalculoRequest> filamentos,
            UUID embalagemId, @Min(1) @Max(10000) int unidades,
            @Min(1) @Max(1000000) long minutos, @Min(0) @Max(1000000) Long trabalho,
            @DecimalMin("0") @DecimalMax("99999") BigDecimal markup) {}

    public record ValorCalculo(UUID id, String nome, BigDecimal custoUnitario, BigDecimal precoUnitario, Integer unidades) {}

    public record ConfirmacaoCalculo(@NotNull @Valid EntradaCalculo entrada,
            @NotNull BigDecimal custoEsperado, @NotNull BigDecimal precoEsperado) {}

    public record PadroesCalculo(boolean embalagemFixa, boolean markupFixo, BigDecimal markup) {}

    @GetMapping("/padroes")
    public PadroesCalculo padroes() {
        var config = configuracoes.buscarConfiguracao().orElseGet(ConfiguracaoPadrao::criar);
        return new PadroesCalculo(Boolean.TRUE.equals(config.getEmbalagemFixa()),
                Boolean.TRUE.equals(config.getMarkupFixo()), config.getMarkupPercentual());
    }

    public record CalculoSalvo(ValorCalculo valores, String impressora, String filamento, String embalagem,
            Long minutos, String situacao, java.time.LocalDate data) {}

    @GetMapping("/{id}")
    public CalculoSalvo detalhe(@PathVariable UUID id) {
        var h = historico.fetchById(id).orElseThrow();
        return new CalculoSalvo(new ValorCalculo(h.getId(), h.getNomePeca(), h.getCustoTotal(), h.getPrecoSugerido(), h.getUnidades()),
                h.getNomeImpressora(), h.getNomeFilamento(), h.getNomeEmbalagem(), h.getTempoImpressaoMinutos(),
                Boolean.TRUE.equals(h.getVendido()) ? "Vendido" : Boolean.TRUE.equals(h.getProduzido()) ? "Produzido" : "Só cálculo",
                h.getDataCalculo() == null ? null : h.getDataCalculo().toLocalDate());
    }

    private CalculoRequest preparar(EntradaCalculo entrada) {
        var config = configuracoes.buscarConfiguracao().orElseGet(ConfiguracaoPadrao::criar);
        if (!Boolean.TRUE.equals(config.getEmbalagemFixa()) && entrada.embalagemId() == null)
            throw new RegraNegocioException("embalagem_obrigatoria", "Selecione uma embalagem.", HttpStatus.UNPROCESSABLE_ENTITY);
        var r = new CalculoRequest();
        r.setPecaId(entrada.pecaId());
        r.setImpressoraId(entrada.impressoraId());
        r.setFilamentos(entrada.filamentos());
        r.setUnidades(entrada.unidades());
        r.setTempoImpressaoMinutos(entrada.minutos());
        r.setTempoTrabalhoMinutos(entrada.trabalho() == null ? 0L : entrada.trabalho());
        r.setEmbalagemId(entrada.embalagemId());
        r.setMarkupPercentual(entrada.markup() == null || Boolean.TRUE.equals(config.getMarkupFixo())
                ? config.getMarkupPercentual() : entrada.markup());
        return r;
    }

    @PostMapping("/previa")
    public ValorCalculo consultar(@Valid @RequestBody EntradaCalculo entrada) {
        var r = preparar(entrada);
        var resultado = entrada.pecaId() == null
                ? calculos.preverAvulsoTotais(r, "Consulta rápida", entrada.unidades()) : calculos.prever(r);
        return new ValorCalculo(null, resultado.nomePeca(), resultado.custoTotal(), resultado.precoSugerido(), resultado.unidades());
    }

    @PostMapping
    public ValorCalculo salvar(@Valid @RequestBody ConfirmacaoCalculo confirmacao) {
        var h = calculos.calcular(preparar(confirmacao.entrada()));
        if (h.getCustoTotal().compareTo(confirmacao.custoEsperado()) != 0
                || h.getPrecoSugerido().compareTo(confirmacao.precoEsperado()) != 0)
            throw new RegraNegocioException("preco_alterado", "Os custos mudaram. Calcule novamente para conferir o preço atualizado.", HttpStatus.CONFLICT);
        h.setTemFoto(false);
        var salvo = historico.create(h);
        return new ValorCalculo(salvo.getId(), salvo.getNomePeca(), salvo.getCustoTotal(), salvo.getPrecoSugerido(), salvo.getUnidades());
    }

    @GetMapping
    public PaginaResponse<HistoricoListaResponse> listar(@RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="") String busca) {
        var ordem = Sort.by(Sort.Order.desc("dataCalculo"), Sort.Order.asc("id"));
        return PaginaResponse.de(catalogo.buscarHistorico(busca.trim(), null, "", "TODOS", "TODOS",
                PaginaResponse.pedido(pagina, 12, ordem)));
    }
}
