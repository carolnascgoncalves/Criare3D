package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ConsultaLoteService {
    private final CalculoHistoricoRepository repository;
    private final FalhaProducaoRepository falhas;
    private final VendaCalculoRepository vendas;
    private final CustoPersonalizadoRepository custos;
    private final ItemPedidoRepository reservas;
    public record Material(String nome, BigDecimal gramas, BigDecimal saldo, String cor, String codigoCor,
                           BigDecimal custo) {
        public Material(String nome, BigDecimal gramas, BigDecimal saldo) {
            this(nome, gramas, saldo, null, null, null);
        }

        public String getCorSegura() {
            return codigoCor != null && codigoCor.matches("#[0-9a-fA-F]{6}") ? codigoCor : "#cccccc";
        }

        public boolean isInsuficiente() {
            return saldo != null && saldo.compareTo(gramas) < 0;
        }

        public BigDecimal getFalta() {
            return saldo == null ? BigDecimal.ZERO : gramas.subtract(saldo).max(BigDecimal.ZERO);
        }
    }


    public record Operacao(CalculoHistorico lote, long reservadas, List<Material> materiais,
        List<FalhaProducao> perdas, List<CustoPersonalizado> extras, List<VendaCalculo> vendas,
        BigDecimal totalExtras, BigDecimal lucroPrevisto, BigDecimal lucroVendas, BigDecimal perdaMaterial) {}

    @Transactional(readOnly=true)
    public Optional<Operacao> buscar(UUID id) {
        var c=repository.findById(id).orElse(null);
        if(c==null)return Optional.empty();
        var materiais = new java.util.ArrayList<>(c.getItensFilamento().stream().map(i -> new Material(i.getNomeFilamento(),
            i.getQuantidadeFilamentoGramas().add(i.getPurgaGramas() == null ? BigDecimal.ZERO : i.getPurgaGramas()).multiply(BigDecimal.valueOf(c.getUnidades())),
            i.getFilamento() == null ? null : i.getFilamento().getEstoqueGramas(),
            i.getCorFilamento(), i.getCodigoCor(), i.getCustoFilamento().multiply(BigDecimal.valueOf(c.getUnidades())))).toList());
        if (materiais.isEmpty()) materiais.add(new Material(c.getNomeFilamento(),
            c.getQuantidadeFilamentoGramas().multiply(BigDecimal.valueOf(c.getUnidades())),
            c.getFilamento() == null ? null : c.getFilamento().getEstoqueGramas(),
            c.getFilamento() == null ? null : c.getFilamento().getCor(),
            c.getFilamento() == null ? null : c.getFilamento().getCodigoCor(), c.getCustoFilamento().multiply(BigDecimal.valueOf(c.getUnidades()))));
        c.getRateiosCustos().size();
        var perdas = falhas.findByCalculo_IdOrderByDataRegistroDesc(id);
        var custosExtras = custos.findByCalculo_IdOrderByDataRegistroDesc(id);
        var totalExtras = custosExtras.stream().filter(custo -> !custo.isCancelado()).map(custo -> custo.getValor()).reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(perdas.stream().filter(f -> !f.isCancelada()).map(f -> f.getCustoMaterial()).reduce(BigDecimal.ZERO, BigDecimal::add));
        var registrosVenda = vendas.findByCalculo_IdOrderByDataVendaDesc(id);
        var estimado = c.getLucroLiquido() != null ? c.getLucroLiquido() : c.getPrecoSugerido().subtract(c.getCustoTotal());
        var resultadoVendas = registrosVenda.stream().map(v -> (v.getLucroUnitario() == null ? estimado : v.getLucroUnitario()).multiply(BigDecimal.valueOf(v.getUnidadesAtivas()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(new Operacao(c, reservas.reservadas(id), materiais, perdas, custosExtras, registrosVenda,
            totalExtras, estimado.multiply(BigDecimal.valueOf(c.getUnidadesVendidas())), resultadoVendas,
            perdas.stream().filter(f -> !f.isCancelada()).map(f -> f.getCustoMaterial()).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }
}
