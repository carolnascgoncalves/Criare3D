package br.com.criare3d.service;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.PlataformaVenda;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import br.com.criare3d.service.interfaces.EmbalagemService;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import br.com.criare3d.service.interfaces.ManutencaoImpressoraService;
import br.com.criare3d.service.interfaces.PecaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static br.com.criare3d.service.CalculoFinanceiro.precisao;
import static br.com.criare3d.service.CalculoFinanceiro.ratear;

@Service
@Transactional(readOnly = true)
public class CalculoService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    @org.springframework.beans.factory.annotation.Autowired
    private java.time.Clock clock = java.time.Clock.system(ZoneId.of("America/Sao_Paulo"));
    private final CalculoPlataformaService calculoPlataformaService;
    private final PecaService pecaService;
    private final FilamentoService filamentoService;
    private final ImpressoraService impressoraService;
    private final EmbalagemService embalagemService;
    private final ConfiguracaoService configuracaoService;
    private final ManutencaoImpressoraService manutencaoImpressoraService;
    public CalculoService(CalculoPlataformaService calculoPlataformaService, PecaService pecaService, FilamentoService filamentoService, ImpressoraService impressoraService, EmbalagemService embalagemService, ConfiguracaoService configuracaoService, ManutencaoImpressoraService manutencaoImpressoraService) {
        this.calculoPlataformaService = calculoPlataformaService;
        this.pecaService = pecaService;
        this.filamentoService = filamentoService;
        this.impressoraService = impressoraService;
        this.embalagemService = embalagemService;
        this.configuracaoService = configuracaoService;
        this.manutencaoImpressoraService = manutencaoImpressoraService;
    }

    public CalculoHistorico calcular(CalculoRequest request) {
        CalculoValidador.validarRequisicao(request);
        Peca peca = pecaService.fetchById(request.getPecaId()).orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));
        Impressora impressora = impressoraService.fetchById(request.getImpressoraId()).orElseThrow(() -> new IllegalArgumentException("Impressora não encontrada"));
        Configuracao configuracao = configuracaoService.buscarConfiguracao().orElseGet(ConfiguracaoPadrao::criar);
        CalculoValidador.validar(request, peca, impressora, configuracao);
        Set<UUID> ids = new LinkedHashSet<>();
        request.getFilamentos().forEach(item -> ids.add(item.getFilamentoId()));
        Map<UUID, Filamento> filamentos = new HashMap<>();
        filamentoService.fetchByIds(ids).forEach(f -> filamentos.put(f.getId(), f));
        Embalagem embalagem = Boolean.TRUE.equals(configuracao.getEmbalagemFixa()) ? null
                : embalagemService.fetchById(request.getEmbalagemId()).orElseThrow(() -> new IllegalArgumentException("Embalagem não encontrada"));
        BigDecimal manutencao = manutencaoImpressoraService.custoPorHora(impressora.getId());
        return new MotorCalculo(calculoPlataformaService).calcular(request, peca, impressora, configuracao,
                filamentos, embalagem, manutencao, LocalDateTime.now(clock));
    }

    public CalculoHistorico calcularComPrecoFixo(
            CalculoRequest request,
            BigDecimal precoVenda
    ) {
        if (precoVenda == null || precoVenda.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Informe um preço de venda maior que zero"
            );
        }

        PlataformaVenda plataformaOriginal = request.getPlataformaVenda();
        BigDecimal markupOriginal = request.getMarkupPercentual();

        CalculoHistorico historico;

        try {
            // Primeiro calcula somente os custos, sem taxas de plataforma.
            request.setPlataformaVenda(PlataformaVenda.NENHUMA);
            request.setMarkupPercentual(ZERO);

            historico = calcular(request);

        } finally {
            // Restaura os valores originais da comparação.
            request.setPlataformaVenda(plataformaOriginal);
            request.setMarkupPercentual(markupOriginal);
        }

        Configuracao configuracao =
                configuracaoService.buscarConfiguracao().orElseGet(ConfiguracaoPadrao::criar);

        calculoPlataformaService.calcularTaxasPrecoFixo(
                request,
                historico,
                precoVenda,
                configuracao,
                plataformaOriginal
        );

        return historico;
    }
}
