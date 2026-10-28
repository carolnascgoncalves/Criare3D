
package br.com.criare3d.service;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.service.interfaces.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class CalculoHistoricoServiceImp implements CalculoHistoricoService {

    @Autowired
    private CalculoHistoricoRepository calculoHistoricoRepository;

    @Autowired
    private PecaService pecaService;

    @Autowired
    private FilamentoService filamentoService;

    @Autowired
    private ImpressoraService impressoraService;

    @Autowired
    private EmbalagemService embalagemService;

    @Autowired
    private ConfiguracaoService configuracaoService;

    @Override
    public long contarPorFilamento(UUID filamentoId) {
        return calculoHistoricoRepository.contarUsosDosItensPorFilamento(filamentoId);
    }

    @Override
    public long contarPorEmbalagem(UUID embalagemId) {
        return calculoHistoricoRepository.countByEmbalagem_Id(embalagemId);
    }

    @Override
    public long contarPorImpressora(UUID impressoraId) {
        return calculoHistoricoRepository.countByImpressora_Id(impressoraId);
    }

    @Override
    public long contarPorPeca(UUID pecaId) {
        return calculoHistoricoRepository.countByPeca_Id(pecaId);
    }

    @Override
    public CalculoHistorico create(CalculoHistorico calculoHistorico) {
        return calculoHistoricoRepository.save(calculoHistorico);
    }

    @Override
    public void delete(UUID id) {
        calculoHistoricoRepository.deleteById(id);
    }


    public CalculoHistorico calcular(CalculoRequest request) {
        Peca peca = pecaService.fetchById(request.getPecaId())
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));

        if (request.getFilamentos() == null || request.getFilamentos().isEmpty()) {
            throw new IllegalArgumentException("Adicione pelo menos um filamento");
        }

        Impressora impressora = impressoraService.fetchById(request.getImpressoraId())
                .orElseThrow(() -> new IllegalArgumentException("Impressora não encontrada"));

        Configuracao configuracao = configuracaoService.buscarOuCriarConfiguracao();

        if (impressora.getPotenciaWatts() == null
                || impressora.getPotenciaWatts().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "A potência da impressora deve ser maior que zero");
        }

        if (configuracao.getTarifaEnergiaKwh() == null
                || configuracao.getTarifaEnergiaKwh().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "A tarifa de energia não pode ser negativa ou nula");
        }

        if (configuracao.getMarkupPercentual() == null
                || configuracao.getMarkupPercentual().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "O percentual de lucro não pode ser negativo ou nulo");
        }

        BigDecimal tempo = BigDecimal.valueOf(request.getTempoImpressaoMinutos());
        BigDecimal custoFilamentoTotal = BigDecimal.ZERO;

        CalculoHistorico historico = CalculoHistorico.builder()
                .itensFilamento(new java.util.ArrayList<>())
                .build();

        for (ItemCalculoRequest itemRequest : request.getFilamentos()) {
            if (itemRequest.getFilamentoId() == null) {
                throw new IllegalArgumentException("Selecione todos os filamentos");
            }

            BigDecimal quantidade = itemRequest.getQuantidadeFilamentoGramas();

            if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "A quantidade de cada filamento deve ser maior que zero");
            }

            Filamento filamento = filamentoService.fetchById(itemRequest.getFilamentoId())
                    .orElseThrow(() -> new IllegalArgumentException("Filamento não encontrado"));

            if (filamento.getPesoRoloGramas() == null
                    || filamento.getPesoRoloGramas().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "O peso do rolo deve ser maior que zero: " + filamento.getNome());
            }

            if (filamento.getPrecoRolo() == null
                    || filamento.getPrecoRolo().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "O preço do rolo é inválido: " + filamento.getNome());
            }

            BigDecimal custoIndividual = quantidade
                    .multiply(filamento.getPrecoRolo())
                    .divide(filamento.getPesoRoloGramas(), 2, RoundingMode.HALF_UP);

            ItemCalculoHistorico item = ItemCalculoHistorico.builder()
                    .calculoHistorico(historico)
                    .filamento(filamento)
                    .nomeFilamento(filamento.getNome())
                    .corFilamento(filamento.getCor())
                    .codigoCor(filamento.getCodigoCor())
                    .precoRoloFilamento(filamento.getPrecoRolo())
                    .pesoRoloGramas(filamento.getPesoRoloGramas())
                    .quantidadeFilamentoGramas(quantidade)
                    .custoFilamento(custoIndividual)
                    .build();

            historico.getItensFilamento().add(item);
            custoFilamentoTotal = custoFilamentoTotal.add(custoIndividual);
        }

        custoFilamentoTotal = custoFilamentoTotal.setScale(2, RoundingMode.HALF_UP);

        ItemCalculoHistorico primeiroItem = historico.getItensFilamento().get(0);

        historico.setFilamento(primeiroItem.getFilamento());
        historico.setNomeFilamento(primeiroItem.getNomeFilamento());
        historico.setPrecoRoloFilamento(primeiroItem.getPrecoRoloFilamento());
        historico.setPesoRoloGramas(primeiroItem.getPesoRoloGramas());
        historico.setQuantidadeFilamentoGramas(primeiroItem.getQuantidadeFilamentoGramas());
        historico.setCustoFilamento(custoFilamentoTotal);

        BigDecimal custoEnergia = impressora.getPotenciaWatts()
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(tempo)
                .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP)
                .multiply(configuracao.getTarifaEnergiaKwh())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal custoEmbalagem;
        String nomeEmbalagem;
        Embalagem embalagem = null;

        if (Boolean.TRUE.equals(configuracao.getEmbalagemFixa())) {
            custoEmbalagem = configuracao.getCustoEmbalagemFixa();

            if (custoEmbalagem == null || custoEmbalagem.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("O custo fixo da embalagem não pode ser negativo ou nulo");
            }
            nomeEmbalagem = "Embalagem (valor fixo)";
        } else {
            if (request.getEmbalagemId() == null) {
                throw new IllegalArgumentException("Selecione uma embalagem");
            }

            embalagem = embalagemService.fetchById(request.getEmbalagemId())
                    .orElseThrow(() -> new IllegalArgumentException("Embalagem não encontrada"));

            custoEmbalagem = embalagem.getCustoUnitario();
            nomeEmbalagem = embalagem.getNome();
        }

        BigDecimal custoTotal = custoFilamentoTotal
                .add(custoEnergia)
                .add(custoEmbalagem)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal fatorMarkup = BigDecimal.ONE.add(
                configuracao.getMarkupPercentual()
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
        );

        BigDecimal precoSugerido = custoTotal
                .multiply(fatorMarkup)
                .setScale(2, RoundingMode.HALF_UP);

        historico.setPeca(peca);
        historico.setNomePeca(peca.getNome());

        historico.setImpressora(impressora);
        historico.setNomeImpressora(impressora.getNome());
        historico.setPotenciaWatts(impressora.getPotenciaWatts());
        historico.setTempoImpressaoMinutos(request.getTempoImpressaoMinutos());

        historico.setTarifaEnergiaKwh(configuracao.getTarifaEnergiaKwh());
        historico.setCustoEnergia(custoEnergia);

        historico.setEmbalagem(embalagem);
        historico.setNomeEmbalagem(nomeEmbalagem);
        historico.setCustoEmbalagem(custoEmbalagem);

        historico.setCustoTotal(custoTotal);
        historico.setMarkupPercentual(configuracao.getMarkupPercentual());
        historico.setPrecoSugerido(precoSugerido);
        historico.setDataCalculo(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));

        return historico;
    }


    @Override
    public Page<CalculoHistorico> fetchAll(Pageable pageable) {
        return calculoHistoricoRepository.findAll(pageable);
    }

    @Override
    public Optional<CalculoHistorico> fetchById(UUID id) {
        return calculoHistoricoRepository.buscarPorIdComItens(id);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean existsById(UUID id) {
        return calculoHistoricoRepository.existsById(id);
    }
}
