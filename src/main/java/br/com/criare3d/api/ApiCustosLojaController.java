package br.com.criare3d.api;

import br.com.criare3d.controller.CustoLojaController.Formulario;
import br.com.criare3d.models.CustoLoja;
import br.com.criare3d.repository.CustoLojaRepository;
import br.com.criare3d.repository.RateioCustoHistoricoRepository;
import br.com.criare3d.service.RateioCustos;
import br.com.criare3d.service.IdempotenciaService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/custos-loja")
@SecurityRequirement(name="bearerAuth")
@Transactional
public class ApiCustosLojaController {
    @Autowired private CustoLojaRepository custos;
    @Autowired private RateioCustoHistoricoRepository rateios;
    @Autowired private IdempotenciaService operacoes;

    public record CustoResumo(UUID id, String descricao, LocalDate inicio, CustoLoja.Tipo tipo,
            BigDecimal valor, Integer unidadesPrevistas, boolean ativo, BigDecimal meta,
            BigDecimal recuperado, BigDecimal restante, BigDecimal excedente) {}
    public record PecaRateada(UUID calculoId, String nome, BigDecimal valorUnitario, int unidades, int vendidas, BigDecimal recuperado) {}
    public record CustoDetalhe(CustoResumo custo, List<PecaRateada> pecas, long total) {}

    private CustoResumo resumo(CustoLoja c) {
        var recebido = rateios.recuperado(c.getId());
        return new CustoResumo(c.getId(),c.getDescricao(),c.getInicio(),c.getTipo(),c.getValor(),c.getUnidadesPrevistas(),
                c.isAtivo(), c.getMeta(),recebido,RateioCustos.falta(c.getMeta(),recebido),recebido.subtract(c.getMeta()).max(BigDecimal.ZERO));
    }

    @GetMapping
    @Transactional(readOnly=true)
    public List<CustoResumo> listar() {
        return custos.findAllByOrderByInicioDesc().stream().map(this::resumo).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly=true)
    public CustoDetalhe detalhe(@PathVariable UUID id,@RequestParam(defaultValue="0") int pagina) {
        var c=custos.findById(id).orElseThrow();
        var itens=rateios.listarPecas(id,PageRequest.of(Math.max(0,pagina),20)).stream().map(r -> {
            var lote=r.getCalculo();
            return new PecaRateada(lote.getId(),lote.getNomePeca(),r.getValorUnitario(),lote.getUnidades(),
                lote.getUnidadesVendidas(),r.getValorUnitario().multiply(BigDecimal.valueOf(lote.getUnidadesVendidas())));
        }).toList();
        return new CustoDetalhe(resumo(c),itens,rateios.countByCustoLoja_Id(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UUID criar(@RequestParam UUID chave,@Valid @RequestBody Formulario form) {
        var erro=form.validarRateio();
        if (erro!=null) throw new IllegalArgumentException(erro);
        var assinatura=List.of(form.getDescricao(),form.getInicio(),form.getTipo(),form.getMeta(),
                String.valueOf(form.getValor()),String.valueOf(form.getUnidadesPrevistas())).toString();
        return UUID.fromString(operacoes.executar(chave,"CRIAR_CUSTO_LOJA",assinatura,()->custos.save(form.novo()).getId().toString()));
    }

    @PutMapping("/{id}/estado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void estado(@PathVariable UUID id,@RequestParam boolean ativo) {
        custos.findById(id).orElseThrow().setAtivo(ativo);
    }
}
