package br.com.criare3d.service;

import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.dto.FilamentoHistoricoUsoResponse;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.service.interfaces.ManutencaoImpressoraService;
import br.com.criare3d.service.interfaces.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.criare3d.models.enums.PlataformaVenda;
@Service
@Transactional(propagation = Propagation.REQUIRED)
public class CalculoHistoricoServiceImp implements CalculoHistoricoService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal CEM = new BigDecimal("100");
    private static final BigDecimal DOIS = new BigDecimal("2");

    @Autowired
    private CalculoHistoricoRepository calculoHistoricoRepository;

    @Autowired
    private PecaService pecaService;

    @Autowired
    private FilamentoService filamentoService;

    @Autowired
    private ImpressoraService impressoraService;

    @Autowired
    private EmbalagemService embalagemService;

    @Autowired
    private ConfiguracaoService configuracaoService;

    @Autowired
    private UsuarioLogadoService usuarioLogadoService;

    @Autowired
    private ManutencaoImpressoraService manutencaoImpressoraService;

    @Override
    public long contarPorFilamento(UUID filamentoId) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return calculoHistoricoRepository.contarUsosDosItensPorFilamento(
                filamentoId, usuarioId
        );
    }

    @Override
    public java.util.List<FilamentoHistoricoUsoResponse> listarUsosPorFilamento(UUID filamentoId) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return calculoHistoricoRepository.buscarUsosDoFilamento(filamentoId, usuarioId);
    }

    @Override
    public long contarPorEmbalagem(UUID embalagemId) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return calculoHistoricoRepository.countByEmbalagem_IdAndUsuario_Id(
                embalagemId, usuarioId
        );
    }

    @Override
    public long contarPorImpressora(UUID impressoraId) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return calculoHistoricoRepository.countByImpressora_IdAndUsuario_Id(
                impressoraId, usuarioId
        );
    }

    @Override
    public long contarPorPeca(UUID pecaId) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return calculoHistoricoRepository.countByPeca_IdAndUsuario_Id(
                pecaId, usuarioId
        );
    }

    @Override
    public CalculoHistorico create(CalculoHistorico calculoHistorico) {
        calculoHistorico.setUsuario(usuarioLogadoService.buscarUsuarioAtual());

        return calculoHistoricoRepository.save(calculoHistorico);
    }

    @Override
    public void delete(UUID id) {
        fetchById(id).ifPresent(historico -> {
            if (Boolean.TRUE.equals(historico.getProduzido())) {
                ajustarEstoque(historico, true);
            }
            calculoHistoricoRepository.delete(historico);
        });
    }

    @Override
    public void deleteAll(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return;
        new LinkedHashSet<>(ids).forEach(this::delete);
    }

    @Override
    public String atualizarProduzido(UUID id, boolean produzido) {
        var encontrado = fetchById(id);
        if (encontrado.isEmpty()) return "naoEncontrado";

        CalculoHistorico historico = encontrado.get();
        boolean jaProduzido = Boolean.TRUE.equals(historico.getProduzido());
        if (jaProduzido == produzido) return "ok";

        if (!produzido && Boolean.TRUE.equals(historico.getVendido())) {
            return "vendaAtiva";
        }

        if (produzido && !ajustarEstoque(historico, false)) {
            return "estoqueInsuficiente";
        }
        if (!produzido) ajustarEstoque(historico, true);

        historico.setProduzido(produzido);
        calculoHistoricoRepository.save(historico);
        return "ok";
    }

    @Override
    public String atualizarVendido(UUID id, boolean vendido) {
        var encontrado = fetchById(id);
        if (encontrado.isEmpty()) return "naoEncontrado";
        CalculoHistorico historico = encontrado.get();
        if (vendido && !Boolean.TRUE.equals(historico.getProduzido())) {
            if (!ajustarEstoque(historico, false)) return "estoqueInsuficiente";
            historico.setProduzido(true);
        }
        historico.setVendido(vendido);
        calculoHistoricoRepository.save(historico);
        return "ok";
    }

    /** Ajusta o saldo; retorna false sem alterar nada se não houver saldo suficiente. */
    private boolean ajustarEstoque(CalculoHistorico historico, boolean devolver) {
        Map<UUID, BigDecimal> quantidades = new HashMap<>();
        for (ItemCalculoHistorico item : historico.getItensFilamento()) {
            Filamento filamento = item.getFilamento();
            if (filamento == null || filamento.getId() == null) continue;
            quantidades.merge(filamento.getId(), item.getQuantidadeFilamentoGramas(), BigDecimal::add);
        }
        // Registros anteriores ao suporte a múltiplos filamentos usam os campos do cálculo.
        if (quantidades.isEmpty()
                && historico.getFilamento() != null
                && historico.getQuantidadeFilamentoGramas() != null) {
            quantidades.put(
                    historico.getFilamento().getId(),
                    historico.getQuantidadeFilamentoGramas()
            );
        }

        Map<UUID, Filamento> filamentos = new HashMap<>();
        for (UUID filamentoId : quantidades.keySet()) {
            var encontrado = filamentoService.fetchById(filamentoId);
            if (encontrado.isEmpty()) {
                if (!devolver) return false;
                continue;
            }
            filamentos.put(filamentoId, encontrado.get());
        }

        if (!devolver) {
            for (Map.Entry<UUID, Filamento> entrada : filamentos.entrySet()) {
                BigDecimal disponivel = entrada.getValue().getEstoqueGramas() == null
                        ? BigDecimal.ZERO : entrada.getValue().getEstoqueGramas();
                if (disponivel.compareTo(quantidades.get(entrada.getKey())) < 0) return false;
            }
        }

        quantidades.forEach((filamentoId, quantidade) -> {
            Filamento filamento = filamentos.get(filamentoId);
            if (filamento == null) return;
            BigDecimal saldo = filamento.getEstoqueGramas() == null
                    ? BigDecimal.ZERO : filamento.getEstoqueGramas();
            filamento.setEstoqueGramas(devolver ? saldo.add(quantidade) : saldo.subtract(quantidade));
            filamentoService.update(filamentoId, filamento);
        });
        return true;
    }

    @Override
    public CalculoHistorico calcular(CalculoRequest request) {

        Peca peca = pecaService.fetchById(request.getPecaId())
                .orElseThrow(() -> new IllegalArgumentException("Peça não encontrada"));

        if (request.getFilamentos() == null || request.getFilamentos().isEmpty()) {
            throw new IllegalArgumentException("Adicione pelo menos um filamento");
        }

        Impressora impressora = impressoraService.fetchById(request.getImpressoraId())
                .orElseThrow(() -> new IllegalArgumentException("Impressora não encontrada"));

        Configuracao configuracao = configuracaoService.buscarOuCriarConfiguracao();

        if (impressora.getPotenciaWatts() == null
                || impressora.getPotenciaWatts().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("A potência da impressora deve ser maior que zero");
        }

        if (configuracao.getTarifaEnergiaKwh() == null
                || configuracao.getTarifaEnergiaKwh().compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("A tarifa de energia é inválida");
        }

        if (request.getTempoImpressaoMinutos() == null
                || request.getTempoImpressaoMinutos() <= 0) {
            throw new IllegalArgumentException("O tempo de impressão deve ser maior que zero");
        }

        BigDecimal markup = request.getMarkupPercentual();

        if (markup == null || markup.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Selecione um markup válido");
        }

        CalculoHistorico historico = CalculoHistorico.builder()
                .itensFilamento(new java.util.ArrayList<>())
                .build();

        BigDecimal custoFilamentoTotal = ZERO;
        Map<UUID, BigDecimal> consumoPorFilamento = new HashMap<>();

        for (ItemCalculoRequest itemRequest : request.getFilamentos()) {

            if (itemRequest.getFilamentoId() == null) {
                throw new IllegalArgumentException("Selecione todos os filamentos");
            }

            BigDecimal quantidade = itemRequest.getQuantidadeFilamentoGramas();

            if (quantidade == null || quantidade.compareTo(ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "A quantidade de cada filamento deve ser maior que zero");
            }

            Filamento filamento = filamentoService.fetchById(itemRequest.getFilamentoId())
                    .orElseThrow(() -> new IllegalArgumentException("Filamento não encontrado"));

            BigDecimal consumoTotal = consumoPorFilamento.merge(
                    filamento.getId(), quantidade, BigDecimal::add);
            BigDecimal disponivel = filamento.getEstoqueGramas() == null
                    ? ZERO : filamento.getEstoqueGramas();
            if (consumoTotal.compareTo(disponivel) > 0) {
                String quantidadeDisponivel = disponivel
                        .setScale(2, RoundingMode.HALF_UP)
                        .toPlainString()
                        .replace('.', ',');
                throw new IllegalArgumentException(
                        "Estoque insuficiente de " + filamento.getNome()
                                + ". Há " + quantidadeDisponivel + " g disponíveis.");
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

            ItemCalculoHistorico item = ItemCalculoHistorico.builder()
                    .calculoHistorico(historico)
                    .filamento(filamento)
                    .nomeFilamento(filamento.getNome())
                    .corFilamento(filamento.getCor())
                    .codigoCor(filamento.getCodigoCor())
                    .precoRoloFilamento(filamento.getPrecoRolo())
                    .pesoRoloGramas(filamento.getPesoRoloGramas())
                    .quantidadeFilamentoGramas(quantidade)
                    .custoFilamento(custoIndividual)
                    .build();

            historico.getItensFilamento().add(item);
            custoFilamentoTotal = custoFilamentoTotal.add(custoIndividual);
        }

        custoFilamentoTotal = moeda(custoFilamentoTotal);

        ItemCalculoHistorico primeiroItem = historico.getItensFilamento().get(0);

        historico.setFilamento(primeiroItem.getFilamento());
        historico.setNomeFilamento(primeiroItem.getNomeFilamento());
        historico.setPrecoRoloFilamento(primeiroItem.getPrecoRoloFilamento());
        historico.setPesoRoloGramas(primeiroItem.getPesoRoloGramas());
        historico.setQuantidadeFilamentoGramas(primeiroItem.getQuantidadeFilamentoGramas());
        historico.setCustoFilamento(custoFilamentoTotal);

        BigDecimal custoEnergia = CalculoFinanceiro.energia(
                impressora.getPotenciaWatts(), request.getTempoImpressaoMinutos(),
                configuracao.getTarifaEnergiaKwh());

        Long tempoTrabalhoMinutos = Boolean.TRUE.equals(configuracao.getMaoObraTempoFixo())
                ? configuracao.getTempoMaoObraFixoMinutos()
                : (request.getTempoTrabalhoMinutos() != null
                    ? request.getTempoTrabalhoMinutos()
                    : 0L);
        if (tempoTrabalhoMinutos == null || tempoTrabalhoMinutos < 0) {
            throw new IllegalArgumentException("Informe um tempo de trabalho manual válido.");
        }

        BigDecimal custoMaoObra = ZERO;
        if (Boolean.TRUE.equals(configuracao.getMaoObraAtivada())) {
            BigDecimal valorHora = configuracao.getValorMaoObraHora();
            if (valorHora == null || valorHora.signum() < 0) {
                throw new IllegalArgumentException("Configure um custo por hora válido para a mão de obra.");
            }
            custoMaoObra = CalculoFinanceiro.maoDeObra(valorHora, tempoTrabalhoMinutos);
        }

        BigDecimal manutencaoPorHora = manutencaoImpressoraService.custoPorHora(impressora.getId());
        if (manutencaoPorHora == null || manutencaoPorHora.signum() < 0) {
            throw new IllegalArgumentException("A estimativa de manutenção por hora é inválida.");
        }
        BigDecimal custoManutencao = CalculoFinanceiro.manutencao(
                manutencaoPorHora, request.getTempoImpressaoMinutos());

        BigDecimal custoDepreciacao = ZERO;
        if (Boolean.TRUE.equals(configuracao.getDepreciacaoAtivada())) {
            BigDecimal valorImpressora = impressora.getValorCompra();
            BigDecimal vidaUtilHoras = impressora.getVidaUtilHoras();
            if (valorImpressora == null || valorImpressora.signum() < 0
                    || vidaUtilHoras == null || vidaUtilHoras.signum() <= 0) {
                throw new IllegalArgumentException("Cadastre o valor de compra e uma vida útil maior que zero para calcular a depreciação.");
            }
            custoDepreciacao = CalculoFinanceiro.depreciacao(
                    valorImpressora, vidaUtilHoras, request.getTempoImpressaoMinutos());
        }

        BigDecimal custoFalha = ZERO;
        if (Boolean.TRUE.equals(configuracao.getTaxaFalhaAtivada())) {
            BigDecimal percentualFalha = configuracao.getPercentualFalha();
            if (percentualFalha == null || percentualFalha.signum() < 0
                    || percentualFalha.compareTo(CEM) > 0) {
                throw new IllegalArgumentException("Configure um percentual de falhas entre zero e 100.");
            }
            custoFalha = CalculoFinanceiro.falhas(custoFilamentoTotal, custoEnergia, percentualFalha);
        }

        BigDecimal custoEmbalagem;
        String nomeEmbalagem;
        Embalagem embalagem = null;

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

            embalagem = embalagemService.fetchById(request.getEmbalagemId())
                    .orElseThrow(() -> new IllegalArgumentException("Embalagem não encontrada"));

            custoEmbalagem = embalagem.getCustoUnitario();
            if (custoEmbalagem == null || custoEmbalagem.compareTo(ZERO) < 0) {
                throw new IllegalArgumentException(
                        "O custo unitário da embalagem deve ser igual ou maior que zero."
                );
            }

            nomeEmbalagem = embalagem.getNome();
        }

        custoEmbalagem = moeda(custoEmbalagem);

        BigDecimal custoTotal = moeda(
                custoFilamentoTotal.add(custoEnergia).add(custoEmbalagem)
                        .add(custoMaoObra).add(custoDepreciacao).add(custoManutencao).add(custoFalha)
        );

        BigDecimal precoSugerido = CalculoFinanceiro.markup(custoTotal, markup);

        historico.setPeca(peca);
        historico.setImpressora(impressora);
        historico.setEmbalagem(embalagem);

        historico.setNomePeca(peca.getNome());
        historico.setNomeImpressora(impressora.getNome());
        historico.setPotenciaWatts(impressora.getPotenciaWatts());
        historico.setTempoImpressaoMinutos(request.getTempoImpressaoMinutos());
        historico.setTarifaEnergiaKwh(configuracao.getTarifaEnergiaKwh());

        historico.setNomeEmbalagem(nomeEmbalagem);
        historico.setCustoEmbalagem(custoEmbalagem);
        historico.setCustoEnergia(custoEnergia);
        historico.setCustoMaoObra(custoMaoObra);
        historico.setCustoDepreciacao(custoDepreciacao);
        historico.setCustoManutencao(custoManutencao);
        historico.setCustoFalha(custoFalha);
        historico.setMaoObraAtivada(Boolean.TRUE.equals(configuracao.getMaoObraAtivada()));
        historico.setValorMaoObraHora(configuracao.getValorMaoObraHora());
        historico.setDepreciacaoAtivada(Boolean.TRUE.equals(configuracao.getDepreciacaoAtivada()));
        historico.setTempoTrabalhoMinutos(tempoTrabalhoMinutos);
        historico.setValorCompraImpressora(impressora.getValorCompra());
        historico.setVidaUtilImpressoraHoras(impressora.getVidaUtilHoras());
        historico.setManutencaoHoraImpressora(manutencaoPorHora);
        historico.setTaxaFalhaAtivada(Boolean.TRUE.equals(configuracao.getTaxaFalhaAtivada()));
        historico.setPercentualFalha(configuracao.getPercentualFalha());
        historico.setCustoTotal(custoTotal);

        historico.setMarkupPercentual(markup);
        historico.setPrecoSugerido(precoSugerido);

        historico.setDataCalculo(
                LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))
        );

        calcularVendaPlataforma(request, historico, precoSugerido, configuracao);

        return historico;
    }

    private void calcularVendaPlataforma(CalculoRequest request, CalculoHistorico historico, BigDecimal base, Configuracao configuracao) {
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

                    if ("CLASSICO".equals(tipoAnuncio) && (comissao.compareTo(new BigDecimal("10")) < 0 || comissao.compareTo(new BigDecimal("14")) > 0)) {
                        throw new IllegalArgumentException("No anúncio Clássico, informe uma comissão entre 10% e 14%");
                    }

                    if ("PREMIUM".equals(tipoAnuncio) && (comissao.compareTo(new BigDecimal("15")) < 0 || comissao.compareTo(new BigDecimal("19")) > 0)) {
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
                            ? new BigDecimal("19.00")
                            : new BigDecimal("14.00");

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
                            ? new BigDecimal("19.00")
                            : new BigDecimal("14.00");

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
            case "SHOPEE" -> precoVenda.compareTo(new BigDecimal("79.99")) <= 0 ? new BigDecimal("20.00") : new BigDecimal("14.00");

            case "TIKTOK", "TIKTOK_SHOP" -> isencaoTikTok ? ZERO : precoVenda.compareTo(new BigDecimal("50.00")) < 0 ? new BigDecimal("10.00") : new BigDecimal("6.00");

            case "MERCADO_LIVRE" -> avancado ? comissaoInformada : "PREMIUM".equals(tipoAnuncio) ? new BigDecimal("19.00") : new BigDecimal("14.00");

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

    @Override
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
                configuracaoService.buscarOuCriarConfiguracao();

        calcularTaxasPrecoFixo(
                request,
                historico,
                precoVenda,
                configuracao,
                plataformaOriginal
        );

        return historico;
    }

    private void calcularTaxasPrecoFixo(CalculoRequest request, CalculoHistorico historico, BigDecimal precoVenda, Configuracao configuracao, PlataformaVenda plataformaEnum) {
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

                comissaoExibida = precoVenda.compareTo(new BigDecimal("79.99")) <= 0 ? new BigDecimal("20.00") : new BigDecimal("14.00");
            }

            case "TIKTOK", "TIKTOK_SHOP" -> {
                freteGratis = request.isFreteGratisTikTok();
                isencaoTikTok = avancado && request.isIsencaoComissaoTikTok();

                taxas = calcularTaxasTikTok(precoVenda, imposto, afiliado, freteGratis, frete.add(adicional), isencaoTikTok
                );

                comissaoExibida = isencaoTikTok ? ZERO : precoVenda.compareTo(new BigDecimal("50.00")) < 0 ? new BigDecimal("10.00") : new BigDecimal("6.00");
            }

            case "MERCADO_LIVRE" -> {
                tipoAnuncio = request.getTipoAnuncio() == null ? "CLASSICO" : request.getTipoAnuncio().toUpperCase();

                BigDecimal comissao = null;

                if (avancado) {
                    comissao = comissaoInformada;

                    if (comissao == null) {
                        throw new IllegalArgumentException("Informe a comissão do Mercado Livre.");
                    }

                    if ("CLASSICO".equals(tipoAnuncio) && (comissao.compareTo(new BigDecimal("10")) < 0 || comissao.compareTo(new BigDecimal("14")) > 0)) {
                        throw new IllegalArgumentException("No anúncio Clássico, informe uma comissão entre 10% e 14%");
                    }

                    if ("PREMIUM".equals(tipoAnuncio) && (comissao.compareTo(new BigDecimal("15")) < 0 || comissao.compareTo(new BigDecimal("19")) > 0)) {
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
                            ? new BigDecimal("19.00")
                            : new BigDecimal("14.00");

                    taxas = calcularTaxasPercentuais(
                            precoVenda,
                            comissao.add(imposto),
                            frete
                    );

                } else {
                    comissao = "PREMIUM".equals(tipoAnuncio)
                            ? new BigDecimal("19.00")
                            : new BigDecimal("14.00");

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

                comissaoExibida = avancado ? comissaoInformada : "PREMIUM".equals(tipoAnuncio) ? new BigDecimal("19.00") : new BigDecimal("14.00");
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
                ? new BigDecimal("3.00")
                : ZERO;

        BigDecimal[] percentuais = {
                new BigDecimal("20.00"),
                new BigDecimal("14.00"),
                new BigDecimal("14.00"),
                new BigDecimal("14.00")
        };

        BigDecimal[] fixos = {
                new BigDecimal("4.50"),
                new BigDecimal("16.00"),
                new BigDecimal("20.00"),
                new BigDecimal("26.00")
        };

        BigDecimal[] limitesInferiores = {
                ZERO,
                new BigDecimal("80.00"),
                new BigDecimal("100.00"),
                new BigDecimal("200.00")
        };

        BigDecimal[] limitesSuperiores = {
                new BigDecimal("79.99"),
                new BigDecimal("99.99"),
                new BigDecimal("199.99"),
                null
        };

        for (int i = 0; i < percentuais.length; i++) {
            BigDecimal preco = calcularPrecoComTaxas(base,
                    custosExtras.add(adicionalCpf).add(fixos[i]), percentuais[i].add(imposto));
            // A regra vigente reduz a tarifa fixa para metade do preço em itens abaixo de R$ 9.
            if (i == 0 && preco.compareTo(new BigDecimal("9.00")) < 0) {
                preco = calcularPrecoComTaxas(base, custosExtras.add(adicionalCpf),
                        percentuais[i].add(imposto).add(new BigDecimal("50.00")));
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

        if (preco.compareTo(new BigDecimal("79.99")) <= 0) {
            percentual = new BigDecimal("20.00");
            fixo = preco.compareTo(new BigDecimal("9.00")) < 0
                    ? preco.divide(DOIS, 2, RoundingMode.HALF_UP)
                    : new BigDecimal("4.50");
        } else if (preco.compareTo(new BigDecimal("99.99")) <= 0) {
            percentual = new BigDecimal("14.00");
            fixo = new BigDecimal("16.00");
        } else if (preco.compareTo(new BigDecimal("199.99")) <= 0) {
            percentual = new BigDecimal("14.00");
            fixo = new BigDecimal("20.00");
        } else {
            percentual = new BigDecimal("14.00");
            fixo = new BigDecimal("26.00");
        }

        BigDecimal total = preco.multiply(percentual.add(imposto))
                .divide(CEM, 8, RoundingMode.HALF_UP)
                .add(fixo)
                .add(custosExtras);

        if (taxaCpf) {
            total = total.add(new BigDecimal("3.00"));
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
                : new BigDecimal("10.00");

        BigDecimal percentualFrete = freteGratis
                ? new BigDecimal("6.00")
                : ZERO;

        BigDecimal precoFaixa1 = calcularPrecoComTaxas(
                base,
                custosExtras.add(new BigDecimal("4.00")),
                comissao.add(imposto).add(afiliado).add(percentualFrete)
        );

        if (precoFaixa1.compareTo(new BigDecimal("50.00")) < 0) {
            return precoFaixa1;
        }

        BigDecimal comissaoFaixa2 = isencao
                ? ZERO
                : new BigDecimal("6.00");

        BigDecimal percentualFaixa2 = comissaoFaixa2
                .add(imposto)
                .add(afiliado);

        BigDecimal fixo = new BigDecimal("6.00").add(custosExtras);

        if (!freteGratis) {
            return calcularPrecoComTaxas(
                    base, fixo, percentualFaixa2
            );
        }

        BigDecimal precoComFretePercentual = calcularPrecoComTaxas(
                base,
                fixo,
                percentualFaixa2.add(new BigDecimal("6.00"))
        );

        BigDecimal taxaFrete = precoComFretePercentual
                .multiply(new BigDecimal("0.06"));

        if (taxaFrete.compareTo(new BigDecimal("50.00")) <= 0) {
            return precoComFretePercentual;
        }

        return calcularPrecoComTaxas(
                base,
                fixo.add(new BigDecimal("50.00")),
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
        boolean primeiraFaixa = preco.compareTo(new BigDecimal("50.00")) < 0;

        BigDecimal comissao = isencao
                ? ZERO
                : primeiraFaixa
                ? new BigDecimal("10.00")
                : new BigDecimal("6.00");

        BigDecimal fixo = primeiraFaixa
                ? new BigDecimal("4.00")
                : new BigDecimal("6.00");

        BigDecimal percentual = comissao.add(imposto).add(afiliado);

        BigDecimal total = preco.multiply(percentual)
                .divide(CEM, 8, RoundingMode.HALF_UP)
                .add(fixo)
                .add(custosExtras);

        if (freteGratis) {
            BigDecimal taxaFrete = preco.multiply(new BigDecimal("0.06"))
                    .min(new BigDecimal("50.00"));
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
        int coluna;

        if (preco.compareTo(new BigDecimal("48.99")) <= 0) {
            coluna = 0;
        } else if (preco.compareTo(new BigDecimal("78.99")) <= 0) {
            coluna = 1;
        } else if (preco.compareTo(new BigDecimal("99.99")) <= 0) {
            coluna = 2;
        } else if (preco.compareTo(new BigDecimal("119.99")) <= 0) {
            coluna = 3;
        } else {
            throw new IllegalArgumentException(
                    "O preço calculado passou de R$ 119,99. Informe o frete manualmente."
            );
        }

        BigDecimal[][] tabela = {
                {new BigDecimal("6.85"), new BigDecimal("8.15"),
                        new BigDecimal("12.95"), new BigDecimal("14.95")},
                {new BigDecimal("6.95"), new BigDecimal("8.25"),
                        new BigDecimal("13.85"), new BigDecimal("16.15")},
                {new BigDecimal("7.15"), new BigDecimal("8.45"),
                        new BigDecimal("14.45"), new BigDecimal("16.85")},
                {new BigDecimal("7.35"), new BigDecimal("8.65"),
                        new BigDecimal("14.75"), new BigDecimal("17.15")},
                {new BigDecimal("7.45"), new BigDecimal("8.75"),
                        new BigDecimal("15.05"), new BigDecimal("17.65")},
                {new BigDecimal("8.65"), new BigDecimal("9.15"),
                        new BigDecimal("16.45"), new BigDecimal("19.15")},
                {new BigDecimal("8.75"), new BigDecimal("9.75"),
                        new BigDecimal("17.85"), new BigDecimal("20.75")},
                {new BigDecimal("8.85"), new BigDecimal("10.25"),
                        new BigDecimal("19.75"), new BigDecimal("22.85")}
        };

        int linha = switch (faixaPeso) {
            case "ATE_300G" -> 0;
            case "DE_300G_500G" -> 1;
            case "DE_500G_1KG" -> 2;
            case "DE_1KG_1_5KG" -> 3;
            case "DE_1_5KG_2KG" -> 4;
            case "DE_2KG_3KG" -> 5;
            case "DE_3KG_4KG" -> 6;
            case "DE_4KG_5KG" -> 7;
            default -> throw new IllegalArgumentException(
                    "Faixa de peso do Mercado Livre ainda não configurada."
            );
        };

        return tabela[linha][coluna];
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

    @Override
    public org.springframework.data.domain.Page<CalculoHistorico> fetchAll(
            org.springframework.data.domain.Pageable pageable
    ) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return calculoHistoricoRepository.findAllByUsuario_Id(
                usuarioId, pageable
        );
    }

    @Override
    public java.util.Optional<CalculoHistorico> fetchById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return calculoHistoricoRepository.buscarPorIdComItens(
                id, usuarioId
        );
    }

    @Override
    public boolean existsById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return calculoHistoricoRepository.existsByIdAndUsuario_Id(
                id, usuarioId
        );
    }


}
