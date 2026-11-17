package br.com.criare3d.service;


import br.com.criare3d.dto.calculo.*;
import br.com.criare3d.dto.calculo.EntradaCalculo.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static br.com.criare3d.service.CalculoFinanceiro.precisao;

/**
 * Monta snapshots sem persistir ou consultar dados.
 */
public final class MontadorResultado {
    private MontadorResultado() {
    }

    public record DadosLote(BigDecimal quantidadeFilamentoTotal, BigDecimal purgaGramasLote,
                            long minutosMaquinaLote, BigDecimal manutencaoPorHora, Long tempoTrabalhoMinutos,
                            String nomeEmbalagem, BigDecimal custoEmbalagem, LocalDateTime dataCalculo) {
    }

    public static ItemMotor item(EstadoCalculo historico, FilamentoDados filamento,
                                            BigDecimal quantidade, BigDecimal custoIndividual) {
        return ItemMotor.builder()
                        .filamentoId(filamento.getId())
            .nomeFilamento(filamento.getNome())
            .corFilamento(filamento.getCor())
            .codigoCor(filamento.getCodigoCor())
            .precoRoloFilamento(filamento.getPrecoRolo())
            .pesoRoloGramas(filamento.getPesoRoloGramas())
            .quantidadeFilamentoGramas(quantidade)
            .custoFilamento(custoIndividual)
            .build();

    }

    public static void preencher(EstadoCalculo historico, Parametros request, PecaDados peca,
                                 ImpressoraDados impressora, EmbalagemDados embalagem, ConfiguracaoDados configuracao,
                                 CalculadoraProducao.Resultado custos, DadosLote dados) {
        int pecasPorImpressao = request.getPecasPorImpressao() == null ? 1 : request.getPecasPorImpressao();
        int tempoAquecimentoMinutos = impressora.getTempoAquecimentoMinutos() == null ? 0 : impressora.getTempoAquecimentoMinutos();
        BigDecimal purgaGramasPorImpressao = configuracao.getPurgaGramasPorImpressao() == null ? BigDecimal.ZERO : configuracao.getPurgaGramasPorImpressao();
        historico.setPecaId(peca.getId());
        historico.setCategoriaPeca(peca.getCategoria());
        historico.setImpressoraId(impressora.getId());
        if (embalagem != null && embalagem.isArquivado())
            throw new IllegalArgumentException("Restaure a embalagem antes de calcular.");
        historico.setEmbalagemId(embalagem == null ? null : embalagem.getId());

        historico.setNomePeca(peca.getNome());
        historico.setUnidades(request.getUnidades());
        historico.setGramasTotaisLote(precisao(dados.quantidadeFilamentoTotal().multiply(BigDecimal.valueOf(request.getUnidades()))
            .add(dados.purgaGramasLote())));
        historico.setImpressoes(ModeloImpressao.quantidadeImpressoes(request.getUnidades(), pecasPorImpressao));
        historico.setPecasPorImpressao(pecasPorImpressao);
        historico.setTempoAquecimentoMinutos(tempoAquecimentoMinutos);
        historico.setPurgaGramasPorImpressao(purgaGramasPorImpressao);
        historico.setNomeImpressora(impressora.getNome());
        historico.setPotenciaWatts(impressora.getPotenciaWatts());
        historico.setTempoImpressaoMinutos(request.getTempoImpressaoMinutos());
        historico.setMinutosMaquinaLote(dados.minutosMaquinaLote());
        historico.setTarifaEnergiaKwh(configuracao.getTarifaEnergiaKwh());

        historico.setNomeEmbalagem(dados.nomeEmbalagem());
        historico.setCustoEmbalagem(dados.custoEmbalagem());
        historico.setCustoEnergia(custos.custoEnergia());
        historico.setCustoMaoObra(custos.custoMaoObra());
        historico.setCustoDepreciacao(custos.custoDepreciacao());
        historico.setCustoManutencao(custos.custoManutencao());
        historico.setCustoFalha(custos.custoFalha());
        historico.setCustoPurga(custos.custoPurga());
        historico.setMaoObraAtivada(Boolean.TRUE.equals(configuracao.getMaoObraAtivada()));
        historico.setValorMaoObraHora(configuracao.getValorMaoObraHora());
        historico.setDepreciacaoAtivada(Boolean.TRUE.equals(configuracao.getDepreciacaoAtivada()));
        historico.setTempoTrabalhoMinutos(dados.tempoTrabalhoMinutos());
        historico.setValorCompraImpressora(impressora.getValorCompra());
        historico.setVidaUtilImpressoraHoras(impressora.getVidaUtilHoras());
        historico.setDepreciacaoHoraImpressora(impressora.getDepreciacaoPorHora());
        historico.setDepreciacaoOrigemImpressora(impressora.getDepreciacaoOrigem());
        historico.setAnunciosNovosDepreciacao(impressora.getAnunciosNovos());
        historico.setAnunciosUsadosDepreciacao(impressora.getAnunciosUsados());
        historico.setManutencaoHoraImpressora(dados.manutencaoPorHora());
        historico.setTaxaFalhaAtivada(Boolean.TRUE.equals(configuracao.getTaxaFalhaAtivada()));
        historico.setPercentualFalha(configuracao.getPercentualFalha());
        historico.setCustoTotal(custos.custoTotal());

        historico.setMarkupPercentual(request.getMarkupPercentual());
        historico.setPrecoSugerido(custos.precoSugerido());

        historico.setDataCalculo(
            dados.dataCalculo()
        );

    }
}
