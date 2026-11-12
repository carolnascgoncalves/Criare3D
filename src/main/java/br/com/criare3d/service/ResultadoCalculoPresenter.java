
package br.com.criare3d.service;

import br.com.criare3d.service.CalculoService;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.CalculoResponse;
import br.com.criare3d.dto.ComparacaoPlataforma;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.enums.PlataformaVenda;
import br.com.criare3d.service.VerificacaoTarifasPlataforma;
import br.com.criare3d.service.interfaces.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

@org.springframework.stereotype.Service
public class ResultadoCalculoPresenter {
    @org.springframework.beans.factory.annotation.Autowired private CalculoService calculoService;
    @org.springframework.beans.factory.annotation.Autowired private FilamentoService filamentoService;
    @org.springframework.beans.factory.annotation.Autowired private ConfiguracaoService configuracaoService;
    @org.springframework.beans.factory.annotation.Autowired private VerificacaoTarifasPlataforma verificacaoTarifas;

    public ModelAndView resultado(CalculoRequest request, CalculoResponse resultado,
            String modoComparacao, BigDecimal precoVendaFixo, boolean valoresAtualizados) {
        if (!"PRECO_FIXO".equals(modoComparacao)) modoComparacao = "LUCRO_DESEJADO";
        ModelAndView mv = new ModelAndView("historico/resultado");
        mv.addObject("calculo", request);
        mv.addObject("resultado", resultado);
        adicionarResumoLote(mv, resultado);
        mv.addObject("modoComparacao", modoComparacao);
        mv.addObject("precoVendaFixo", precoVendaFixo);
        mv.addObject("avisosEstoque", avisosEstoque(request));
        mv.addObject("avisoValoresAtualizados", valoresAtualizados);
        mv.addObject("dataConferenciaTarifas", verificacaoTarifas.dataExibicao());
        mv.addObject("tarifasPrecisamConferencia", verificacaoTarifas.precisaConferir());
        mv.addObject("comparacoes", compararPlataformas(request, resultado, modoComparacao, precoVendaFixo));
        return mv;
    }

    public List<ComparacaoPlataforma> compararPlataformas(
            CalculoRequest base,
            CalculoResponse resultadoBase,
            String modoComparacao,
            BigDecimal precoVendaFixo
    ) {
        record Opcao(PlataformaVenda plataforma, String nome) {
        }

        List<Opcao> opcoes = List.of(
                new Opcao(PlataformaVenda.SHOPEE, "Shopee"),
                new Opcao(PlataformaVenda.TIKTOK, "TikTok Shop"),
                new Opcao(PlataformaVenda.MERCADO_LIVRE, "Mercado Livre"),
                new Opcao(PlataformaVenda.AMAZON, "Amazon")
        );

        boolean precoFixo = "PRECO_FIXO".equals(modoComparacao);

        List<ComparacaoPlataforma> linhas = new ArrayList<>();

        for (Opcao opcao : opcoes) {

            ComparacaoPlataforma linha = new ComparacaoPlataforma();

            linha.setNome(opcao.nome());
            linha.setCodigo(opcao.plataforma().name());

            linha.setSelecionada(
                    base.getPlataformaVenda() == opcao.plataforma()
                            || (opcao.plataforma() == PlataformaVenda.TIKTOK
                            && "TIKTOK_SHOP".equals(
                            String.valueOf(base.getPlataformaVenda())
                    ))
            );

            if (precoFixo && (precoVendaFixo == null
                    || precoVendaFixo.compareTo(BigDecimal.ZERO) <= 0)) {

                linha.setAviso("Informe um preço de venda maior que zero.");

                linhas.add(linha);
                continue;
            }

            // O cálculo originalmente enviado já contém os valores da plataforma
            // selecionada. Reutilizá-los evita repetir toda a leitura e validação
            // da peça, impressora, filamentos e configuração nessa linha.
            if (!precoFixo && linha.isSelecionada()) {
                preencherValoresComparacao(linha, resultadoBase.getPrecoVendaPlataforma(),
                        resultadoBase.getTaxasPlataforma(), resultadoBase.getLucroLiquido());
                linhas.add(linha);
                continue;
            }

            try {
                CalculoRequest copia = copiarParaComparacao(base);
                copia.setPlataformaVenda(opcao.plataforma());

                CalculoHistorico calculo;

                if (precoFixo) {
                    calculo = calculoService.calcularComPrecoFixo(
                            copia,
                            precoVendaFixo
                    );
                } else {
                    calculo = calculoService.calcular(copia);
                }

                BigDecimal preco = calculo.getPrecoVendaPlataforma();
                BigDecimal lucro = calculo.getLucroLiquido();

                preencherValoresComparacao(linha, preco, calculo.getTaxasPlataforma(), lucro);

            } catch (IllegalArgumentException e) {
                linha.setAviso(e.getMessage());
            }

            linhas.add(linha);
        }

        if (precoFixo) {
            linhas.stream()
                    .filter(ComparacaoPlataforma::isCalculada)
                    .filter(l -> l.getLucro() != null)
                    .max(Comparator.comparing(ComparacaoPlataforma::getLucro))
                    .ifPresent(l -> {
                        l.setMelhor(true);
                        l.setEtiquetaMelhor("maior lucro");
                    });

        } else {
            linhas.stream()
                    .filter(ComparacaoPlataforma::isCalculada)
                    .filter(l -> l.getPrecoVenda() != null)
                    .min(Comparator.comparing(ComparacaoPlataforma::getPrecoVenda))
                    .ifPresent(l -> {
                        l.setMelhor(true);
                        l.setEtiquetaMelhor("menor preço");
                    });
        }

        return linhas;
    }

