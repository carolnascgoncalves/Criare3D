package br.com.criare3d.service;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.models.CalculoHistorico;
import java.math.BigDecimal;

/** Copia parâmetros editáveis; preços, custos e estados operacionais são recalculados. */
public final class CalculoRascunhoMapper {
    private CalculoRascunhoMapper() {}

    public static CalculoRequest copiar(CalculoHistorico origem) {
        var destino = new CalculoRequest();
        destino.setUnidades(origem.getUnidades());
        destino.setPecasPorImpressao(origem.getPecasPorImpressao());
        destino.setTempoImpressaoMinutos(origem.getTempoImpressaoMinutos());
        destino.setTempoTrabalhoMinutos(origem.getTempoTrabalhoMinutos());
        destino.setQuantidadeFilamentoGramas(origem.getQuantidadeFilamentoGramas());
        destino.setMarkupPercentual(origem.getMarkupPercentual());
        if (origem.getTipoAnuncio() != null) destino.setTipoAnuncio(origem.getTipoAnuncio());
        destino.setPercentualComissaoPlataforma(zeroSeNulo(origem.getPercentualComissaoPlataforma()));
        destino.setCustoAdicionalPlataforma(zeroSeNulo(origem.getCustoAdicionalPlataforma()));
        destino.setPercentualImposto(zeroSeNulo(origem.getPercentualImposto()));
        destino.setFreteVendedor(zeroSeNulo(origem.getFreteVendedor()));
        destino.setPercentualAfiliado(zeroSeNulo(origem.getPercentualAfiliado()));
        destino.setTaxaCpfShopee(Boolean.TRUE.equals(origem.getTaxaCpfShopee()));
        if (origem.getTipoPessoaShopee() != null) destino.setTipoPessoaShopee(origem.getTipoPessoaShopee());
        destino.setMais450PedidosShopee(Boolean.TRUE.equals(origem.getMais450PedidosShopee()));
        destino.setFreteGratisTikTok(Boolean.TRUE.equals(origem.getFreteGratisTikTok()));
        destino.setIsencaoComissaoTikTok(Boolean.TRUE.equals(origem.getIsencaoComissaoTikTok()));
        destino.setFaixaPesoPacote(origem.getFaixaPesoPacote());
        return destino;
    }

    private static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
