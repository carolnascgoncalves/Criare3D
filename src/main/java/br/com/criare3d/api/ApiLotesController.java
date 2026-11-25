package br.com.criare3d.api;

import br.com.criare3d.service.*;
import br.com.criare3d.exception.RegraNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.math.BigDecimal;
import java.util.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/vendas")
@RequiredArgsConstructor
@SecurityRequirement(name="bearerAuth")
public class ApiLotesController {
    private final ConsultaLoteService consulta;
    private final VendaService vendas;
    private final ProducaoService producao;
    private final CustoPersonalizadoService custos;
    private final FalhaProducaoService falhas;
    private final IdempotenciaService operacoes;

    public record CustoLote(String descricao, BigDecimal valor, boolean cancelado) {}
    public record VendaLote(UUID id, boolean vinculadaPedido, LocalDateTime data, int unidades, int canceladas, BigDecimal preco, BigDecimal lucro) {}
    public record DetalheLote(String nome, String impressora, boolean produzido, int unidades, int vendidas,
        long reservadas, long disponiveis, BigDecimal custoUnitario, BigDecimal custoLote, BigDecimal preco,
        BigDecimal lucroPrevisto, BigDecimal lucroVendas, BigDecimal totalExtras, List<ConsultaLoteService.Material> materiais,
        List<CustoLote> incluidos,List<CustoLote> personalizados,List<VendaLote> vendas) {}

    @GetMapping("/{id}")
    @Transactional(readOnly=true)
    public DetalheLote abrir(@PathVariable UUID id){
        var op=consulta.buscar(id).orElseThrow();
        var c=op.lote();
        boolean produzido=Boolean.TRUE.equals(c.getProduzido());
        var extras=new ArrayList<CustoLote>();
        op.extras().forEach(x->extras.add(new CustoLote(x.getDescricao(),x.getValor(),x.isCancelado())));
        op.perdas().forEach(x->extras.add(new CustoLote("Perda de filamento · "+x.getGramas()+" g",x.getCustoMaterial(),x.isCancelada())));
        var lucroPadrao = c.getLucroLiquido();
        if (lucroPadrao == null && c.getPrecoSugerido() != null) lucroPadrao = c.getPrecoSugerido().subtract(c.getCustoTotal());
        final var lucroUnitarioOriginal = lucroPadrao;
        return new DetalheLote(c.getNomePeca(),c.getNomeImpressora(),produzido,c.getUnidades(),c.getUnidadesVendidas(),
            op.reservadas(),produzido?Math.max(0L,c.getUnidades()-c.getUnidadesVendidas()-op.reservadas()):0,
            c.getCustoTotal(),c.getCustoTotal().multiply(BigDecimal.valueOf(c.getUnidades())),
            c.getPrecoVendaPlataforma()!=null?c.getPrecoVendaPlataforma():c.getPrecoSugerido(),
            op.lucroPrevisto(),op.lucroVendas(),op.totalExtras(),op.materiais(),
            c.getRateiosCustos().stream().map(x->new CustoLote(x.getDescricao(),x.getValorUnitario(),false)).toList(),extras,
            op.vendas().stream().map(x->new VendaLote(x.getId(), x.getPedidoId()!=null, x.getDataVenda(),x.getUnidadesAtivas(),x.getUnidadesCanceladas(),x.getPrecoUnitario(),x.getLucroUnitario()!=null ? x.getLucroUnitario() : lucroUnitarioOriginal)).toList());
    }

    public record RegistroVenda(@Min(1) int unidades,@PositiveOrZero BigDecimal preco,@PositiveOrZero BigDecimal desconto,@PositiveOrZero BigDecimal taxas,@NotNull UUID chave) {}
    public record RegistroCusto(@NotBlank @Size(min=2,max=150) String descricao,@NotNull @Positive BigDecimal valor,@NotNull UUID chave) {}
    public record RegistroPerda(@NotNull UUID filamentoId,@Min(1) int unidades,@NotNull @Positive BigDecimal gramas,@PositiveOrZero long minutos,@Size(max=250) String observacao,@NotNull UUID chave) {}

    @PostMapping("/{id}/producao") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void produzir(@PathVariable UUID id){conferir(producao.atualizarProduzido(id,true));}

    @PostMapping("/{id}/venda") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vender(@PathVariable UUID id,@Valid @RequestBody RegistroVenda r){
        conferir(vendas.registrarVenda(id,r.unidades(),r.preco(),r.desconto(),r.taxas(),r.chave()));
    }

    public record CancelamentoVenda(@NotNull UUID vendaId, @Min(1) int unidades, @NotNull UUID chave) {}

    @PostMapping("/{id}/cancelamento") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelarVendaLote(@PathVariable UUID id, @Valid @RequestBody CancelamentoVenda r) {
        operacoes.executar(r.chave(), "CANCELAR_VENDA_LOTE", id+"|"+r, () -> {
            conferir(vendas.cancelarVenda(id, r.vendaId(), r.unidades()));
            return "OK";
        });
    }

    @PostMapping("/{id}/custos") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void custo(@PathVariable UUID id,@Valid @RequestBody RegistroCusto r){
        operacoes.executar(r.chave(),"CUSTO_LOTE",id+"|"+r,()->{custos.registrar(id,r.descricao(),r.valor());return "OK";});
    }

    @PostMapping("/{id}/perdas") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void perda(@PathVariable UUID id,@Valid @RequestBody RegistroPerda r){
        operacoes.executar(r.chave(),"PERDA_LOTE",id+"|"+r,()->{falhas.registrar(id,r.filamentoId(),r.unidades(),r.gramas(),r.minutos(),r.observacao());return "OK";});
    }

    private void conferir(ResultadoOperacao resultado){
        if(resultado==ResultadoOperacao.OK)return;
        String mensagem=switch(resultado){
            case RESERVA_ATIVA -> "Há unidades reservadas para pedidos.";
            case ESTOQUE_INSUFICIENTE -> "Saldo de filamento insuficiente para produzir o lote.";
            case PRODUCAO_NECESSARIA -> "Registre a produção antes da venda.";
            case QUANTIDADE_VENDA_ACIMA_DISPONIVEL -> "A quantidade excede o saldo disponível.";
            case VALOR_VENDA_INVALIDO -> "Confira o preço, o desconto e as taxas da venda.";
            default -> "Não foi possível registrar a operação. Confira o lote.";
        };
        throw new RegraNegocioException(resultado.codigo(),mensagem,HttpStatus.CONFLICT);
    }
}
