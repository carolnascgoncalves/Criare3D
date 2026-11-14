package br.com.criare3d.service;

import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VendaService {
    @org.springframework.beans.factory.annotation.Autowired private IdempotenciaService idempotencia;
    public ResultadoOperacao registrarVenda(UUID id,Integer unidades,BigDecimal preco,BigDecimal desconto,BigDecimal taxas,UUID chave) {
        return ResultadoOperacao.valueOf(idempotencia.executar(chave,"VENDA_3D",Arrays.asList(id,unidades,preco,desconto,taxas).toString(),()->registrarVenda(id,unidades,preco,desconto,taxas).name()));
    }
    @org.springframework.beans.factory.annotation.Autowired private MovimentacaoService movimentos;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final CalculoHistoricoRepository calculoHistoricoRepository;
    private final VendaCalculoRepository vendaCalculoRepository;
    private final Clock clock;
    private final ItemPedidoRepository reservas;
    private final PlanejamentoProducaoRepository planejamentoProducaoRepository;
    public VendaService(
            CalculoHistoricoRepository calculoHistoricoRepository,
            VendaCalculoRepository vendaCalculoRepository,
            Clock clock,
            PlanejamentoProducaoRepository planejamentoProducaoRepository, ItemPedidoRepository reservas) {
        this.reservas = java.util.Objects.requireNonNull(reservas);
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        this.planejamentoProducaoRepository = java.util.Objects.requireNonNull(planejamentoProducaoRepository, "planejamentoProducaoRepository");
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
        return registrar(id, unidades, preco, desconto, taxas, null);
    }

    public ResultadoOperacao registrarVendaDoPedido(UUID id, Integer unidades, BigDecimal preco, BigDecimal desconto, BigDecimal taxas, UUID pedidoId) {
        return registrar(id, unidades, preco, desconto, taxas, java.util.Objects.requireNonNull(pedidoId));
    }

    private ResultadoOperacao registrar(UUID id, Integer unidades, BigDecimal preco, BigDecimal desconto, BigDecimal taxas, UUID pedidoId) {
        for (BigDecimal valor : new BigDecimal[]{preco, desconto, taxas}) {
            if (valor != null && (valor.signum() < 0 || valor.compareTo(new BigDecimal("99999999")) > 0)) return ResultadoOperacao.VALOR_VENDA_INVALIDO;
        }
        if (unidades == null || unidades < 1) return ResultadoOperacao.QUANTIDADE_VENDA_INVALIDA;
        CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
        if (calculo == null) return ResultadoOperacao.NAO_ENCONTRADO;

        int vendidas = valorOuZero(calculo.getUnidadesVendidas());
        int restantes = calculo.getUnidades() - vendidas;
        if (unidades > restantes) return ResultadoOperacao.QUANTIDADE_VENDA_ACIMA_DISPONIVEL;
        if (unidades > restantes - reservas.reservadas(id)) return ResultadoOperacao.RESERVA_ATIVA;

        if (!Boolean.TRUE.equals(calculo.getProduzido())) return ResultadoOperacao.PRODUCAO_NECESSARIA;

        LocalDateTime agora = LocalDateTime.now(clock);
        VendaCalculo venda = new VendaCalculo();
        venda.setCalculo(calculo);
        venda.setPedidoId(pedidoId);
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
        if(movimentos!=null)movimentos.registrar("VENDA_3D",calculo.getNomePeca(),BigDecimal.valueOf(unidades).negate(),"un.","lote",id);

        int totalVendido = vendidas + unidades;
        calculo.setUnidadesVendidas(totalVendido);
        calculo.setVendido(totalVendido > 0);
        calculo.setDataVenda(agora);
        if (totalVendido >= calculo.getUnidades()) {
            planejamentoProducaoRepository.concluirPorCalculo(id);
        }
        return ResultadoOperacao.OK;
    }

    public ResultadoOperacao cancelarVendas(UUID id) {
        CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
        if (calculo == null) return ResultadoOperacao.NAO_ENCONTRADO;
        var registros = vendaCalculoRepository.findByCalculo_IdOrderByDataVendaDesc(id);
        if (registros.stream().anyMatch(v -> v.getPedidoId()!=null && v.getUnidadesAtivas()>0)) return ResultadoOperacao.PEDIDO_VINCULADO;
        for (VendaCalculo venda : registros) {
            int devolvidas=venda.getUnidadesAtivas();
            if(movimentos!=null)movimentos.registrar("CANCELAR_VENDA_3D",calculo.getNomePeca(),BigDecimal.valueOf(devolvidas),"un.","lote",id);
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
        return cancelarVenda(calculoId,vendaId,unidades,false);
    }
    public void cancelarPedido(UUID pedidoId) {
        for (var v : vendaCalculoRepository.findByPedidoId(pedidoId)) {
            if (v.getUnidadesAtivas()>0 && cancelarVenda(v.getCalculo().getId(),v.getId(),v.getUnidadesAtivas(),true)!=ResultadoOperacao.OK)
                throw new IllegalArgumentException("Não foi possível cancelar as vendas do pedido.");
        }
    }
    private ResultadoOperacao cancelarVenda(UUID calculoId, UUID vendaId, Integer unidades, boolean peloPedido) {
        CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(calculoId).orElse(null);
        if (calculo == null) return ResultadoOperacao.NAO_ENCONTRADO;
        VendaCalculo venda = vendaCalculoRepository.findById(vendaId).orElse(null);
        if (venda == null || !calculoId.equals(venda.getCalculo().getId())) return ResultadoOperacao.NAO_ENCONTRADO;
        if (venda.getPedidoId()!=null && !peloPedido) return ResultadoOperacao.PEDIDO_VINCULADO;
        if (unidades == null || unidades < 1 || unidades > venda.getUnidadesAtivas()) return ResultadoOperacao.QUANTIDADE_VENDA_INVALIDA;
        venda.setUnidadesCanceladas(venda.getUnidadesCanceladas() + unidades);
        if(movimentos!=null)movimentos.registrar("CANCELAR_VENDA_3D",calculo.getNomePeca(),BigDecimal.valueOf(unidades),"un.","lote",calculoId);
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