    private void preencherValoresComparacao(
            ComparacaoPlataforma linha,
            BigDecimal preco,
            BigDecimal taxas,
            BigDecimal lucro
    ) {
        linha.setPrecoVenda(preco);
        linha.setTaxas(taxas);
        linha.setLucro(lucro);

        if (preco != null && preco.compareTo(BigDecimal.ZERO) > 0 && lucro != null) {
            linha.setMargem(lucro.multiply(new BigDecimal("100"))
                    .divide(preco, 1, RoundingMode.HALF_UP));
        }
    }

    private CalculoRequest copiarParaComparacao(CalculoRequest original) {

        CalculoRequest copia = new CalculoRequest();

        copia.setPecaId(original.getPecaId());
        copia.setImpressoraId(original.getImpressoraId());
        copia.setEmbalagemId(original.getEmbalagemId());
        copia.setTempoImpressaoMinutos(original.getTempoImpressaoMinutos());
        copia.setUnidades(original.getUnidades());
        copia.setPecasPorImpressao(original.getPecasPorImpressao());
        copia.setTempoTrabalhoMinutos(original.getTempoTrabalhoMinutos());
        copia.setMarkupPercentual(original.getMarkupPercentual());

        List<ItemCalculoRequest> itens = new ArrayList<>();

        if (original.getFilamentos() != null) {
            for (ItemCalculoRequest item : original.getFilamentos()) {

                ItemCalculoRequest novo = new ItemCalculoRequest();

                novo.setFilamentoId(item.getFilamentoId());
                novo.setQuantidadeFilamentoGramas(
                        item.getQuantidadeFilamentoGramas()
                );

                itens.add(novo);
            }
        }

        copia.setFilamentos(itens);

        copia.setTipoAnuncio(original.getTipoAnuncio());
        copia.setPercentualComissaoPlataforma(
                original.getPercentualComissaoPlataforma()
        );
        copia.setCustoAdicionalPlataforma(
                original.getCustoAdicionalPlataforma()
        );
        copia.setPercentualImposto(original.getPercentualImposto());
        copia.setFreteVendedor(original.getFreteVendedor());

        copia.setTaxaCpfShopee(original.isTaxaCpfShopee());
        copia.setTipoPessoaShopee(original.getTipoPessoaShopee());
        copia.setMais450PedidosShopee(original.isMais450PedidosShopee());

        copia.setFreteGratisTikTok(original.isFreteGratisTikTok());
        copia.setPercentualAfiliado(original.getPercentualAfiliado());
        copia.setIsencaoComissaoTikTok(original.isIsencaoComissaoTikTok());

        copia.setFreteMercadoLivreManual(original.getFreteMercadoLivreManual());
        copia.setFreteMercadoLivreManualNecessario(original.isFreteMercadoLivreManualNecessario());

        String faixaPeso = original.getFaixaPesoPacote();

        if (faixaPeso == null || faixaPeso.isBlank()) {
            faixaPeso = estimarFaixaPesoPacote(original);
        }

        copia.setFaixaPesoPacote(faixaPeso);

        return copia;
    }

    private String estimarFaixaPesoPacote(CalculoRequest request) {

        BigDecimal pesoImpressao = BigDecimal.ZERO;

        if (request.getFilamentos() != null) {
            for (ItemCalculoRequest item : request.getFilamentos()) {

                if (item.getQuantidadeFilamentoGramas() != null) {
                    pesoImpressao = pesoImpressao.add(
                            item.getQuantidadeFilamentoGramas()
                    );
                }
            }
        }

        if (pesoImpressao.compareTo(BigDecimal.ZERO) == 0
                && request.getQuantidadeFilamentoGramas() != null) {

            pesoImpressao = request.getQuantidadeFilamentoGramas();
        }

        // Margem estimada para a embalagem.
        BigDecimal pesoEstimado = pesoImpressao.add(
                new BigDecimal("100")
        );

        if (pesoEstimado.compareTo(new BigDecimal("300")) <= 0) {
            return "ATE_300G";
        }

        if (pesoEstimado.compareTo(new BigDecimal("500")) <= 0) {
            return "DE_300G_500G";
        }

        if (pesoEstimado.compareTo(new BigDecimal("1000")) <= 0) {
            return "DE_500G_1KG";
        }

        if (pesoEstimado.compareTo(new BigDecimal("1500")) <= 0) {
            return "DE_1KG_1_5KG";
        }

        if (pesoEstimado.compareTo(new BigDecimal("2000")) <= 0) {
            return "DE_1_5KG_2KG";
        }

        if (pesoEstimado.compareTo(new BigDecimal("3000")) <= 0) {
            return "DE_2KG_3KG";
        }

        if (pesoEstimado.compareTo(new BigDecimal("4000")) <= 0) {
            return "DE_3KG_4KG";
        }

        if (pesoEstimado.compareTo(new BigDecimal("5000")) <= 0) {
            return "DE_4KG_5KG";
        }

        throw new IllegalArgumentException(
                "O peso estimado do pacote ultrapassa 5 kg. "
                        + "Não foi possível calcular o frete do Mercado Livre."
        );
    }

