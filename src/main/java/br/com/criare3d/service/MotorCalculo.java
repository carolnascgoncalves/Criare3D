package br.com.criare3d.service;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static br.com.criare3d.service.CalculoFinanceiro.precisao;
import static br.com.criare3d.service.CalculoFinanceiro.ratear;

/** Motor sem banco, sessão ou gravação: todos os dados são fornecidos pelo chamador. */
public final class MotorCalculo {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal CEM = new BigDecimal("100");
    private final CalculoPlataformaService calculoPlataformaService;
    public MotorCalculo(CalculoPlataformaService plataformas) { this.calculoPlataformaService = plataformas; }
    public CalculoHistorico calcular(CalculoRequest request, Peca peca, Impressora impressora,
            Configuracao configuracao, Map<UUID, Filamento> filamentosCadastrados,
            Embalagem embalagemSelecionada, BigDecimal manutencaoPorHora, LocalDateTime dataCalculo) {

        CalculoValidador.validar(request, peca, impressora, configuracao);
        int unidades = request.getUnidades();
        int pecasPorImpressao = request.getPecasPorImpressao() == null ? 1 : request.getPecasPorImpressao();
        int fornadas = ModeloImpressao.quantidadeImpressoes(unidades, pecasPorImpressao);
        int tempoAquecimentoMinutos = impressora.getTempoAquecimentoMinutos() == null
                ? 0 : impressora.getTempoAquecimentoMinutos();
        long minutosMaquinaLote;
        try {
            minutosMaquinaLote = ModeloImpressao.minutosTotais(
                    request.getTempoImpressaoMinutos(), tempoAquecimentoMinutos,
                    unidades, pecasPorImpressao);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("O tempo total de impressão ultrapassa o limite permitido.");
        }

        BigDecimal markup = request.getMarkupPercentual();

        CalculoHistorico historico = CalculoHistorico.builder()
                .itensFilamento(new java.util.ArrayList<>())
                .build();

        BigDecimal custoFilamentoTotal = ZERO;
        BigDecimal quantidadeFilamentoTotal = ZERO;

        for (ItemCalculoRequest itemRequest : request.getFilamentos()) {

            if (itemRequest.getFilamentoId() == null) {
                throw new IllegalArgumentException("Selecione todos os filamentos");
            }

            BigDecimal quantidade = itemRequest.getQuantidadeFilamentoGramas();

            if (quantidade == null || quantidade.compareTo(ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "A quantidade de cada filamento deve ser maior que zero");
            }

            Filamento filamento = filamentosCadastrados.get(itemRequest.getFilamentoId());
            if (filamento == null || filamento.isArquivado()) {
                throw new IllegalArgumentException("Filamento não encontrado");
            }

            if (filamento.getPesoRoloGramas() == null
                    || filamento.getPesoRoloGramas().compareTo(ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "O peso do rolo deve ser maior que zero: " + filamento.getNome());
            }

            if (filamento.getPrecoRolo() == null
                    || filamento.getPrecoRolo().compareTo(ZERO) < 0) {
                throw new IllegalArgumentException(
                        "O preço do rolo é inválido: " + filamento.getNome());
            }

            BigDecimal custoIndividual = CalculoFinanceiro.filamento(
                    quantidade, filamento.getPrecoRolo(), filamento.getPesoRoloGramas());

            ItemCalculoHistorico item = MontadorHistorico.item(historico, filamento, quantidade, custoIndividual);

            historico.getItensFilamento().add(item);
            custoFilamentoTotal = custoFilamentoTotal.add(custoIndividual);
            quantidadeFilamentoTotal = quantidadeFilamentoTotal.add(quantidade);
        }

        custoFilamentoTotal = precisao(custoFilamentoTotal);

        ItemCalculoHistorico primeiroItem = historico.getItensFilamento().get(0);

        historico.setFilamento(primeiroItem.getFilamento());
        historico.setNomeFilamento(primeiroItem.getNomeFilamento());
        historico.setPrecoRoloFilamento(primeiroItem.getPrecoRoloFilamento());
        historico.setPesoRoloGramas(primeiroItem.getPesoRoloGramas());
        historico.setQuantidadeFilamentoGramas(primeiroItem.getQuantidadeFilamentoGramas());
        historico.setCustoFilamento(custoFilamentoTotal);

        BigDecimal purgaGramasPorImpressao = configuracao.getPurgaGramasPorImpressao() == null
                ? ZERO : configuracao.getPurgaGramasPorImpressao();

        Long tempoTrabalhoMinutos = Boolean.TRUE.equals(configuracao.getMaoObraTempoFixo())
                ? configuracao.getTempoMaoObraFixoMinutos()
                : (request.getTempoTrabalhoMinutos() != null
                    ? request.getTempoTrabalhoMinutos()
                    : 0L);
        if (tempoTrabalhoMinutos == null || tempoTrabalhoMinutos < 0) {
            throw new IllegalArgumentException("Informe um tempo de trabalho manual válido.");
        }

        if (Boolean.TRUE.equals(configuracao.getMaoObraAtivada())) {
            BigDecimal valorHora = configuracao.getValorMaoObraHora();
            if (valorHora == null || valorHora.signum() < 0) {
                throw new IllegalArgumentException("Configure um custo por hora válido para a mão de obra.");
            }
        }

        if (manutencaoPorHora == null || manutencaoPorHora.signum() < 0) {
            throw new IllegalArgumentException("A estimativa de manutenção por hora é inválida.");
        }
        if (Boolean.TRUE.equals(configuracao.getDepreciacaoAtivada())) {
            BigDecimal valorImpressora = impressora.getValorCompra();
            BigDecimal vidaUtilHoras = impressora.getVidaUtilHoras();
            BigDecimal depreciacaoPorHora = impressora.getDepreciacaoPorHora();
            boolean taxaDiretaValida = depreciacaoPorHora != null && depreciacaoPorHora.signum() >= 0;
            boolean dadosLegadosValidos = valorImpressora != null && valorImpressora.signum() >= 0
                    && vidaUtilHoras != null && vidaUtilHoras.signum() > 0;
            if (!taxaDiretaValida && !dadosLegadosValidos) {
                throw new IllegalArgumentException(
                        "A depreciação está ativada nas Configurações, mas faltam dados na impressora selecionada. " +
                        "Informe o custo estimado de depreciação por hora em Impressoras; a calculadora usará " +
                        "esse valor e o tempo de impressão para incluir o custo."
                );
            }
        }

        // As entradas já estão validadas; a calculadora concentra as fórmulas e o rateio do lote.
        CalculadoraProducao.CustosOperacionaisLote custosOperacionais =
                CalculadoraProducao.calcularCustosOperacionaisLote(
                        impressora.getPotenciaWatts(), minutosMaquinaLote,
                        configuracao.getTarifaEnergiaKwh(), purgaGramasPorImpressao, fornadas,
                        custoFilamentoTotal, quantidadeFilamentoTotal,
                        Boolean.TRUE.equals(configuracao.getMaoObraAtivada()),
                        configuracao.getValorMaoObraHora(), tempoTrabalhoMinutos,
                        manutencaoPorHora, Boolean.TRUE.equals(configuracao.getDepreciacaoAtivada()),
                        impressora.getValorCompra(), impressora.getVidaUtilHoras(), impressora.getDepreciacaoPorHora());
        BigDecimal energiaLote = custosOperacionais.energia();
        BigDecimal purgaGramasLote = custosOperacionais.gramasPurga();
        BigDecimal purgaGramasUnidade = ratear(purgaGramasLote, unidades);
        BigDecimal custoPurgaLote = custosOperacionais.custoPurga();
        BigDecimal custoMaoObraLote = custosOperacionais.maoDeObra();
        BigDecimal custoManutencaoLote = custosOperacionais.manutencao();
        BigDecimal custoDepreciacaoLote = custosOperacionais.depreciacao();
        List<ItemCalculoHistorico> itensHistorico = historico.getItensFilamento();
        DistribuidorPurga.distribuir(purgaGramasUnidade, quantidadeFilamentoTotal, itensHistorico);

        boolean taxaFalhaAtivada = Boolean.TRUE.equals(configuracao.getTaxaFalhaAtivada());
        BigDecimal percentualFalha = configuracao.getPercentualFalha();
        if (taxaFalhaAtivada) {
            if (percentualFalha == null || percentualFalha.signum() < 0
                    || percentualFalha.compareTo(CEM) > 0) {
                throw new IllegalArgumentException("Configure um percentual de falhas entre zero e 100.");
            }
        } else if (percentualFalha == null) {
            percentualFalha = ZERO;
        }

        BigDecimal custoEmbalagem;
        String nomeEmbalagem;
        Embalagem embalagem = embalagemSelecionada;

        if (Boolean.TRUE.equals(configuracao.getEmbalagemFixa())) {

            custoEmbalagem = configuracao.getCustoEmbalagemFixa();

            if (custoEmbalagem == null || custoEmbalagem.compareTo(ZERO) < 0) {
                throw new IllegalArgumentException(
                        "O custo fixo da embalagem não pode ser negativo ou nulo");
            }

            nomeEmbalagem = "Embalagem (valor fixo)";

        } else {

            if (request.getEmbalagemId() == null) {
                throw new IllegalArgumentException("Selecione uma embalagem");
            }

            if (embalagem == null) throw new IllegalArgumentException("Embalagem não encontrada");

            custoEmbalagem = embalagem.getCustoUnitario();
            if (custoEmbalagem == null || custoEmbalagem.compareTo(ZERO) < 0) {
                throw new IllegalArgumentException(
                        "O custo unitário da embalagem deve ser igual ou maior que zero."
                );
            }

            nomeEmbalagem = embalagem.getNome();
        }

        custoEmbalagem = precisao(custoEmbalagem);

        CalculadoraProducao.Resultado custos = CalculadoraProducao.calcular(
                new CalculadoraProducao.Entrada(unidades, custoFilamentoTotal, custoPurgaLote,
                        energiaLote, custoEmbalagem, custoMaoObraLote, custoDepreciacaoLote,
                        custoManutencaoLote, taxaFalhaAtivada, percentualFalha, markup));
        BigDecimal custoPurga = custos.custoPurga();
        BigDecimal custoEnergia = custos.custoEnergia();
        BigDecimal custoMaoObra = custos.custoMaoObra();
        BigDecimal custoDepreciacao = custos.custoDepreciacao();
        BigDecimal custoManutencao = custos.custoManutencao();
        BigDecimal custoFalha = custos.custoFalha();
        BigDecimal custoTotal = custos.custoTotal();
        BigDecimal precoSugerido = custos.precoSugerido();

        MontadorHistorico.preencher(historico, request, peca, impressora, embalagem, configuracao, custos,
                new MontadorHistorico.DadosLote(quantidadeFilamentoTotal, purgaGramasLote, minutosMaquinaLote,
                        manutencaoPorHora, tempoTrabalhoMinutos, nomeEmbalagem, custoEmbalagem, dataCalculo));

        calculoPlataformaService.calcularVendaPlataforma(request, historico, precoSugerido, configuracao);

        return historico;
        }
}
