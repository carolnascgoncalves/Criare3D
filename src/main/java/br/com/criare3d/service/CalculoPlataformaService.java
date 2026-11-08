package br.com.criare3d.service;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.Configuracao;
import br.com.criare3d.models.enums.PlataformaVenda;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static br.com.criare3d.service.TabelaTarifasPlataforma.*;

@Service
public class CalculoPlataformaService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal DOIS = new BigDecimal("2");

    void calcularVendaPlataforma(CalculoRequest request, CalculoHistorico historico, BigDecimal base, Configuracao configuracao) {
        boolean avancado = Boolean.TRUE.equals(configuracao.getFormularioAvancado());

        String plataforma = request.getPlataformaVenda() == null ? "NENHUMA" : request.getPlataformaVenda().name();

        BigDecimal imposto = percentual(request.getPercentualImposto());
        BigDecimal frete = obterFreteVendedor(request, avancado, plataforma);

        BigDecimal adicional = avancado && "MERCADO_LIVRE".equals(plataforma) ? valor(request.getCustoAdicionalPlataforma()) : ZERO;

        BigDecimal afiliado = avancado && ("TIKTOK".equals(plataforma) || "TIKTOK_SHOP".equals(plataforma)) ? percentual(request.getPercentualAfiliado()) : ZERO;

        BigDecimal comissaoInformada = percentual(request.getPercentualComissaoPlataforma());

        validarNaoNegativo(imposto, "O imposto não pode ser negativo");
        validarNaoNegativo(frete, "O frete do vendedor não pode ser negativo");
        validarNaoNegativo(adicional, "O custo adicional não pode ser negativo");
        validarNaoNegativo(afiliado, "A comissão do afiliado não pode ser negativa");
        validarNaoNegativo(comissaoInformada, "A comissão não pode ser negativa");

        BigDecimal precoVenda;
        BigDecimal taxasVenda = ZERO;

        boolean taxaCpf = false;
        boolean freteGratis = false;
        boolean mais450 = false;
        boolean isencaoTikTok = false;

        String tipoPessoa = null;
        String tipoAnuncio = null;
        String faixaPeso = null;

        switch (plataforma) {
            case "SHOPEE" -> {
                tipoPessoa = avancado ? request.getTipoPessoaShopee() : "CPF";

                if (tipoPessoa == null) {
                    tipoPessoa = "CPF";
                }

                tipoPessoa = tipoPessoa.toUpperCase();

                mais450 = avancado && "CPF".equals(tipoPessoa) && request.isMais450PedidosShopee();

                taxaCpf = mais450;

                precoVenda = calcularPrecoShopee(base, frete.add(adicional), imposto, mais450);

                taxasVenda = calcularTaxasShopee(precoVenda, imposto, frete.add(adicional), mais450);
            }

            case "TIKTOK", "TIKTOK_SHOP" -> {
                freteGratis = request.isFreteGratisTikTok();
                isencaoTikTok = avancado && request.isIsencaoComissaoTikTok();

                BigDecimal comissaoAfiliado = avancado ? afiliado : ZERO;

                precoVenda = calcularPrecoTikTok(base, frete.add(adicional), imposto, comissaoAfiliado, freteGratis, isencaoTikTok);

                taxasVenda = calcularTaxasTikTok(precoVenda, imposto, comissaoAfiliado, freteGratis, frete.add(adicional), isencaoTikTok);
            }

            case "MERCADO_LIVRE" -> {
                tipoAnuncio = request.getTipoAnuncio() == null ? "CLASSICO" : request.getTipoAnuncio().toUpperCase();

                BigDecimal comissao = null;

                if (avancado) {
                    comissao = comissaoInformada;

                    if (comissao == null) {
                        throw new IllegalArgumentException("Informe a comissão do Mercado Livre.");
                    }

                    if ("CLASSICO".equals(tipoAnuncio) && (comissao.compareTo(ML_CLASSICO_COMISSAO_MINIMA) < 0 || comissao.compareTo(ML_CLASSICO_COMISSAO_MAXIMA) > 0)) {
                        throw new IllegalArgumentException("No anúncio Clássico, informe uma comissão entre 10% e 14%");
                    }

                    if ("PREMIUM".equals(tipoAnuncio) && (comissao.compareTo(ML_PREMIUM_COMISSAO_MINIMA) < 0 || comissao.compareTo(ML_PREMIUM_COMISSAO_MAXIMA) > 0)) {
                        throw new IllegalArgumentException("No anúncio Premium, informe uma comissão entre 15% e 19%");
                    }

                    if (!"CLASSICO".equals(tipoAnuncio) && !"PREMIUM".equals(tipoAnuncio)) {
                        throw new IllegalArgumentException("Selecione o tipo de anúncio");
                    }

                    BigDecimal percentualTotal = comissao.add(imposto);
                    BigDecimal fixos = frete.add(adicional);

                    precoVenda = calcularPrecoComTaxas(base, fixos, percentualTotal);

                    taxasVenda = calcularTaxasPercentuais(precoVenda, percentualTotal, fixos);

                } else if (request.isFreteMercadoLivreManualNecessario()) {
                    faixaPeso = null;
                    comissao = "PREMIUM".equals(tipoAnuncio)
                            ? ML_PREMIUM_COMISSAO_PADRAO
                            : ML_CLASSICO_COMISSAO_PADRAO;

                    BigDecimal percentualTotal = comissao.add(imposto);

                    precoVenda = calcularPrecoComTaxas(
                            base,
                            frete,
                            percentualTotal
                    );

                    taxasVenda = calcularTaxasPercentuais(
                            precoVenda,
                            percentualTotal,
                            frete
                    );

                } else {
                    comissao = "PREMIUM".equals(tipoAnuncio)
                            ? ML_PREMIUM_COMISSAO_PADRAO
                            : ML_CLASSICO_COMISSAO_PADRAO;

                    faixaPeso = request.getFaixaPesoPacote();

                    if (faixaPeso == null || faixaPeso.isBlank()) {
                        throw new IllegalArgumentException(
                                "Selecione o peso do pacote para o Mercado Livre"
                        );
                    }

                    precoVenda = calcularPrecoMercadoLivreSimples(
                            base,
                            frete,
                            imposto,
                            comissao,
                            faixaPeso
                    );

                    BigDecimal freteMercadoLivre =
                            buscarFreteMercadoLivre(faixaPeso, precoVenda);

                    taxasVenda = calcularTaxasPercentuais(
                            precoVenda,
                            comissao.add(imposto),
                            frete.add(freteMercadoLivre)
                    );
                }

                historico.setTipoAnuncio(tipoAnuncio);
            }

            default -> {
                plataforma = "NENHUMA";
                precoVenda = base;
            }
        }

        precoVenda = moeda(precoVenda);
        taxasVenda = moeda(taxasVenda);

        historico.setPlataformaVenda(plataforma);
        historico.setTipoAnuncio(tipoAnuncio);
        BigDecimal comissaoExibida = switch (plataforma) {
            case "SHOPEE" -> precoVenda.compareTo(SHOPEE_LIMITE_80) <= 0 ? SHOPEE_COMISSAO_ATE_79_99 : SHOPEE_COMISSAO_ACIMA_79_99;

            case "TIKTOK", "TIKTOK_SHOP" -> isencaoTikTok ? ZERO : precoVenda.compareTo(TIKTOK_LIMITE_PRIMEIRA_FAIXA) < 0 ? TIKTOK_COMISSAO_PRIMEIRA_FAIXA : TIKTOK_COMISSAO_DEMAIS_FAIXAS;

            case "MERCADO_LIVRE" -> avancado ? comissaoInformada : "PREMIUM".equals(tipoAnuncio) ? ML_PREMIUM_COMISSAO_PADRAO : ML_CLASSICO_COMISSAO_PADRAO;

            default -> ZERO;
        };

        historico.setPercentualComissaoPlataforma(comissaoExibida);

        historico.setCustoAdicionalPlataforma(adicional);
        historico.setPercentualImposto(imposto);
        historico.setFreteVendedor(frete);
        historico.setTaxaCpfShopee(taxaCpf);
        historico.setFreteGratisTikTok(freteGratis);
        historico.setPercentualAfiliado(afiliado);

        historico.setTipoPessoaShopee(tipoPessoa);
        historico.setMais450PedidosShopee(mais450);
        historico.setIsencaoComissaoTikTok(isencaoTikTok);
        historico.setFaixaPesoPacote(faixaPeso);
        historico.setFormularioAvancado(avancado);

        historico.setPrecoVendaPlataforma(precoVenda);
        historico.setTaxasPlataforma(taxasVenda);
        historico.setLucroLiquido(
                moeda(precoVenda.subtract(taxasVenda)
                        .subtract(historico.getCustoTotal()))
        );
    }

    void calcularTaxasPrecoFixo(CalculoRequest request, CalculoHistorico historico, BigDecimal precoVenda, Configuracao configuracao, PlataformaVenda plataformaEnum) {
        boolean avancado = Boolean.TRUE.equals(configuracao.getFormularioAvancado());

        String plataforma = plataformaEnum == null ? "NENHUMA" : plataformaEnum.name();

        BigDecimal imposto = percentual(request.getPercentualImposto());
        BigDecimal frete = obterFreteVendedor(request, avancado, plataforma);

        BigDecimal adicional = avancado && "MERCADO_LIVRE".equals(plataforma) ? valor(request.getCustoAdicionalPlataforma()) : ZERO;

        BigDecimal afiliado = avancado && ("TIKTOK".equals(plataforma) || "TIKTOK_SHOP".equals(plataforma)) ? percentual(request.getPercentualAfiliado()) : ZERO;

        BigDecimal comissaoInformada =
                percentual(request.getPercentualComissaoPlataforma());

        validarNaoNegativo(imposto, "O imposto não pode ser negativo");
        validarNaoNegativo(frete, "O frete do vendedor não pode ser negativo");
        validarNaoNegativo(adicional, "O custo adicional não pode ser negativo");
        validarNaoNegativo(afiliado, "A comissão do afiliado não pode ser negativa");
        validarNaoNegativo(comissaoInformada, "A comissão não pode ser negativa");

        BigDecimal taxas = ZERO;

        boolean taxaCpf = false;
        boolean freteGratis = false;
        boolean mais450 = false;
        boolean isencaoTikTok = false;

        String tipoPessoa = null;
        String tipoAnuncio = null;
        String faixaPeso = null;

        BigDecimal comissaoExibida;

        switch (plataforma) {

            case "SHOPEE" -> {
                tipoPessoa = avancado ? request.getTipoPessoaShopee() : "CPF";

                if (tipoPessoa == null) {
                    tipoPessoa = "CPF";
                }

                tipoPessoa = tipoPessoa.toUpperCase();

                mais450 = avancado && "CPF".equals(tipoPessoa) && request.isMais450PedidosShopee();

                taxaCpf = mais450;

                taxas = calcularTaxasShopee(precoVenda, imposto, frete.add(adicional), mais450);

                comissaoExibida = precoVenda.compareTo(SHOPEE_LIMITE_80) <= 0 ? SHOPEE_COMISSAO_ATE_79_99 : SHOPEE_COMISSAO_ACIMA_79_99;
            }

            case "TIKTOK", "TIKTOK_SHOP" -> {
                freteGratis = request.isFreteGratisTikTok();
                isencaoTikTok = avancado && request.isIsencaoComissaoTikTok();

                taxas = calcularTaxasTikTok(precoVenda, imposto, afiliado, freteGratis, frete.add(adicional), isencaoTikTok
                );

                comissaoExibida = isencaoTikTok ? ZERO : precoVenda.compareTo(TIKTOK_LIMITE_PRIMEIRA_FAIXA) < 0 ? TIKTOK_COMISSAO_PRIMEIRA_FAIXA : TIKTOK_COMISSAO_DEMAIS_FAIXAS;
            }

            case "MERCADO_LIVRE" -> {
                tipoAnuncio = request.getTipoAnuncio() == null ? "CLASSICO" : request.getTipoAnuncio().toUpperCase();

                BigDecimal comissao = null;

                if (avancado) {
                    comissao = comissaoInformada;

                    if (comissao == null) {
                        throw new IllegalArgumentException("Informe a comissão do Mercado Livre.");
                    }

                    if ("CLASSICO".equals(tipoAnuncio) && (comissao.compareTo(ML_CLASSICO_COMISSAO_MINIMA) < 0 || comissao.compareTo(ML_CLASSICO_COMISSAO_MAXIMA) > 0)) {
                        throw new IllegalArgumentException("No anúncio Clássico, informe uma comissão entre 10% e 14%");
                    }

                    if ("PREMIUM".equals(tipoAnuncio) && (comissao.compareTo(ML_PREMIUM_COMISSAO_MINIMA) < 0 || comissao.compareTo(ML_PREMIUM_COMISSAO_MAXIMA) > 0)) {
                        throw new IllegalArgumentException("No anúncio Premium, informe uma comissão entre 15% e 19%");
                    }

                    if (!"CLASSICO".equals(tipoAnuncio)
                            && !"PREMIUM".equals(tipoAnuncio)) {
                        throw new IllegalArgumentException("Selecione o tipo de anúncio");
                    }

                    taxas = calcularTaxasPercentuais(precoVenda, comissao.add(imposto), frete.add(adicional));

                } else if (request.isFreteMercadoLivreManualNecessario()) {
                    faixaPeso = null;
                    comissao = "PREMIUM".equals(tipoAnuncio)
                            ? ML_PREMIUM_COMISSAO_PADRAO
                            : ML_CLASSICO_COMISSAO_PADRAO;

                    taxas = calcularTaxasPercentuais(
                            precoVenda,
                            comissao.add(imposto),
                            frete
                    );

                } else {
                    comissao = "PREMIUM".equals(tipoAnuncio)
                            ? ML_PREMIUM_COMISSAO_PADRAO
                            : ML_CLASSICO_COMISSAO_PADRAO;

                    faixaPeso = request.getFaixaPesoPacote();

                    if (faixaPeso == null || faixaPeso.isBlank()) {
                        throw new IllegalArgumentException(
                                "Selecione o peso do pacote para o Mercado Livre"
                        );
                    }

                    BigDecimal freteMercadoLivre =
                            buscarFreteMercadoLivre(faixaPeso, precoVenda);

                    taxas = calcularTaxasPercentuais(
                            precoVenda,
                            comissao.add(imposto),
                            frete.add(freteMercadoLivre)
                    );
                }

                comissaoExibida = avancado ? comissaoInformada : "PREMIUM".equals(tipoAnuncio) ? ML_PREMIUM_COMISSAO_PADRAO : ML_CLASSICO_COMISSAO_PADRAO;
            }

            default -> {
                comissaoExibida = ZERO;
                taxas = ZERO;
            }
        }

        taxas = moeda(taxas);

        historico.setPlataformaVenda(plataforma);
        historico.setTipoAnuncio(tipoAnuncio);
        historico.setPercentualComissaoPlataforma(comissaoExibida);

        historico.setCustoAdicionalPlataforma(adicional);
        historico.setPercentualImposto(imposto);
        historico.setFreteVendedor(frete);

        historico.setTaxaCpfShopee(taxaCpf);
        historico.setFreteGratisTikTok(freteGratis);
        historico.setPercentualAfiliado(afiliado);

        historico.setTipoPessoaShopee(tipoPessoa);
        historico.setMais450PedidosShopee(mais450);
        historico.setIsencaoComissaoTikTok(isencaoTikTok);
        historico.setFaixaPesoPacote(faixaPeso);
        historico.setFormularioAvancado(avancado);

        historico.setPrecoVendaPlataforma(moeda(precoVenda));
        historico.setTaxasPlataforma(taxas);

        historico.setLucroLiquido(moeda(precoVenda.subtract(taxas).subtract(historico.getCustoTotal())));
    }

    BigDecimal calcularPrecoShopee(
            BigDecimal base,
            BigDecimal custosExtras,
            BigDecimal imposto,
            boolean taxaCpf
    ) {
        BigDecimal adicionalCpf = taxaCpf
                ? SHOPEE_TAXA_CPF
                : ZERO;

        BigDecimal[] percentuais = {
                SHOPEE_COMISSAO_ATE_79_99,
                SHOPEE_COMISSAO_ACIMA_79_99,
                SHOPEE_COMISSAO_ACIMA_79_99,
                SHOPEE_COMISSAO_ACIMA_79_99
        };

        BigDecimal[] fixos = {
                SHOPEE_FIXO_ATE_79_99,
                SHOPEE_FIXO_80_A_99_99,
                SHOPEE_FIXO_100_A_199_99,
                SHOPEE_FIXO_A_PARTIR_200
        };

        BigDecimal[] limitesInferiores = {
                ZERO,
                SHOPEE_INICIO_FAIXA_80,
                SHOPEE_INICIO_FAIXA_100,
                SHOPEE_INICIO_FAIXA_200
        };

        BigDecimal[] limitesSuperiores = {
                SHOPEE_LIMITE_80,
                SHOPEE_LIMITE_100,
                SHOPEE_LIMITE_200,
                null
        };

        for (int i = 0; i < percentuais.length; i++) {
            BigDecimal preco = calcularPrecoComTaxas(base,
                    custosExtras.add(adicionalCpf).add(fixos[i]), percentuais[i].add(imposto));
            // A regra vigente reduz a tarifa fixa para metade do preço em itens abaixo de R$ 9.
            if (i == 0 && preco.compareTo(SHOPEE_PRECO_MINIMO) < 0) {
                preco = calcularPrecoComTaxas(base, custosExtras.add(adicionalCpf),
                        percentuais[i].add(imposto).add(SHOPEE_COMISSAO_EQUIVALENTE_PRECO_BAIXO));
            }

            boolean acima = preco.compareTo(limitesInferiores[i]) >= 0;
            boolean abaixo = limitesSuperiores[i] == null
                    || preco.compareTo(limitesSuperiores[i]) <= 0;

            if (acima && abaixo) {
                return preco;
            }
        }

        throw new IllegalArgumentException(
                "Não foi possível determinar a faixa de cobrança da Shopee"
        );
    }

    BigDecimal calcularTaxasShopee(
            BigDecimal preco,
            BigDecimal imposto,
            BigDecimal custosExtras,
            boolean taxaCpf
    ) {
        BigDecimal percentual;
        BigDecimal fixo;

        if (preco.compareTo(SHOPEE_LIMITE_80) <= 0) {
            percentual = SHOPEE_COMISSAO_ATE_79_99;
            fixo = preco.compareTo(SHOPEE_PRECO_MINIMO) < 0
                    ? preco.divide(DOIS, 2, RoundingMode.HALF_UP)
                    : SHOPEE_FIXO_ATE_79_99;
        } else if (preco.compareTo(SHOPEE_LIMITE_100) <= 0) {
            percentual = SHOPEE_COMISSAO_ACIMA_79_99;
            fixo = SHOPEE_FIXO_80_A_99_99;
        } else if (preco.compareTo(SHOPEE_LIMITE_200) <= 0) {
            percentual = SHOPEE_COMISSAO_ACIMA_79_99;
            fixo = SHOPEE_FIXO_100_A_199_99;
        } else {
            percentual = SHOPEE_COMISSAO_ACIMA_79_99;
            fixo = SHOPEE_FIXO_A_PARTIR_200;
        }

        BigDecimal total = preco.multiply(percentual.add(imposto))
                .divide(CEM, 8, RoundingMode.HALF_UP)
                .add(fixo)
                .add(custosExtras);

        if (taxaCpf) {
            total = total.add(SHOPEE_TAXA_CPF);
        }

        return moeda(total);
    }

    BigDecimal calcularPrecoTikTok(
            BigDecimal base,
            BigDecimal custosExtras,
            BigDecimal imposto,
            BigDecimal afiliado,
            boolean freteGratis,
            boolean isencao
    ) {
        BigDecimal comissao = isencao
                ? ZERO
                : TIKTOK_COMISSAO_PRIMEIRA_FAIXA;

        BigDecimal percentualFrete = freteGratis
                ? TIKTOK_PERCENTUAL_FRETE_GRATIS
                : ZERO;

        BigDecimal precoFaixa1 = calcularPrecoComTaxas(
                base,
                custosExtras.add(TIKTOK_TAXA_FIXA_PRIMEIRA_FAIXA),
                comissao.add(imposto).add(afiliado).add(percentualFrete)
        );

        if (precoFaixa1.compareTo(TIKTOK_LIMITE_PRIMEIRA_FAIXA) < 0) {
            return precoFaixa1;
        }

        BigDecimal comissaoFaixa2 = isencao
                ? ZERO
                : TIKTOK_COMISSAO_DEMAIS_FAIXAS;

        BigDecimal percentualFaixa2 = comissaoFaixa2
                .add(imposto)
                .add(afiliado);

        BigDecimal fixo = TIKTOK_TAXA_FIXA_DEMAIS_FAIXAS.add(custosExtras);

        if (!freteGratis) {
            return calcularPrecoComTaxas(
                    base, fixo, percentualFaixa2
            );
        }

        BigDecimal precoComFretePercentual = calcularPrecoComTaxas(
                base,
                fixo,
                percentualFaixa2.add(TIKTOK_PERCENTUAL_FRETE_GRATIS)
        );

        BigDecimal taxaFrete = precoComFretePercentual
                .multiply(TIKTOK_PERCENTUAL_FRETE_GRATIS.movePointLeft(2));

        if (taxaFrete.compareTo(TIKTOK_TETO_TAXA_FRETE) <= 0) {
            return precoComFretePercentual;
        }

        return calcularPrecoComTaxas(
                base,
                fixo.add(TIKTOK_TETO_TAXA_FRETE),
                percentualFaixa2
        );
    }

    BigDecimal calcularTaxasTikTok(
            BigDecimal preco,
            BigDecimal imposto,
            BigDecimal afiliado,
            boolean freteGratis,
            BigDecimal custosExtras,
            boolean isencao
    ) {
        boolean primeiraFaixa = preco.compareTo(TIKTOK_LIMITE_PRIMEIRA_FAIXA) < 0;

        BigDecimal comissao = isencao
                ? ZERO
                : primeiraFaixa
                ? TIKTOK_COMISSAO_PRIMEIRA_FAIXA
                : TIKTOK_COMISSAO_DEMAIS_FAIXAS;

        BigDecimal fixo = primeiraFaixa
                ? TIKTOK_TAXA_FIXA_PRIMEIRA_FAIXA
                : TIKTOK_TAXA_FIXA_DEMAIS_FAIXAS;

        BigDecimal percentual = comissao.add(imposto).add(afiliado);

        BigDecimal total = preco.multiply(percentual)
                .divide(CEM, 8, RoundingMode.HALF_UP)
                .add(fixo)
                .add(custosExtras);

        if (freteGratis) {
            BigDecimal taxaFrete = preco.multiply(TIKTOK_PERCENTUAL_FRETE_GRATIS.movePointLeft(2))
                    .min(TIKTOK_TETO_TAXA_FRETE);
            total = total.add(taxaFrete);
        }

        return moeda(total);
    }

    BigDecimal calcularPrecoMercadoLivreSimples(
            BigDecimal base,
            BigDecimal freteVendedor,
            BigDecimal imposto,
            BigDecimal comissao,
            String faixaPeso
    ) {
        BigDecimal preco = calcularPrecoComTaxas(
                base,
                freteVendedor,
                comissao.add(imposto)
        );

        for (int i = 0; i < 5; i++) {
            BigDecimal freteMercadoLivre = buscarFreteMercadoLivre(
                    faixaPeso, preco
            );

            BigDecimal novoPreco = calcularPrecoComTaxas(
                    base,
                    freteVendedor.add(freteMercadoLivre),
                    comissao.add(imposto)
            );

            if (novoPreco.compareTo(preco) == 0) {
                return novoPreco;
            }

            preco = novoPreco;
        }

        buscarFreteMercadoLivre(faixaPeso, preco);
        return preco;
    }

    private BigDecimal buscarFreteMercadoLivre(
            String faixaPeso,
            BigDecimal preco
    ) {
        return TabelaTarifasPlataforma.freteMercadoLivre(faixaPeso, preco);
    }

    BigDecimal calcularPrecoComTaxas(
            BigDecimal base,
            BigDecimal custosFixos,
            BigDecimal percentualTotal
    ) {
        BigDecimal denominador = BigDecimal.ONE.subtract(
                percentualTotal.divide(CEM, 10, RoundingMode.HALF_UP)
        );

        if (denominador.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "A soma dos percentuais de taxas não pode chegar a 100%"
            );
        }

        return base.add(custosFixos)
                .divide(denominador, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularTaxasPercentuais(BigDecimal preco, BigDecimal percentual, BigDecimal fixos) {
        return moeda(preco.multiply(percentual).divide(CEM, 8, RoundingMode.HALF_UP).add(fixos));
    }

    private BigDecimal obterFreteVendedor(CalculoRequest request, boolean avancado, String plataforma) {
        if (!avancado && "MERCADO_LIVRE".equals(plataforma) && request.isFreteMercadoLivreManualNecessario()) {
            BigDecimal freteManual = request.getFreteMercadoLivreManual();

            if (freteManual == null) {
                throw new IllegalArgumentException("Informe o custo de envio que ficará por sua conta.");
            }

            return valor(freteManual);
        }

        return valor(request.getFreteVendedor());
    }


    private BigDecimal percentual(BigDecimal valor) {
        return valor == null ? ZERO : valor;
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor == null ? ZERO : valor;
    }

    private BigDecimal moeda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private void validarNaoNegativo(BigDecimal valor, String mensagem) {
        if (valor.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(mensagem);
        }
    }

}
