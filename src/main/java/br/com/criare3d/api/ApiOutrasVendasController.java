package br.com.criare3d.api;

import br.com.criare3d.models.VendaProdutoLoja;
import br.com.criare3d.repository.ProdutoLojaRepository;
import br.com.criare3d.repository.VendaProdutoLojaRepository;
import br.com.criare3d.service.OutrasVendasService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/outras-vendas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Transactional(readOnly = true)
public class ApiOutrasVendasController {
    private final ProdutoLojaRepository produtos;
    private final VendaProdutoLojaRepository vendas;
    private final OutrasVendasService service;

    public record Produto(UUID id, String nome, BigDecimal preco, BigDecimal custo, boolean arquivado) {}
    public record Venda(UUID id, String nome, int quantidade, int ativas, int canceladas, BigDecimal precoUnitario,
                        BigDecimal custoUnitario, BigDecimal receita, BigDecimal lucro, String nomeEmbalagem,
                        BigDecimal custoEmbalagem, LocalDate data) {}
    public record Lista<T>(List<T> itens, long total, int pagina, int tamanho) {}
    public record ProdutoEntrada(UUID id, @NotBlank @Size(min=2,max=150) String nome,
                                 @NotNull @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal preco,
                                 @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal custo) {}
    public record VendaEntrada(@NotNull UUID produtoId, @Min(1) @Max(10000) int quantidade,
                               @PositiveOrZero BigDecimal preco, @PositiveOrZero BigDecimal custo,
                               LocalDate data, @NotNull VendaProdutoLoja.TipoEmbalagem tipoEmbalagem,
                               UUID embalagemId, @PositiveOrZero BigDecimal valorEmbalagem,
                               @Min(0) @Max(10000) int quantidadeEmbalagens, @NotNull UUID chave) {}
    public record Cancelamento(@Min(1) int quantidade) {}

    @GetMapping("/produtos")
    public Lista<Produto> produtos(@RequestParam(defaultValue="false") boolean arquivados,
                                   @RequestParam(defaultValue="0") int pagina) {
        var page = arquivados ? produtos.findByArquivadoTrueOrderByNomeAsc(PageRequest.of(Math.max(0,pagina),20))
                : produtos.findByArquivadoFalseOrderByNomeAsc(PageRequest.of(Math.max(0,pagina),20));
        return new Lista<>(page.getContent().stream().map(p -> new Produto(p.getId(),p.getNome(),p.getPreco(),p.getCusto(),p.isArquivado())).toList(),
                page.getTotalElements(),page.getNumber(),page.getSize());
    }

    @GetMapping("/vendas")
    public Lista<Venda> vendas(@RequestParam(defaultValue="") String busca,@RequestParam(defaultValue="0") int pagina) {
        var page = vendas.pesquisar(busca.strip(),PageRequest.of(Math.max(0,pagina),20));
        return new Lista<>(page.getContent().stream().map(v -> new Venda(v.getId(),v.getNomeProduto(),v.getQuantidade(),v.getAtivas(),v.getCanceladas(),
                v.getPrecoUnitario(),v.getCustoUnitario(),v.getReceita(),v.getLucro(),v.getNomeEmbalagem(),v.getCustoEmbalagemAtivas(),v.getDataVenda().toLocalDate())).toList(),
                page.getTotalElements(),page.getNumber(),page.getSize());
    }

    @PostMapping("/produtos")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public Produto salvar(@Valid @RequestBody ProdutoEntrada entrada) {
        var p = service.salvar(entrada.id(),entrada.nome(),entrada.preco(),entrada.custo());
        return new Produto(p.getId(),p.getNome(),p.getPreco(),p.getCusto(),p.isArquivado());
    }

    @PutMapping("/produtos/{id}/estado")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void estado(@PathVariable UUID id,@RequestParam boolean arquivado) { service.arquivar(id,arquivado); }

    @PostMapping("/vendas")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public UUID vender(@Valid @RequestBody VendaEntrada v) {
        return service.vender(v.produtoId(),v.quantidade(),v.preco(),v.custo(),v.data(),v.tipoEmbalagem(),v.embalagemId(),
                v.valorEmbalagem(),v.quantidadeEmbalagens(),v.chave()).getId();
    }

    @PostMapping("/vendas/{id}/cancelamento")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable UUID id,@Valid @RequestBody Cancelamento c) { service.cancelar(id,c.quantidade()); }
}
