package br.com.criare3d.dto.calculo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import br.com.criare3d.models.enums.PlataformaVenda;

/** Somente valores: nenhuma entidade ou proxy participa da simulação. */
public record EntradaCalculo(Parametros request, PecaDados peca, ImpressoraDados impressora,
    ConfiguracaoDados configuracao, Map<UUID, FilamentoDados> filamentos, EmbalagemDados embalagem,
    BigDecimal manutencaoPorHora, LocalDateTime dataCalculo, List<CustoDados> custosLoja,
    boolean tempoPorPlaca, int placasConsulta, String nomeEmbalagem) {
    public EntradaCalculo {
        filamentos = Map.copyOf(filamentos);
        custosLoja = List.copyOf(custosLoja);
    }

    public EntradaCalculo comNomeEmbalagem(String nome) {
        return new EntradaCalculo(request, peca, impressora, configuracao, filamentos, embalagem,
            manutencaoPorHora, dataCalculo, custosLoja, tempoPorPlaca, placasConsulta, nome);
    }
    public record Parametros(
        UUID pecaId,
        List<ItemEntrada> filamentos,
        Integer unidades,
        Integer pecasPorImpressao,
        UUID impressoraId,
        Long tempoImpressaoMinutos,
        Long tempoTrabalhoMinutos,
        UUID embalagemId,
        BigDecimal markupPercentual,
        PlataformaVenda plataformaVenda,
        String tipoAnuncio,
        BigDecimal percentualComissaoPlataforma,
        BigDecimal custoAdicionalPlataforma,
        BigDecimal percentualImposto,
        BigDecimal freteVendedor,
        BigDecimal percentualAfiliado,
        String tipoPessoaShopee,
        boolean mais450PedidosShopee,
        boolean freteGratisTikTok,
        boolean isencaoComissaoTikTok,
        String faixaPesoPacote,
        BigDecimal freteMercadoLivreManual,
        boolean freteMercadoLivreManualNecessario) {
        public UUID getPecaId() { return pecaId; }
        public List<ItemEntrada> getFilamentos() { return filamentos; }
        public Integer getUnidades() { return unidades; }
        public Integer getPecasPorImpressao() { return pecasPorImpressao; }
        public UUID getImpressoraId() { return impressoraId; }
        public Long getTempoImpressaoMinutos() { return tempoImpressaoMinutos; }
        public Long getTempoTrabalhoMinutos() { return tempoTrabalhoMinutos; }
        public UUID getEmbalagemId() { return embalagemId; }
        public BigDecimal getMarkupPercentual() { return markupPercentual; }
        public PlataformaVenda getPlataformaVenda() { return plataformaVenda; }
        public String getTipoAnuncio() { return tipoAnuncio; }
        public BigDecimal getPercentualComissaoPlataforma() { return percentualComissaoPlataforma; }
        public BigDecimal getCustoAdicionalPlataforma() { return custoAdicionalPlataforma; }
        public BigDecimal getPercentualImposto() { return percentualImposto; }
        public BigDecimal getFreteVendedor() { return freteVendedor; }
        public BigDecimal getPercentualAfiliado() { return percentualAfiliado; }
        public String getTipoPessoaShopee() { return tipoPessoaShopee; }
        public boolean isMais450PedidosShopee() { return mais450PedidosShopee; }
        public boolean isFreteGratisTikTok() { return freteGratisTikTok; }
        public boolean isIsencaoComissaoTikTok() { return isencaoComissaoTikTok; }
        public String getFaixaPesoPacote() { return faixaPesoPacote; }
        public BigDecimal getFreteMercadoLivreManual() { return freteMercadoLivreManual; }
        public boolean isFreteMercadoLivreManualNecessario() { return freteMercadoLivreManualNecessario; }
        public Parametros { if (filamentos != null) filamentos = Collections.unmodifiableList(new ArrayList<>(filamentos)); }
    }
    public record PecaDados(
        boolean arquivado,
        UUID id,
        String nome,
        String categoria) {
        public boolean isArquivado() { return arquivado; }
        public UUID getId() { return id; }
        public String getNome() { return nome; }
        public String getCategoria() { return categoria; }
    }
    public record ImpressoraDados(
        boolean arquivado,
        UUID id,
        String nome,
        BigDecimal potenciaWatts,
        BigDecimal valorCompra,
        BigDecimal vidaUtilHoras,
        BigDecimal depreciacaoPorHora,
        String depreciacaoOrigem,
        Integer anunciosNovos,
        Integer anunciosUsados,
        Integer tempoAquecimentoMinutos) {
        public boolean isArquivado() { return arquivado; }
        public UUID getId() { return id; }
        public String getNome() { return nome; }
        public BigDecimal getPotenciaWatts() { return potenciaWatts; }
        public BigDecimal getValorCompra() { return valorCompra; }
        public BigDecimal getVidaUtilHoras() { return vidaUtilHoras; }
        public BigDecimal getDepreciacaoPorHora() { return depreciacaoPorHora; }
        public String getDepreciacaoOrigem() { return depreciacaoOrigem; }
        public Integer getAnunciosNovos() { return anunciosNovos; }
        public Integer getAnunciosUsados() { return anunciosUsados; }
        public Integer getTempoAquecimentoMinutos() { return tempoAquecimentoMinutos; }
    }
    public record ConfiguracaoDados(
        BigDecimal tarifaEnergiaKwh,
        Boolean embalagemFixa,
        BigDecimal custoEmbalagemFixa,
        Boolean formularioAvancado,
        Boolean maoObraAtivada,
        BigDecimal valorMaoObraHora,
        Boolean maoObraTempoFixo,
        Long tempoMaoObraFixoMinutos,
        Boolean depreciacaoAtivada,
        Boolean taxaFalhaAtivada,
        BigDecimal percentualFalha,
        BigDecimal purgaGramasPorImpressao) {
        public BigDecimal getTarifaEnergiaKwh() { return tarifaEnergiaKwh; }
        public Boolean getEmbalagemFixa() { return embalagemFixa; }
        public BigDecimal getCustoEmbalagemFixa() { return custoEmbalagemFixa; }
        public Boolean getFormularioAvancado() { return formularioAvancado; }
        public Boolean getMaoObraAtivada() { return maoObraAtivada; }
        public BigDecimal getValorMaoObraHora() { return valorMaoObraHora; }
        public Boolean getMaoObraTempoFixo() { return maoObraTempoFixo; }
        public Long getTempoMaoObraFixoMinutos() { return tempoMaoObraFixoMinutos; }
        public Boolean getDepreciacaoAtivada() { return depreciacaoAtivada; }
        public Boolean getTaxaFalhaAtivada() { return taxaFalhaAtivada; }
        public BigDecimal getPercentualFalha() { return percentualFalha; }
        public BigDecimal getPurgaGramasPorImpressao() { return purgaGramasPorImpressao; }
    }
    public record FilamentoDados(
        boolean arquivado,
        UUID id,
        String nome,
        String cor,
        String codigoCor,
        BigDecimal pesoRoloGramas,
        BigDecimal precoRolo) {
        public boolean isArquivado() { return arquivado; }
        public UUID getId() { return id; }
        public String getNome() { return nome; }
        public String getCor() { return cor; }
        public String getCodigoCor() { return codigoCor; }
        public BigDecimal getPesoRoloGramas() { return pesoRoloGramas; }
        public BigDecimal getPrecoRolo() { return precoRolo; }
    }
    public record EmbalagemDados(
        boolean arquivado,
        UUID id,
        String nome,
        BigDecimal custoUnitario) {
        public boolean isArquivado() { return arquivado; }
        public UUID getId() { return id; }
        public String getNome() { return nome; }
        public BigDecimal getCustoUnitario() { return custoUnitario; }
    }
    public record ItemEntrada(
        UUID filamentoId,
        BigDecimal quantidadeFilamentoGramas) {
        public UUID getFilamentoId() { return filamentoId; }
        public BigDecimal getQuantidadeFilamentoGramas() { return quantidadeFilamentoGramas; }
    }
    public record CustoDados(
        UUID id,
        String descricao,
        String tipo,
        BigDecimal valor,
        BigDecimal meta,
        Integer unidadesPrevistas) {
        public UUID getId() { return id; }
        public String getDescricao() { return descricao; }
        public String getTipo() { return tipo; }
        public BigDecimal getValor() { return valor; }
        public BigDecimal getMeta() { return meta; }
        public Integer getUnidadesPrevistas() { return unidadesPrevistas; }
    }
}
