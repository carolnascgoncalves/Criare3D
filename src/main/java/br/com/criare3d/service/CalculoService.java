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
    private br.com.criare3d.repository.CustoLojaRepository custosLoja;
    private final java.time.Clock clock;
    private final CalculoPlataformaService calculoPlataformaService;
    private final PecaService pecaService;
    private final FilamentoService filamentoService;
    private final ImpressoraService impressoraService;
    private final EmbalagemService embalagemService;
    private final ConfiguracaoService configuracaoService;
    private final ManutencaoImpressoraService manutencaoImpressoraService;
    public CalculoService(
            CalculoPlataformaService calculoPlataformaService,
            PecaService pecaService,
            FilamentoService filamentoService,
            ImpressoraService impressoraService,
            EmbalagemService embalagemService,
            ConfiguracaoService configuracaoService,
            ManutencaoImpressoraService manutencaoImpressoraService,
            java.time.Clock clock) {
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
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
        return calcularDados(request, peca);
    }

    public CalculoHistorico calcularAvulso(CalculoRequest request, String nome) {
        if (nome == null || nome.isBlank()) nome = "Consulta rápida";
        if (nome.length() > 150) throw new IllegalArgumentException("Use até 150 caracteres no nome.");
        Peca peca = new Peca();
        peca.setNome(nome); peca.setId(UUID.randomUUID());
        request.setPecaId(peca.getId());
        CalculoValidador.validarRequisicao(request);
        var resultado = calcularDados(request, peca, true);
        resultado.setPeca(null);
        return resultado;
    }

    /** Entradas da consulta são totais do conjunto, rateadas apenas para exibir a média unitária. */
    public CalculoHistorico calcularAvulsoTotais(CalculoRequest request, String nome, int placas) {
        if (placas < 1 || placas > 1000000) throw new IllegalArgumentException("Informe até 1.000.000 de impressões no lote.");
        if (request.getUnidades() == null || request.getUnidades() < 1 || request.getUnidades() > 10000)
            throw new IllegalArgumentException("Informe entre 1 e 10.000 peças no conjunto.");
        if (nome == null || nome.isBlank()) nome="Consulta rápida";
        if (nome.length()>150) throw new IllegalArgumentException("Use até 150 caracteres no nome.");
        var peca=new Peca();peca.setId(UUID.randomUUID());peca.setNome(nome);request.setPecaId(peca.getId());
        CalculoValidador.validarRequisicao(request);
        for (var item : request.getFilamentos()) {
            if (item.getQuantidadeFilamentoGramas()==null || item.getQuantidadeFilamentoGramas().signum()<=0)
                throw new IllegalArgumentException("Informe os gramas totais de cada filamento.");
            item.setQuantidadeFilamentoGramas(item.getQuantidadeFilamentoGramas().divide(
                    BigDecimal.valueOf(request.getUnidades()), 8, java.math.RoundingMode.HALF_UP));
        }
        request.setPecasPorImpressao(request.getUnidades());
        var resultado=calcularDados(request,peca,true,placas);resultado.setPeca(null);return resultado;
    }

    private CalculoHistorico calcularDados(CalculoRequest request, Peca peca) {
        return calcularDados(request, peca, false);
    }

    private CalculoHistorico calcularDados(CalculoRequest request, Peca peca, boolean tempoPorPlaca) {
        return calcularDados(request,peca,tempoPorPlaca,0);
    }
    private CalculoHistorico calcularDados(CalculoRequest request, Peca peca, boolean tempoPorPlaca, int placasConsulta) {
        Impressora impressora = impressoraService.fetchById(request.getImpressoraId()).orElseThrow(() -> new IllegalArgumentException("Impressora não encontrada"));
        Configuracao configuracao = configuracaoService.buscarConfiguracao().orElseGet(ConfiguracaoPadrao::criar);
        if(placasConsulta>0 && request.getEmbalagemId()!=null && request.getCustoEmbalagemPersonalizada()==null) {
            var copia=new Configuracao();org.springframework.beans.BeanUtils.copyProperties(configuracao,copia);
            copia.setEmbalagemFixa(false);configuracao=copia;
        }
        if(request.getCustoEmbalagemPersonalizada()!=null) {
            var valor=request.getCustoEmbalagemPersonalizada();
            if(valor.signum()<0 || valor.compareTo(new BigDecimal("99999999"))>0)
                throw new IllegalArgumentException("Informe um valor válido para a embalagem personalizada.");
            var copia=new Configuracao();org.springframework.beans.BeanUtils.copyProperties(configuracao,copia);
            copia.setEmbalagemFixa(true);copia.setCustoEmbalagemFixa(valor);configuracao=copia;
        }
        CalculoValidador.validar(request, peca, impressora, configuracao);
        Set<UUID> ids = new LinkedHashSet<>();
        request.getFilamentos().forEach(item -> ids.add(item.getFilamentoId()));
        Map<UUID, Filamento> filamentos = new HashMap<>();
        filamentoService.fetchByIds(ids).forEach(f -> filamentos.put(f.getId(), f));
        Embalagem embalagem = Boolean.TRUE.equals(configuracao.getEmbalagemFixa()) ? null
                : embalagemService.fetchById(request.getEmbalagemId()).orElseThrow(() -> new IllegalArgumentException("Embalagem não encontrada"));
        BigDecimal manutencao = manutencaoImpressoraService.custoPorHora(impressora.getId());
        return new MotorCalculo(calculoPlataformaService).calcular(request, peca, impressora, configuracao,
                filamentos, embalagem, manutencao, LocalDateTime.now(clock),
                custosLoja == null ? java.util.List.of() : custosLoja.findByAtivoTrueAndInicioLessThanEqualOrderByInicioAsc(java.time.LocalDate.now(clock)), tempoPorPlaca, placasConsulta);
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
