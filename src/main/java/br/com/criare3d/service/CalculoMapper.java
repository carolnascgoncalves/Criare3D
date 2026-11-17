package br.com.criare3d.service;

import br.com.criare3d.dto.*;
import br.com.criare3d.dto.calculo.*;
import br.com.criare3d.dto.calculo.EntradaCalculo.*;
import br.com.criare3d.models.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Limite entre os snapshots do motor e os objetos de persistência. */
public final class CalculoMapper {
    private CalculoMapper() {}
    public static Parametros dados(CalculoRequest origem) {
        if (origem == null) return null;
        return new Parametros(
            origem.getPecaId(),
            origem.getFilamentos() == null ? null : origem.getFilamentos().stream().map(i -> i == null ? null : new ItemEntrada(i.getFilamentoId(), i.getQuantidadeFilamentoGramas())).toList(),
            origem.getUnidades(),
            origem.getPecasPorImpressao(),
            origem.getImpressoraId(),
            origem.getTempoImpressaoMinutos(),
            origem.getTempoTrabalhoMinutos(),
            origem.getEmbalagemId(),
            origem.getMarkupPercentual(),
            origem.getPlataformaVenda(),
            origem.getTipoAnuncio(),
            origem.getPercentualComissaoPlataforma(),
            origem.getCustoAdicionalPlataforma(),
            origem.getPercentualImposto(),
            origem.getFreteVendedor(),
            origem.getPercentualAfiliado(),
            origem.getTipoPessoaShopee(),
            origem.isMais450PedidosShopee(),
            origem.isFreteGratisTikTok(),
            origem.isIsencaoComissaoTikTok(),
            origem.getFaixaPesoPacote(),
            origem.getFreteMercadoLivreManual(),
            origem.isFreteMercadoLivreManualNecessario());
    }
    public static PecaDados dados(Peca origem) {
        if (origem == null) return null;
        return new PecaDados(
            origem.isArquivado(),
            origem.getId(),
            origem.getNome(),
            origem.getCategoria());
    }
    public static ImpressoraDados dados(Impressora origem) {
        if (origem == null) return null;
        return new ImpressoraDados(
            origem.isArquivado(),
            origem.getId(),
            origem.getNome(),
            origem.getPotenciaWatts(),
            origem.getValorCompra(),
            origem.getVidaUtilHoras(),
            origem.getDepreciacaoPorHora(),
            origem.getDepreciacaoOrigem(),
            origem.getAnunciosNovos(),
            origem.getAnunciosUsados(),
            origem.getTempoAquecimentoMinutos());
    }
    public static ConfiguracaoDados dados(Configuracao origem) {
        if (origem == null) return null;
        return new ConfiguracaoDados(
            origem.getTarifaEnergiaKwh(),
            origem.getEmbalagemFixa(),
            origem.getCustoEmbalagemFixa(),
            origem.getFormularioAvancado(),
            origem.getMaoObraAtivada(),
            origem.getValorMaoObraHora(),
            origem.getMaoObraTempoFixo(),
            origem.getTempoMaoObraFixoMinutos(),
            origem.getDepreciacaoAtivada(),
            origem.getTaxaFalhaAtivada(),
            origem.getPercentualFalha(),
            origem.getPurgaGramasPorImpressao());
    }
    public static FilamentoDados dados(Filamento origem) {
        if (origem == null) return null;
        return new FilamentoDados(
            origem.isArquivado(),
            origem.getId(),
            origem.getNome(),
            origem.getCor(),
            origem.getCodigoCor(),
            origem.getPesoRoloGramas(),
            origem.getPrecoRolo());
    }
    public static EmbalagemDados dados(Embalagem origem) {
        if (origem == null) return null;
        return new EmbalagemDados(
            origem.isArquivado(),
            origem.getId(),
            origem.getNome(),
            origem.getCustoUnitario());
    }
    public static EntradaCalculo entrada(CalculoRequest request, Peca peca, Impressora impressora,
        Configuracao configuracao, Map<UUID, Filamento> filamentos, Embalagem embalagem,
        BigDecimal manutencao, LocalDateTime data, List<CustoLoja> custos, boolean tempoPorPlaca, int placas) {
        var materiais = new HashMap<UUID, FilamentoDados>();
        filamentos.forEach((id, f) -> materiais.put(id, dados(f)));
        var extras = custos.stream().map(c -> new CustoDados(c.getId(), c.getDescricao(), c.getTipo().name(),
            c.getValor(), c.getMeta(), c.getUnidadesPrevistas())).toList();
        return new EntradaCalculo(dados(request), dados(peca), dados(impressora), dados(configuracao), materiais,
            dados(embalagem), manutencao, data, extras, tempoPorPlaca, placas, null);
    }

