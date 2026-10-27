
package br.com.criare3d.service;

import br.com.criare3d.dto.CalculoRequest;
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
        return calculoHistoricoRepository.countByFilamento_Id(filamentoId);
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

        Filamento filamento = filamentoService.fetchById(request.getFilamentoId())
                .orElseThrow(() -> new IllegalArgumentException("Filamento não encontrado"));

        Impressora impressora = impressoraService.fetchById(request.getImpressoraId())
                .orElseThrow(() -> new IllegalArgumentException("Impressora não encontrada"));

        Configuracao configuracao = configuracaoService.buscarOuCriarConfiguracao();

        BigDecimal quantidade = request.getQuantidadeFilamentoGramas();
        BigDecimal tempo = BigDecimal.valueOf(request.getTempoImpressaoMinutos());

        if (filamento.getPesoRoloGramas() == null
                || filamento.getPesoRoloGramas().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O peso do rolo deve ser maior que zero");
        }

        BigDecimal custoFilamento = quantidade
                .multiply(filamento.getPrecoRolo())
                .divide(filamento.getPesoRoloGramas(), 2, RoundingMode.HALF_UP);

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

        BigDecimal custoTotal = custoFilamento
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

        CalculoHistorico historico = CalculoRequest.toEntity(request);

        historico.setPeca(peca);
        historico.setNomePeca(peca.getNome());

        historico.setFilamento(filamento);
        historico.setNomeFilamento(filamento.getNome());
        historico.setPrecoRoloFilamento(filamento.getPrecoRolo());
        historico.setPesoRoloGramas(filamento.getPesoRoloGramas());
        historico.setQuantidadeFilamentoGramas(quantidade);
        historico.setCustoFilamento(custoFilamento);

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
        historico.setDataCalculo(LocalDateTime.now());

        return historico;
    }

    @Override
    public Page<CalculoHistorico> fetchAll(Pageable pageable) {
        return calculoHistoricoRepository.findAll(pageable);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<CalculoHistorico> fetchById(UUID id) {
        return calculoHistoricoRepository.findById(id);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean existsById(UUID id) {
        return calculoHistoricoRepository.existsById(id);
    }
}
