package br.com.criare3d.service;

import br.com.criare3d.dto.AjusteEstoqueCalculoResponse;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.*;
import br.com.criare3d.service.interfaces.FilamentoService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProducaoService {
    @org.springframework.beans.factory.annotation.Autowired private MovimentacaoService movimentos;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final CalculoHistoricoRepository calculoHistoricoRepository;
    private final VendaCalculoRepository vendaCalculoRepository;
    private final FilamentoService filamentoService;
    private final Clock clock;
    private final ItemPedidoRepository reservas;
    public ProducaoService(
            CalculoHistoricoRepository calculoHistoricoRepository,
            VendaCalculoRepository vendaCalculoRepository,
            FilamentoService filamentoService,
            Clock clock, ItemPedidoRepository reservas) {
        this.reservas=java.util.Objects.requireNonNull(reservas);
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        this.calculoHistoricoRepository = calculoHistoricoRepository;
        this.vendaCalculoRepository = vendaCalculoRepository;
        this.filamentoService = filamentoService;
    }

    public ResultadoOperacao atualizarProduzido(UUID id, boolean produzido) {
        CalculoHistorico bloqueado = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
        if (bloqueado == null) return ResultadoOperacao.NAO_ENCONTRADO;
        List<AjusteEstoqueCalculoResponse> dados =
                calculoHistoricoRepository.buscarDadosParaAtualizarStatus(id);
        if (dados.isEmpty()) return ResultadoOperacao.NAO_ENCONTRADO;

        AjusteEstoqueCalculoResponse historico = dados.get(0);
        boolean jaProduzido = Boolean.TRUE.equals(historico.produzido());
        if (jaProduzido == produzido) return ResultadoOperacao.OK;

        if (!produzido) {
            if (reservas.reservadas(id)>0) return ResultadoOperacao.RESERVA_ATIVA;
            CalculoHistorico calculo = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
            if (calculo == null) return ResultadoOperacao.NAO_ENCONTRADO;
            if (Boolean.TRUE.equals(historico.vendido())
                    || valorOuZero(calculo.getUnidadesVendidas()) > 0
                    || vendaCalculoRepository.existeVendaAtiva(id)) {
                return ResultadoOperacao.VENDA_ATIVA;
            }
        }

        if (produzido && !ajustarEstoque(dados, false,bloqueado)) {
            return ResultadoOperacao.ESTOQUE_INSUFICIENTE;
        }
        if (!produzido) ajustarEstoque(dados, true,bloqueado);

        bloqueado.setProduzido(produzido);
        bloqueado.setDataProducao(produzido ? LocalDateTime.now(clock) : null);
        if (produzido) reservas.concluirPlanejamentoPorLote(id);
        return ResultadoOperacao.OK;
    }

    private int valorOuZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private boolean ajustarEstoque(List<AjusteEstoqueCalculoResponse> dados, boolean devolver,CalculoHistorico lote) {
        Map<UUID, BigDecimal> quantidades = new HashMap<>();
        for (AjusteEstoqueCalculoResponse item : dados) {
            if (item.filamentoItemId() != null && item.quantidadeItem() != null) {
                quantidades.merge(item.filamentoItemId(), item.quantidadeItem(), BigDecimal::add);
            }
        }

        // Compatibilidade com históricos antigos sem itens de filamento vinculados.
        if (quantidades.isEmpty()) {
            AjusteEstoqueCalculoResponse principal = dados.get(0);
            if (principal.filamentoPrincipalId() != null && principal.quantidadePrincipal() != null) {
                quantidades.put(principal.filamentoPrincipalId(), principal.quantidadePrincipal());
            }
        }

        if (!devolver && quantidades.isEmpty()) return false;

        Map<UUID, Filamento> filamentos = new HashMap<>();
        for (Filamento filamento : filamentoService.fetchByIds(quantidades.keySet())) {
            filamentos.put(filamento.getId(), filamento);
        }
        if (!devolver && filamentos.size() != quantidades.size()) return false;

        if (!devolver) {
            for (Map.Entry<UUID, Filamento> entrada : filamentos.entrySet()) {
                BigDecimal disponivel = entrada.getValue().getEstoqueGramas() == null
                        ? ZERO : entrada.getValue().getEstoqueGramas();
                if (disponivel.compareTo(quantidades.get(entrada.getKey())) < 0) return false;
            }
        }

        quantidades.forEach((filamentoId, quantidade) -> {
            Filamento filamento = filamentos.get(filamentoId);
            if (filamento == null) return;
            BigDecimal saldo = filamento.getEstoqueGramas() == null
                    ? ZERO : filamento.getEstoqueGramas();
            filamento.setEstoqueGramas(devolver ? saldo.add(quantidade) : saldo.subtract(quantidade));
            if(movimentos!=null)movimentos.registrar(devolver?"DESFAZER_PRODUCAO":"PRODUCAO",filamento.getNome()+" · "+filamento.getCor()+" — "+lote.getNomePeca(),devolver?quantidade:quantidade.negate(),"g","lote",lote.getId(),filamento.getId(),filamento.getEstoqueGramas());
        });
        return true;
    }
}