    public static CalculoHistorico historico(ResultadoCalculo resultado, Peca peca, Impressora impressora,
        Map<UUID, Filamento> filamentos, Embalagem embalagem, List<CustoLoja> extras) {
        var h = CalculoHistorico.builder().build();
        h.setNomePeca(resultado.nomePeca());
        h.setCategoriaPeca(resultado.categoriaPeca());
        h.setTemFoto(resultado.temFoto());
        h.setNomeFilamento(resultado.nomeFilamento());
        h.setPrecoRoloFilamento(resultado.precoRoloFilamento());
        h.setPesoRoloGramas(resultado.pesoRoloGramas());
        h.setQuantidadeFilamentoGramas(resultado.quantidadeFilamentoGramas());
        h.setGramasTotaisLote(resultado.gramasTotaisLote());
        h.setNomeImpressora(resultado.nomeImpressora());
        h.setPotenciaWatts(resultado.potenciaWatts());
        h.setTempoImpressaoMinutos(resultado.tempoImpressaoMinutos());
        h.setMinutosMaquinaLote(resultado.minutosMaquinaLote());
        h.setPecasPorImpressao(resultado.pecasPorImpressao());
        h.setTempoAquecimentoMinutos(resultado.tempoAquecimentoMinutos());
        h.setPurgaGramasPorImpressao(resultado.purgaGramasPorImpressao());
        h.setTarifaEnergiaKwh(resultado.tarifaEnergiaKwh());
        h.setCustoFilamento(resultado.custoFilamento());
        h.setCustoEnergia(resultado.custoEnergia());
        h.setCustoMaoObra(resultado.custoMaoObra());
        h.setCustoDepreciacao(resultado.custoDepreciacao());
        h.setCustoManutencao(resultado.custoManutencao());
        h.setCustoFalha(resultado.custoFalha());
        h.setCustoPurga(resultado.custoPurga());
        h.setMaoObraAtivada(resultado.maoObraAtivada());
        h.setValorMaoObraHora(resultado.valorMaoObraHora());
        h.setDepreciacaoAtivada(resultado.depreciacaoAtivada());
        h.setTempoTrabalhoMinutos(resultado.tempoTrabalhoMinutos());
        h.setValorCompraImpressora(resultado.valorCompraImpressora());
        h.setVidaUtilImpressoraHoras(resultado.vidaUtilImpressoraHoras());
        h.setDepreciacaoHoraImpressora(resultado.depreciacaoHoraImpressora());
        h.setDepreciacaoOrigemImpressora(resultado.depreciacaoOrigemImpressora());
        h.setAnunciosNovosDepreciacao(resultado.anunciosNovosDepreciacao());
        h.setAnunciosUsadosDepreciacao(resultado.anunciosUsadosDepreciacao());
        h.setManutencaoHoraImpressora(resultado.manutencaoHoraImpressora());
        h.setTaxaFalhaAtivada(resultado.taxaFalhaAtivada());
        h.setPercentualFalha(resultado.percentualFalha());
        h.setNomeEmbalagem(resultado.nomeEmbalagem());
        h.setCustoEmbalagem(resultado.custoEmbalagem());
        h.setCustoLoja(resultado.custoLoja());
        h.setCustoTotal(resultado.custoTotal());
        h.setMarkupPercentual(resultado.markupPercentual());
        h.setPrecoSugerido(resultado.precoSugerido());
        h.setDataCalculo(resultado.dataCalculo());
        h.setUnidades(resultado.unidades());
        h.setPlataformaVenda(resultado.plataformaVenda());
        h.setTipoAnuncio(resultado.tipoAnuncio());
        h.setPercentualComissaoPlataforma(resultado.percentualComissaoPlataforma());
        h.setCustoAdicionalPlataforma(resultado.custoAdicionalPlataforma());
        h.setPercentualImposto(resultado.percentualImposto());
        h.setFreteVendedor(resultado.freteVendedor());
        h.setTaxaCpfShopee(resultado.taxaCpfShopee());
        h.setFreteGratisTikTok(resultado.freteGratisTikTok());
        h.setPercentualAfiliado(resultado.percentualAfiliado());
        h.setPrecoVendaPlataforma(resultado.precoVendaPlataforma());
        h.setTaxasPlataforma(resultado.taxasPlataforma());
        h.setLucroLiquido(resultado.lucroLiquido());
        h.setTipoPessoaShopee(resultado.tipoPessoaShopee());
        h.setMais450PedidosShopee(resultado.mais450PedidosShopee());
        h.setIsencaoComissaoTikTok(resultado.isencaoComissaoTikTok());
        h.setFaixaPesoPacote(resultado.faixaPesoPacote());
        h.setFormularioAvancado(resultado.formularioAvancado());
        h.setFornadas(resultado.impressoes());
        h.setPeca(peca);
        h.setImpressora(impressora);
        h.setEmbalagem(embalagem);
        h.setFilamento(filamentos.get(resultado.filamentoId()));
        for (var material : resultado.itensFilamento()) {
            var i = ItemCalculoHistorico.builder().build();
            i.setNomeFilamento(material.nomeFilamento());
            i.setCorFilamento(material.corFilamento());
            i.setCodigoCor(material.codigoCor());
            i.setPrecoRoloFilamento(material.precoRoloFilamento());
            i.setPesoRoloGramas(material.pesoRoloGramas());
            i.setQuantidadeFilamentoGramas(material.quantidadeFilamentoGramas());
            i.setPurgaGramas(material.purgaGramas());
            i.setCustoFilamento(material.custoFilamento());
            i.setFilamento(filamentos.get(material.filamentoId()));
            i.setCalculoHistorico(h);
            h.getItensFilamento().add(i);
        }
        for (int indice = 0; indice < resultado.rateiosCustos().size(); indice++) {
            var rateio = resultado.rateiosCustos().get(indice);
            var item = new RateioCustoHistorico();
            item.setCalculo(h);
            item.setCustoLoja(extras.get(indice));
            item.setDescricao(rateio.descricao());
            item.setValorUnitario(rateio.valorUnitario());
            h.getRateiosCustos().add(item);
        }
        return h;
    }
    public static CalculoResponse resposta(ResultadoCalculo resultado) {
        var dto = new CalculoResponse();
        org.springframework.beans.BeanUtils.copyProperties(resultado, dto, "itensFilamento", "rateiosCustos");
        dto.setItensFilamento(resultado.itensFilamento().stream().map(item -> ItemCalculoResponse.builder()
            .id(item.id()).nomeFilamento(item.nomeFilamento()).corFilamento(item.corFilamento())
            .codigoCor(item.codigoCor()).precoRoloFilamento(item.precoRoloFilamento())
            .pesoRoloGramas(item.pesoRoloGramas()).quantidadeFilamentoGramas(item.quantidadeFilamentoGramas())
            .purgaGramas(item.purgaGramas()).custoFilamento(item.custoFilamento())
            .precoRoloAtual(item.precoRoloFilamento()).pesoRoloAtual(item.pesoRoloGramas()).build()).toList());
        dto.setRateiosCustos(resultado.rateiosCustos().stream()
            .map(r -> new CalculoResponse.RateioResumo(r.descricao(), r.valorUnitario())).toList());
        var lucro = resultado.precoSugerido().subtract(resultado.custoTotal());
        dto.setLucroEstimado(lucro);
        dto.setMargemSobrePreco(resultado.precoSugerido().signum() <= 0 ? null : lucro.multiply(new BigDecimal("100"))
            .divide(resultado.precoSugerido(), 4, java.math.RoundingMode.HALF_UP));
        dto.setTemFoto(false);
        return dto;
    }
}
