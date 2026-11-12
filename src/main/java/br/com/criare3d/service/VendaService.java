package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

@Service
@Transactional
public class VendaService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final CalculoHistoricoRepository calculoHistoricoRepository;
    private final VendaCalculoRepository vendaCalculoRepository;
    @Autowired private Clock clock = Clock.system(ZoneId.of("America/Sao_Paulo"));
    @Autowired(required = false) private PlanejamentoProducaoRepository planejamentoProducaoRepository;
    public VendaService(CalculoHistoricoRepository calculoHistoricoRepository, VendaCalculoRepository vendaCalculoRepository) {
        this.calculoHistoricoRepository = calculoHistoricoRepository;
        this.vendaCalculoRepository = vendaCalculoRepository;
    }

    public ResultadoOperacao atualizarVendido(UUID id, boolean vendido) {
        CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
        if (calculo == null) return ResultadoOperacao.NAO_ENCONTRADO;
        if (!vendido) return cancelarVendas(id);
        int restantes = Math.max(0, calculo.getUnidades() - valorOuZero(calculo.getUnidadesVendidas()));
        return restantes == 0 ? ResultadoOperacao.OK : registrarVenda(id, restantes);
    }

    public ResultadoOperacao registrarVenda(UUID id, Integer unidades) {
        return registrarVenda(id, unidades, null, null, null);
    }

    public ResultadoOperacao registrarVenda(UUID id, Integer unidades, BigDecimal preco, BigDecimal desconto, BigDecimal taxas) {
        for (BigDecimal valor : new BigDecimal[]{preco, desconto, taxas}) {
            if (valor != null && (valor.signum() < 0 || valor.compareTo(new BigDecimal("99999999")) > 0)) return ResultadoOperacao.VALOR_VENDA_INVALIDO;
        }
        if (unidades == null || unidades < 1) return ResultadoOperacao.QUANTIDADE_VENDA_INVALIDA;
        CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
        if (calculo == null) return ResultadoOperacao.NAO_ENCONTRADO;

        int vendidas = valorOuZero(calculo.getUnidadesVendidas());
        int restantes = calculo.getUnidades() - vendidas;
        if (unidades > restantes) return ResultadoOperacao.QUANTIDADE_VENDA_ACIMA_DISPONIVEL;

        if (!Boolean.TRUE.equals(calculo.getProduzido())) return ResultadoOperacao.PRODUCAO_NECESSARIA;

        LocalDateTime agora = LocalDateTime.now(clock);
        VendaCalculo venda = new VendaCalculo();
        venda.setCalculo(calculo);
        venda.setUnidades(unidades);
        venda.setDataVenda(agora);
        BigDecimal sugerido = calculo.getPrecoVendaPlataforma() != null ? calculo.getPrecoVendaPlataforma() : calculo.getPrecoSugerido();
        BigDecimal descontoAplicado = desconto == null ? ZERO : desconto;
        BigDecimal precoAplicado = preco == null ? sugerido : preco;
        if (precoAplicado == null || descontoAplicado.compareTo(precoAplicado) > 0) return ResultadoOperacao.VALOR_VENDA_INVALIDO;
        BigDecimal taxasAplicadas = taxas == null ? (calculo.getTaxasPlataforma() == null ? ZERO : calculo.getTaxasPlataforma()) : taxas;
        venda.setPrecoUnitario(precoAplicado);
        venda.setDescontoUnitario(descontoAplicado);
        venda.setTaxasUnitarias(taxasAplicadas);
        venda.setValoresReais(preco != null && taxas != null);
        venda.setLucroUnitario(precoAplicado.subtract(descontoAplicado).subtract(taxasAplicadas)
                .subtract(calculo.getCustoTotal()));
        vendaCalculoRepository.save(venda);

        int totalVendido = vendidas + unidades;
        calculo.setUnidadesVendidas(totalVendido);
        calculo.setVendido(totalVendido > 0);
        calculo.setDataVenda(agora);
        if (planejamentoProducaoRepository != null && totalVendido >= calculo.getUnidades()) {
            planejamentoProducaoRepository.concluirPorCalculo(id);
        }
        return ResultadoOperacao.OK;
    }

    public ResultadoOperacao cancelarVendas(UUID id) {
        CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
        if (calculo == null) return ResultadoOperacao.NAO_ENCONTRADO;
        for (VendaCalculo venda : vendaCalculoRepository.findByCalculo_IdOrderByDataVendaDesc(id)) {
            venda.setUnidadesCanceladas(venda.getUnidades());
            venda.setDataCancelamento(LocalDateTime.now(clock));
        }
        recalcularVendas(calculo);
        return ResultadoOperacao.OK;
    }

    private int valorOuZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    public ResultadoOperacao cancelarVenda(UUID calculoId, UUID vendaId, Integer unidades) {
        CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(calculoId).orElse(null);
        if (calculo == null) return ResultadoOperacao.NAO_ENCONTRADO;
        VendaCalculo venda = vendaCalculoRepository.findById(vendaId).orElse(null);
        if (venda == null || !calculoId.equals(venda.getCalculo().getId())) return ResultadoOperacao.NAO_ENCONTRADO;
        if (unidades == null || unidades < 1 || unidades > venda.getUnidadesAtivas()) return ResultadoOperacao.QUANTIDADE_VENDA_INVALIDA;
        venda.setUnidadesCanceladas(venda.getUnidadesCanceladas() + unidades);
        venda.setDataCancelamento(LocalDateTime.now(clock));
        recalcularVendas(calculo);
        return ResultadoOperacao.OK;
    }

    private void recalcularVendas(CalculoHistorico calculo) {
        List<VendaCalculo> vendas = vendaCalculoRepository.findByCalculo_IdOrderByDataVendaDesc(calculo.getId());
        int ativas = vendas.stream().mapToInt(VendaCalculo::getUnidadesAtivas).sum();
        calculo.setUnidadesVendidas(ativas);
        calculo.setVendido(ativas > 0);
        calculo.setDataVenda(vendas.stream().filter(v -> v.getUnidadesAtivas() > 0)
                .map(VendaCalculo::getDataVenda).max(LocalDateTime::compareTo).orElse(null));
    }
}
