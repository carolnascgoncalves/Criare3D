package br.com.criare3d.service;

import br.com.criare3d.dto.AjusteEstoqueCalculoResponse;
import br.com.criare3d.dto.CalculoRecenteResponse;
import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.FilamentoHistoricoUsoResponse;
import br.com.criare3d.dto.ItemCalculoRequest;
import br.com.criare3d.models.*;
import br.com.criare3d.models.enums.PlataformaVenda;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.repository.ItemCalculoHistoricoRepository;
import br.com.criare3d.service.interfaces.CalculoHistoricoService;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import br.com.criare3d.service.interfaces.EmbalagemService;
import br.com.criare3d.service.interfaces.FilamentoService;
import br.com.criare3d.service.interfaces.ImpressoraService;
import br.com.criare3d.service.interfaces.ManutencaoImpressoraService;
import br.com.criare3d.service.interfaces.PecaService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static br.com.criare3d.service.CalculoFinanceiro.moeda;

@Service
@Transactional
public class CalculoHistoricoServiceImp implements CalculoHistoricoService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal CEM = new BigDecimal("100");

    private final CalculoHistoricoRepository calculoHistoricoRepository;
    private final ItemCalculoHistoricoRepository itemCalculoHistoricoRepository;
    private final CalculoPlataformaService calculoPlataformaService;
    private final PecaService pecaService;
    private final FilamentoService filamentoService;
    private final ImpressoraService impressoraService;
    private final EmbalagemService embalagemService;
    private final ConfiguracaoService configuracaoService;
    private final UsuarioLogadoService usuarioLogadoService;
    private final ManutencaoImpressoraService manutencaoImpressoraService;

    public CalculoHistoricoServiceImp(
            CalculoHistoricoRepository calculoHistoricoRepository,
            ItemCalculoHistoricoRepository itemCalculoHistoricoRepository,
            CalculoPlataformaService calculoPlataformaService,
            PecaService pecaService,
            FilamentoService filamentoService,
            ImpressoraService impressoraService,
            EmbalagemService embalagemService,
            ConfiguracaoService configuracaoService,
            UsuarioLogadoService usuarioLogadoService,
            ManutencaoImpressoraService manutencaoImpressoraService
    ) {
        this.calculoHistoricoRepository = calculoHistoricoRepository;
        this.itemCalculoHistoricoRepository = itemCalculoHistoricoRepository;
        this.calculoPlataformaService = calculoPlataformaService;
        this.pecaService = pecaService;
        this.filamentoService = filamentoService;
        this.impressoraService = impressoraService;
        this.embalagemService = embalagemService;
        this.configuracaoService = configuracaoService;
        this.usuarioLogadoService = usuarioLogadoService;
        this.manutencaoImpressoraService = manutencaoImpressoraService;
    }

    @Override
    public long contarPorFilamento(UUID filamentoId) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return calculoHistoricoRepository.contarUsosDosItensPorFilamento(
                filamentoId, usuarioId
        );
    }

    @Override
    public List<FilamentoHistoricoUsoResponse> listarUsosPorFilamento(UUID filamentoId) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return calculoHistoricoRepository.buscarUsosDoFilamento(filamentoId, usuarioId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> buscarFilamentosComHistoricoProduzido(Collection<UUID> filamentoIds) {
        if (filamentoIds == null || filamentoIds.isEmpty()) return Set.of();
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return Set.copyOf(calculoHistoricoRepository.buscarIdsFilamentoComHistoricoProduzido(
                usuarioId, List.copyOf(filamentoIds)));
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
        if (id != null) deleteAll(List.of(id));
    }

    @Override
    public void deleteAll(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return;

        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        List<UUID> solicitados = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (solicitados.isEmpty()) return;

        List<AjusteEstoqueCalculoResponse> dados =
                calculoHistoricoRepository.buscarDadosParaExclusaoEmLote(solicitados, usuarioId);
        if (dados.isEmpty()) return;

        ajustarEstoqueEmLote(dados);

        List<UUID> encontrados = dados.stream()
                .map(AjusteEstoqueCalculoResponse::calculoId).distinct().toList();
        itemCalculoHistoricoRepository.excluirItensDosCalculos(encontrados, usuarioId);
        calculoHistoricoRepository.deleteAllByIdInBatch(encontrados);
    }

    @Override
    public String atualizarProduzido(UUID id, boolean produzido) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        List<AjusteEstoqueCalculoResponse> dados =
                calculoHistoricoRepository.buscarDadosParaAtualizarStatus(id, usuarioId);
        if (dados.isEmpty()) return "naoEncontrado";

        AjusteEstoqueCalculoResponse historico = dados.get(0);
        boolean jaProduzido = Boolean.TRUE.equals(historico.produzido());
        if (jaProduzido == produzido) return "ok";

        if (!produzido && Boolean.TRUE.equals(historico.vendido())) {
            return "vendaAtiva";
        }

        if (produzido && !ajustarEstoque(dados, false)) {
            return "estoqueInsuficiente";
        }
        if (!produzido) ajustarEstoque(dados, true);

        calculoHistoricoRepository.atualizarStatus(
                id, usuarioId, produzido, Boolean.TRUE.equals(historico.vendido()));
        return "ok";
    }

    @Override
    public String atualizarVendido(UUID id, boolean vendido) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        List<AjusteEstoqueCalculoResponse> dados =
                calculoHistoricoRepository.buscarDadosParaAtualizarStatus(id, usuarioId);
        if (dados.isEmpty()) return "naoEncontrado";

        AjusteEstoqueCalculoResponse historico = dados.get(0);
        boolean produzido = Boolean.TRUE.equals(historico.produzido());
        if (vendido && !produzido) {
            if (!ajustarEstoque(dados, false)) return "estoqueInsuficiente";
            produzido = true;
        }
        calculoHistoricoRepository.atualizarStatus(id, usuarioId, produzido, vendido);
        return "ok";
    }

    /** Ajusta o estoque usando apenas os campos mínimos lidos para a troca de status. */
    private boolean ajustarEstoque(List<AjusteEstoqueCalculoResponse> dados, boolean devolver) {
        Map<UUID, BigDecimal> quantidades = new HashMap<>();
        for (AjusteEstoqueCalculoResponse item : dados) {
            if (item.filamentoItemId() != null && item.quantidadeItem() != null) {
                quantidades.merge(item.filamentoItemId(), item.quantidadeItem(), BigDecimal::add);
            }
        }

        // Compatibilidade com históricos antigos sem itens de filamento vinculados.
        if (quantidades.isEmpty()) {
            AjusteEstoqueCalculoResponse principal = dados.get(0);
            if (principal.filamentoPrincipalId() != null && principal.quantidadePrincipal() != null) {
                quantidades.put(principal.filamentoPrincipalId(), principal.quantidadePrincipal());
            }
        }

        Map<UUID, Filamento> filamentos = new HashMap<>();
        for (Filamento filamento : filamentoService.fetchByIds(quantidades.keySet())) {
            filamentos.put(filamento.getId(), filamento);
        }
        if (!devolver && filamentos.size() != quantidades.size()) return false;

        if (!devolver) {
            for (Map.Entry<UUID, Filamento> entrada : filamentos.entrySet()) {
                BigDecimal disponivel = entrada.getValue().getEstoqueGramas() == null
                        ? ZERO : entrada.getValue().getEstoqueGramas();
                if (disponivel.compareTo(quantidades.get(entrada.getKey())) < 0) return false;
            }
        }

        quantidades.forEach((filamentoId, quantidade) -> {
            Filamento filamento = filamentos.get(filamentoId);
            if (filamento == null) return;
            BigDecimal saldo = filamento.getEstoqueGramas() == null
                    ? ZERO : filamento.getEstoqueGramas();
            filamento.setEstoqueGramas(devolver ? saldo.add(quantidade) : saldo.subtract(quantidade));
        });
        return true;
    }

    private void ajustarEstoqueEmLote(List<AjusteEstoqueCalculoResponse> dados) {
        Map<UUID, Map<UUID, BigDecimal>> quantidadesPorCalculo = new HashMap<>();
        Map<UUID, AjusteEstoqueCalculoResponse> exemploPorCalculo = new HashMap<>();

        for (AjusteEstoqueCalculoResponse item : dados) {
            if (!Boolean.TRUE.equals(item.produzido())) continue;
            exemploPorCalculo.putIfAbsent(item.calculoId(), item);
            Map<UUID, BigDecimal> quantidades = quantidadesPorCalculo.computeIfAbsent(
                    item.calculoId(), ignorado -> new HashMap<>());
            if (item.filamentoItemId() != null && item.quantidadeItem() != null) {
                quantidades.merge(item.filamentoItemId(), item.quantidadeItem(), BigDecimal::add);
            }
        }

        Map<UUID, BigDecimal> totaisPorFilamento = new HashMap<>();
        quantidadesPorCalculo.forEach((calculoId, quantidades) -> {
            if (quantidades.isEmpty()) {
                AjusteEstoqueCalculoResponse legado = exemploPorCalculo.get(calculoId);
                if (legado != null && legado.filamentoPrincipalId() != null
                        && legado.quantidadePrincipal() != null) {
                    quantidades.put(legado.filamentoPrincipalId(), legado.quantidadePrincipal());
                }
            }
            quantidades.forEach((filamentoId, quantidade) ->
                    totaisPorFilamento.merge(filamentoId, quantidade, BigDecimal::add));
        });

        if (totaisPorFilamento.isEmpty()) return;

        for (Filamento filamento : filamentoService.fetchByIds(totaisPorFilamento.keySet())) {
            BigDecimal saldo = filamento.getEstoqueGramas() == null
                    ? ZERO : filamento.getEstoqueGramas();
            filamento.setEstoqueGramas(saldo.add(totaisPorFilamento.get(filamento.getId())));
        }
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

        java.util.Set<UUID> idsFilamentosSolicitados = new LinkedHashSet<>();
        for (ItemCalculoRequest itemRequest : request.getFilamentos()) {
            if (itemRequest.getFilamentoId() != null) {
                idsFilamentosSolicitados.add(itemRequest.getFilamentoId());
            }
        }
        Map<UUID, Filamento> filamentosCadastrados = new HashMap<>();
        for (Filamento filamento : filamentoService.fetchByIds(idsFilamentosSolicitados)) {
            filamentosCadastrados.put(filamento.getId(), filamento);
        }

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
            if (filamento == null) {
                throw new IllegalArgumentException("Filamento não encontrado");
            }

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
                throw new IllegalArgumentException(
                        "A depreciação está ativada nas Configurações, mas faltam dados na impressora selecionada. " +
                        "Cadastre o valor de compra e a vida útil em Impressoras; a calculadora usará esses dados " +
                        "e o tempo de impressão para incluir esse custo no cálculo."
                );
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

        calculoPlataformaService.calcularVendaPlataforma(request, historico, precoSugerido, configuracao);

        return historico;
    }

    // Regras de tarifas ficam isoladas em CalculoPlataformaService.

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

        calculoPlataformaService.calcularTaxasPrecoFixo(
                request,
                historico,
                precoVenda,
                configuracao,
                plataformaOriginal
        );

        return historico;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<CalculoRecenteResponse> fetchRecentes(int limite) {
        if (limite <= 0) return java.util.List.of();
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return calculoHistoricoRepository.buscarCalculosRecentes(
                usuarioId, PageRequest.of(0, limite)
        );
    }

    @Override
    public long contarProduzidos() {
        return calculoHistoricoRepository.countByProduzidoTrueAndUsuario_Id(
                usuarioLogadoService.buscarIdUsuarioAtual()
        );
    }

    @Override
    public long contarVendidos() {
        return calculoHistoricoRepository.countByVendidoTrueAndUsuario_Id(
                usuarioLogadoService.buscarIdUsuarioAtual()
        );
    }

    @Override
    public Optional<CalculoHistorico> fetchById(UUID id) {
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
