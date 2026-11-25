package br.com.criare3d.api.extrato;

import br.com.criare3d.api.*;
import br.com.criare3d.dto.FiltroMovimentacao;
import br.com.criare3d.models.Movimentacao;
import br.com.criare3d.models.enums.TipoMovimentacao;
import br.com.criare3d.service.MovimentacaoService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/extrato")
@SecurityRequirement(name = "bearerAuth")
public class ApiExtratoController {
    private final MovimentacaoService movimentos;
    private final Clock clock;
    private final ApiDatas datas;

    public ApiExtratoController(MovimentacaoService movimentos, Clock clock, ApiDatas datas) {
        this.movimentos = movimentos;
        this.clock = clock;
        this.datas = datas;
    }

    public record MovimentacaoResponse(
            UUID id,
            String tipo,
            String descricao,
            String quantidade,
            String medida,
            String origem,
            UUID referencia,
            UUID filamentoId,
            String codigoCor,
            String saldoApos,
            OffsetDateTime data) {}

    public record Resumo(String medida, String entrou, String saiu, String saldo, String pecasVendidas) {}

    public record Abertura(long registros, String gramas) {}

    public record Extrato(
            PaginaResponse<MovimentacaoResponse> movimentacoes,
            List<Resumo> resumo,
            Abertura abertura,
            String pecasVendidas) {}

    @GetMapping
    public Extrato listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fim,
            @RequestParam(defaultValue = "") String tipo,
            @RequestParam(defaultValue = "") String direcao,
            @RequestParam(defaultValue = "") String busca,
            @RequestParam(required = false) UUID filamento,
            @RequestParam(defaultValue = "false") boolean saldo,
            @RequestParam(defaultValue = "false") boolean tudo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        if ((!tipo.isEmpty() && !TipoMovimentacao.valido(tipo))
                || !Set.of("", "entrada", "saida").contains(direcao)
                || busca.length() > 150) throw new IllegalArgumentException("Filtro inválido.");
        var hoje = LocalDate.now(clock);
        var filtro =
                new FiltroMovimentacao(
                        inicio == null ? hoje.minusDays(30) : inicio,
                        fim == null ? hoje : fim,
                        tipo,
                        direcao,
                        busca,
                        filamento,
                        saldo,
                        tudo);
        var resultado =
                movimentos.extrato(
                        filtro,
                        PaginaResponse.pedido(
                                pagina, tamanho, Sort.by(Sort.Direction.DESC, "data", "id")));
        var resumo =
                resultado.resumo().stream()
                        .map(
                                r ->
                                        new Resumo(
                                                r.getMedida(),
                                                decimal(r.getEntrou()),
                                                decimal(r.getSaiu()),
                                                decimal(java.util.Objects.requireNonNullElse(r.getEntrou(), BigDecimal.ZERO)
                                                        .add(java.util.Objects.requireNonNullElse(r.getSaiu(), BigDecimal.ZERO))),
                                                decimal(r.getPecasVendidas())))
                        .toList();
        var abertura =
                resultado.abertura() == null
                        ? null
                        : new Abertura(
                                resultado.abertura().getTotal(),
                                decimal(resultado.abertura().getGramas()));
        var cores = movimentos.coresDosFilamentos(resultado.pagina().getContent());
        return new Extrato(
                PaginaResponse.de(resultado.pagina().map(m -> linha(m, cores))),
                resumo,
                abertura,
                decimal(resultado.pecasVendidas()));
    }

    private MovimentacaoResponse linha(Movimentacao m, Map<UUID, String> cores) {
        var material = m.getFilamentoId() != null ? m.getFilamentoId() : "filamento".equals(m.getOrigem()) ? m.getReferencia() : null;
        return new MovimentacaoResponse(
                m.getId(),
                m.getTipo(),
                m.getDescricao(),
                decimal(m.getQuantidade()),
                m.getMedida(),
                m.getOrigem(),
                m.getReferencia(),
                m.getFilamentoId(),
                material == null ? null : cores.get(material),
                m.getSaldoApos() == null ? null : decimal(m.getSaldoApos()),
                datas.converter(m.getData()));
    }

    private static String decimal(BigDecimal valor) {
        return valor == null ? "0" : valor.toPlainString();
    }
}
