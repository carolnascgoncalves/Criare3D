package br.com.criare3d.service;

import br.com.criare3d.dto.SimulacaoFilamentoResponse;
import br.com.criare3d.dto.SimulacaoHistoricoResponse;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.repository.ItemCalculoHistoricoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Service
@Transactional(readOnly = true)
public class SimulacaoService {
    private final CalculoHistoricoRepository calculoHistoricoRepository;
    private final ItemCalculoHistoricoRepository itemCalculoHistoricoRepository;
    public SimulacaoService(CalculoHistoricoRepository calculoHistoricoRepository, ItemCalculoHistoricoRepository itemCalculoHistoricoRepository) {
        this.calculoHistoricoRepository = calculoHistoricoRepository;
        this.itemCalculoHistoricoRepository = itemCalculoHistoricoRepository;
    }

    @Transactional(readOnly = true)
    public List<SimulacaoHistoricoResponse> buscarCalculosParaSimulacao(int limite) {
        if (limite <= 0) return List.of();
        List<CalculoHistorico> calculos = calculoHistoricoRepository.findAll(
                PageRequest.of(0, Math.min(limite, 100), Sort.by(Sort.Direction.DESC, "dataCalculo")))
                .getContent();
        return montarDadosSimulacao(calculos);
    }

    @Transactional(readOnly = true)
    public Optional<SimulacaoHistoricoResponse> buscarCalculoParaSimulacao(UUID calculoId) {
        if (calculoId == null) return Optional.empty();
        return calculoHistoricoRepository.buscarPorIdComItens(calculoId).map(this::paraSimulacao);
    }

    private List<SimulacaoHistoricoResponse> montarDadosSimulacao(List<CalculoHistorico> calculos) {
        if (calculos.isEmpty()) return List.of();
        List<UUID> ids = calculos.stream().map(CalculoHistorico::getId).toList();
        Map<UUID, List<ItemCalculoHistorico>> itensPorCalculo = new HashMap<>();
        itemCalculoHistoricoRepository.findByCalculoHistorico_IdIn(ids).forEach(item ->
                itensPorCalculo.computeIfAbsent(item.getCalculoHistorico().getId(), ignorado -> new java.util.ArrayList<>()).add(item));
        return calculos.stream().map(calculo -> paraSimulacao(calculo,
                itensPorCalculo.getOrDefault(calculo.getId(), List.of()))).toList();
    }

    private SimulacaoHistoricoResponse paraSimulacao(CalculoHistorico calculo) {
        return paraSimulacao(calculo, calculo.getItensFilamento());
    }

    private SimulacaoHistoricoResponse paraSimulacao(CalculoHistorico calculo, List<ItemCalculoHistorico> itens) {
        List<SimulacaoFilamentoResponse> filamentos = itens == null
                ? List.of()
                : itens.stream().map(item -> new SimulacaoFilamentoResponse(
                        item.getNomeFilamento(), item.getCorFilamento(), item.getCodigoCor(),
                        item.getQuantidadeFilamentoGramas().add(item.getPurgaGramas() == null
                                ? BigDecimal.ZERO : item.getPurgaGramas())))
                .toList();
        if (filamentos.isEmpty() && calculo.getNomeFilamento() != null) {
            filamentos = List.of(new SimulacaoFilamentoResponse(calculo.getNomeFilamento(), null, null,
                    calculo.getQuantidadeFilamentoGramas() == null ? BigDecimal.ZERO : calculo.getQuantidadeFilamentoGramas()));
        }
        return new SimulacaoHistoricoResponse(calculo.getId(), calculo.getNomePeca(), calculo.getTemFoto(),
                calculo.getProduzido(), calculo.getVendido(), calculo.getDataCalculo(),
                calculo.getNomeImpressora(), calculo.getUnidades(), calculo.getPecasPorImpressao(),
                calculo.getTempoImpressaoMinutos(), calculo.getTempoAquecimentoMinutos(), filamentos);
    }
}