    public void adicionarResumoLote(ModelAndView mv, CalculoResponse calculo) {
        int unidades = calculo.getUnidades() == null ? 1 : calculo.getUnidades();
        int fornadas = calculo.getFornadas() == null ? unidades : calculo.getFornadas();
        BigDecimal precoUnitario = calculo.getPrecoVendaPlataforma() != null
                ? calculo.getPrecoVendaPlataforma() : calculo.getPrecoSugerido();
        BigDecimal lucroUnitario = calculo.getLucroLiquido() != null
                ? calculo.getLucroLiquido() : calculo.getLucroEstimado();
        mv.addObject("unidades", unidades);
        mv.addObject("fornadas", fornadas);
        BigDecimal purgaGramasTotal = calculo.getPurgaGramasPorImpressao() == null
                ? BigDecimal.ZERO
                : calculo.getPurgaGramasPorImpressao().multiply(BigDecimal.valueOf((long) fornadas));
        mv.addObject("purgaGramasTotal", purgaGramasTotal);
        mv.addObject("custoTotalLote", calculo.getCustoTotal().multiply(BigDecimal.valueOf(unidades)));
        mv.addObject("precoUnitario", precoUnitario);
        mv.addObject("precoTotalLote", precoUnitario.multiply(BigDecimal.valueOf(unidades)));
        mv.addObject("lucroTotalLote", lucroUnitario.multiply(BigDecimal.valueOf(unidades)));
    }

    public List<String> avisosEstoque(CalculoRequest request) {
        Map<UUID, BigDecimal> consumo = new LinkedHashMap<>();
        int unidades = request.getUnidades() == null ? 1 : request.getUnidades();
        int pecasPorImpressao = request.getPecasPorImpressao() == null ? 1 : request.getPecasPorImpressao();
        int impressoes = Math.max(1, (unidades + pecasPorImpressao - 1) / pecasPorImpressao);
        BigDecimal gramasBaseTotal = request.getFilamentos() == null ? BigDecimal.ZERO : request.getFilamentos().stream()
                .filter(item -> item.getFilamentoId() != null && item.getQuantidadeFilamentoGramas() != null)
                .map(ItemCalculoRequest::getQuantidadeFilamentoGramas)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (request.getFilamentos() != null) {
            request.getFilamentos().forEach(item -> {
                if (item.getFilamentoId() != null && item.getQuantidadeFilamentoGramas() != null) {
                    consumo.merge(item.getFilamentoId(), item.getQuantidadeFilamentoGramas()
                            .multiply(BigDecimal.valueOf(unidades)),
                            BigDecimal::add);
                }
            });
            if (gramasBaseTotal.signum() > 0) {
                BigDecimal purgaLote = configuracaoService.buscarOuCriarConfiguracao()
                        .getPurgaGramasPorImpressao();
                if (purgaLote != null && purgaLote.signum() > 0) {
                    purgaLote = purgaLote.multiply(BigDecimal.valueOf(impressoes));
                    BigDecimal purgaTotal = purgaLote;
                    request.getFilamentos().stream()
                            .filter(item -> item.getFilamentoId() != null && item.getQuantidadeFilamentoGramas() != null)
                            .forEach(item -> consumo.merge(item.getFilamentoId(), purgaTotal
                                    .multiply(item.getQuantidadeFilamentoGramas())
                                    .divide(gramasBaseTotal, 8, java.math.RoundingMode.HALF_UP), BigDecimal::add));
                }
            }
        }
        List<String> avisos = new ArrayList<>();
        filamentoService.fetchByIds(consumo.keySet()).forEach(filamento -> {
            BigDecimal disponivel = filamento.getEstoqueGramas() == null
                    ? BigDecimal.ZERO : filamento.getEstoqueGramas();
            BigDecimal necessario = consumo.get(filamento.getId());
            if (necessario.compareTo(disponivel) > 0) {
                avisos.add("Estoque de " + filamento.getNome() + ": este lote usa "
                        + necessario.stripTrailingZeros().toPlainString().replace('.', ',')
                        + " g e há " + disponivel.stripTrailingZeros().toPlainString().replace('.', ',')
                        + " g disponíveis. O orçamento pode ser calculado; será preciso ajustar o estoque antes de marcar como produzido.");
            }
        });
        return avisos;
    }
}
