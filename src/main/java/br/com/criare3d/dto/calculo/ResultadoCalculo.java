package br.com.criare3d.dto.calculo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Resultado imutável da simulação, sem relacionamentos de persistência. */
public record ResultadoCalculo(
        UUID id,
        String nomePeca,
        String categoriaPeca,
        Boolean temFoto,
        String nomeFilamento,
        BigDecimal precoRoloFilamento,
        BigDecimal pesoRoloGramas,
        BigDecimal quantidadeFilamentoGramas,
        BigDecimal gramasTotaisLote,
        String nomeImpressora,
        BigDecimal potenciaWatts,
        Long tempoImpressaoMinutos,
        Long minutosMaquinaLote,
        Integer pecasPorImpressao,
        Integer tempoAquecimentoMinutos,
        BigDecimal purgaGramasPorImpressao,
        BigDecimal tarifaEnergiaKwh,
        BigDecimal custoFilamento,
        BigDecimal custoEnergia,
        BigDecimal custoMaoObra,
        BigDecimal custoDepreciacao,
        BigDecimal custoManutencao,
        BigDecimal custoFalha,
        BigDecimal custoPurga,
        Boolean maoObraAtivada,
        BigDecimal valorMaoObraHora,
        Boolean depreciacaoAtivada,
        Long tempoTrabalhoMinutos,
        BigDecimal valorCompraImpressora,
        BigDecimal vidaUtilImpressoraHoras,
        BigDecimal depreciacaoHoraImpressora,
        String depreciacaoOrigemImpressora,
        Integer anunciosNovosDepreciacao,
        Integer anunciosUsadosDepreciacao,
        BigDecimal manutencaoHoraImpressora,
        Boolean taxaFalhaAtivada,
        BigDecimal percentualFalha,
        String nomeEmbalagem,
        BigDecimal custoEmbalagem,
        BigDecimal custoLoja,
        BigDecimal custoTotal,
        BigDecimal markupPercentual,
        BigDecimal precoSugerido,
        LocalDateTime dataCalculo,
        Integer unidades,
        String plataformaVenda,
        String tipoAnuncio,
        BigDecimal percentualComissaoPlataforma,
        BigDecimal custoAdicionalPlataforma,
        BigDecimal percentualImposto,
        BigDecimal freteVendedor,
        Boolean taxaCpfShopee,
        Boolean freteGratisTikTok,
        BigDecimal percentualAfiliado,
        BigDecimal precoVendaPlataforma,
        BigDecimal taxasPlataforma,
        BigDecimal lucroLiquido,
        String tipoPessoaShopee,
        Boolean mais450PedidosShopee,
        Boolean isencaoComissaoTikTok,
        String faixaPesoPacote,
        Boolean formularioAvancado,
        Integer impressoes,
        UUID pecaId,
        UUID impressoraId,
        UUID embalagemId,
        UUID filamentoId,
        List<Item> itensFilamento,
        List<Rateio> rateiosCustos) {
        public UUID getId() { return id; }
        public String getNomePeca() { return nomePeca; }
        public String getCategoriaPeca() { return categoriaPeca; }
        public Boolean getTemFoto() { return temFoto; }
        public String getNomeFilamento() { return nomeFilamento; }
        public BigDecimal getPrecoRoloFilamento() { return precoRoloFilamento; }
        public BigDecimal getPesoRoloGramas() { return pesoRoloGramas; }
        public BigDecimal getQuantidadeFilamentoGramas() { return quantidadeFilamentoGramas; }
        public BigDecimal getGramasTotaisLote() { return gramasTotaisLote; }
        public String getNomeImpressora() { return nomeImpressora; }
        public BigDecimal getPotenciaWatts() { return potenciaWatts; }
        public Long getTempoImpressaoMinutos() { return tempoImpressaoMinutos; }
        public Long getMinutosMaquinaLote() { return minutosMaquinaLote; }
        public Integer getPecasPorImpressao() { return pecasPorImpressao; }
        public Integer getTempoAquecimentoMinutos() { return tempoAquecimentoMinutos; }
        public BigDecimal getPurgaGramasPorImpressao() { return purgaGramasPorImpressao; }
        public BigDecimal getTarifaEnergiaKwh() { return tarifaEnergiaKwh; }
        public BigDecimal getCustoFilamento() { return custoFilamento; }
        public BigDecimal getCustoEnergia() { return custoEnergia; }
        public BigDecimal getCustoMaoObra() { return custoMaoObra; }
        public BigDecimal getCustoDepreciacao() { return custoDepreciacao; }
        public BigDecimal getCustoManutencao() { return custoManutencao; }
        public BigDecimal getCustoFalha() { return custoFalha; }
        public BigDecimal getCustoPurga() { return custoPurga; }
        public Boolean getMaoObraAtivada() { return maoObraAtivada; }
        public BigDecimal getValorMaoObraHora() { return valorMaoObraHora; }
        public Boolean getDepreciacaoAtivada() { return depreciacaoAtivada; }
        public Long getTempoTrabalhoMinutos() { return tempoTrabalhoMinutos; }
        public BigDecimal getValorCompraImpressora() { return valorCompraImpressora; }
        public BigDecimal getVidaUtilImpressoraHoras() { return vidaUtilImpressoraHoras; }
        public BigDecimal getDepreciacaoHoraImpressora() { return depreciacaoHoraImpressora; }
        public String getDepreciacaoOrigemImpressora() { return depreciacaoOrigemImpressora; }
        public Integer getAnunciosNovosDepreciacao() { return anunciosNovosDepreciacao; }
        public Integer getAnunciosUsadosDepreciacao() { return anunciosUsadosDepreciacao; }
        public BigDecimal getManutencaoHoraImpressora() { return manutencaoHoraImpressora; }
        public Boolean getTaxaFalhaAtivada() { return taxaFalhaAtivada; }
        public BigDecimal getPercentualFalha() { return percentualFalha; }
        public String getNomeEmbalagem() { return nomeEmbalagem; }
        public BigDecimal getCustoEmbalagem() { return custoEmbalagem; }
        public BigDecimal getCustoLoja() { return custoLoja; }
        public BigDecimal getCustoTotal() { return custoTotal; }
        public BigDecimal getMarkupPercentual() { return markupPercentual; }
        public BigDecimal getPrecoSugerido() { return precoSugerido; }
        public LocalDateTime getDataCalculo() { return dataCalculo; }
        public Integer getUnidades() { return unidades; }
        public String getPlataformaVenda() { return plataformaVenda; }
        public String getTipoAnuncio() { return tipoAnuncio; }
        public BigDecimal getPercentualComissaoPlataforma() { return percentualComissaoPlataforma; }
        public BigDecimal getCustoAdicionalPlataforma() { return custoAdicionalPlataforma; }
        public BigDecimal getPercentualImposto() { return percentualImposto; }
        public BigDecimal getFreteVendedor() { return freteVendedor; }
        public Boolean getTaxaCpfShopee() { return taxaCpfShopee; }
        public Boolean getFreteGratisTikTok() { return freteGratisTikTok; }
        public BigDecimal getPercentualAfiliado() { return percentualAfiliado; }
        public BigDecimal getPrecoVendaPlataforma() { return precoVendaPlataforma; }
        public BigDecimal getTaxasPlataforma() { return taxasPlataforma; }
        public BigDecimal getLucroLiquido() { return lucroLiquido; }
        public String getTipoPessoaShopee() { return tipoPessoaShopee; }
        public Boolean getMais450PedidosShopee() { return mais450PedidosShopee; }
        public Boolean getIsencaoComissaoTikTok() { return isencaoComissaoTikTok; }
        public String getFaixaPesoPacote() { return faixaPesoPacote; }
        public Boolean getFormularioAvancado() { return formularioAvancado; }
        public Integer getImpressoes() { return impressoes; }
        public UUID getPecaId() { return pecaId; }
        public UUID getImpressoraId() { return impressoraId; }
        public UUID getEmbalagemId() { return embalagemId; }
        public UUID getFilamentoId() { return filamentoId; }
        public List<Item> getItensFilamento() { return itensFilamento; }
        public List<Rateio> getRateiosCustos() { return rateiosCustos; }
    public ResultadoCalculo { itensFilamento = List.copyOf(itensFilamento); rateiosCustos = List.copyOf(rateiosCustos); }
    public Integer getFornadas() { return impressoes; }
    public static ResultadoCalculo de(EstadoCalculo estado) {
        return new ResultadoCalculo(
            estado.getId(),
            estado.getNomePeca(),
            estado.getCategoriaPeca(),
            estado.getTemFoto(),
            estado.getNomeFilamento(),
            estado.getPrecoRoloFilamento(),
            estado.getPesoRoloGramas(),
            estado.getQuantidadeFilamentoGramas(),
            estado.getGramasTotaisLote(),
            estado.getNomeImpressora(),
            estado.getPotenciaWatts(),
            estado.getTempoImpressaoMinutos(),
            estado.getMinutosMaquinaLote(),
            estado.getPecasPorImpressao(),
            estado.getTempoAquecimentoMinutos(),
            estado.getPurgaGramasPorImpressao(),
            estado.getTarifaEnergiaKwh(),
            estado.getCustoFilamento(),
            estado.getCustoEnergia(),
            estado.getCustoMaoObra(),
            estado.getCustoDepreciacao(),
            estado.getCustoManutencao(),
            estado.getCustoFalha(),
            estado.getCustoPurga(),
            estado.getMaoObraAtivada(),
            estado.getValorMaoObraHora(),
            estado.getDepreciacaoAtivada(),
            estado.getTempoTrabalhoMinutos(),
            estado.getValorCompraImpressora(),
            estado.getVidaUtilImpressoraHoras(),
            estado.getDepreciacaoHoraImpressora(),
            estado.getDepreciacaoOrigemImpressora(),
            estado.getAnunciosNovosDepreciacao(),
            estado.getAnunciosUsadosDepreciacao(),
            estado.getManutencaoHoraImpressora(),
            estado.getTaxaFalhaAtivada(),
            estado.getPercentualFalha(),
            estado.getNomeEmbalagem(),
            estado.getCustoEmbalagem(),
            estado.getCustoLoja(),
            estado.getCustoTotal(),
            estado.getMarkupPercentual(),
            estado.getPrecoSugerido(),
            estado.getDataCalculo(),
            estado.getUnidades(),
            estado.getPlataformaVenda(),
            estado.getTipoAnuncio(),
            estado.getPercentualComissaoPlataforma(),
            estado.getCustoAdicionalPlataforma(),
            estado.getPercentualImposto(),
            estado.getFreteVendedor(),
            estado.getTaxaCpfShopee(),
            estado.getFreteGratisTikTok(),
            estado.getPercentualAfiliado(),
            estado.getPrecoVendaPlataforma(),
            estado.getTaxasPlataforma(),
            estado.getLucroLiquido(),
            estado.getTipoPessoaShopee(),
            estado.getMais450PedidosShopee(),
            estado.getIsencaoComissaoTikTok(),
            estado.getFaixaPesoPacote(),
            estado.getFormularioAvancado(),
            estado.getImpressoes(),
            estado.getPecaId(),
            estado.getImpressoraId(),
            estado.getEmbalagemId(),
            estado.getFilamentoId(),
            estado.getItensFilamento().stream().map(i -> new Item(i.getId(), i.getNomeFilamento(), i.getCorFilamento(), i.getCodigoCor(), i.getPrecoRoloFilamento(), i.getPesoRoloGramas(), i.getQuantidadeFilamentoGramas(), i.getPurgaGramas(), i.getCustoFilamento(), i.getFilamentoId())).toList(),
            estado.getRateiosCustos().stream().map(i -> new Rateio(i.getCustoLojaId(), i.getDescricao(), i.getValorUnitario())).toList());
    }
    public record Item(
        UUID id,
        String nomeFilamento,
        String corFilamento,
        String codigoCor,
        BigDecimal precoRoloFilamento,
        BigDecimal pesoRoloGramas,
        BigDecimal quantidadeFilamentoGramas,
        BigDecimal purgaGramas,
        BigDecimal custoFilamento,
        UUID filamentoId) {
        public UUID getId() { return id; }
        public String getNomeFilamento() { return nomeFilamento; }
        public String getCorFilamento() { return corFilamento; }
        public String getCodigoCor() { return codigoCor; }
        public BigDecimal getPrecoRoloFilamento() { return precoRoloFilamento; }
        public BigDecimal getPesoRoloGramas() { return pesoRoloGramas; }
        public BigDecimal getQuantidadeFilamentoGramas() { return quantidadeFilamentoGramas; }
        public BigDecimal getPurgaGramas() { return purgaGramas; }
        public BigDecimal getCustoFilamento() { return custoFilamento; }
        public UUID getFilamentoId() { return filamentoId; }
    }
    public record Rateio(
        UUID custoLojaId,
        String descricao,
        BigDecimal valorUnitario) {
        public UUID getCustoLojaId() { return custoLojaId; }
        public String getDescricao() { return descricao; }
        public BigDecimal getValorUnitario() { return valorUnitario; }
    }
}
