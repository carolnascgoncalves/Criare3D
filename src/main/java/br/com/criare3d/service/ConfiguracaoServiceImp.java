package br.com.criare3d.service;

import br.com.criare3d.models.Configuracao;
import br.com.criare3d.repository.ConfiguracaoRepository;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Transactional
public class ConfiguracaoServiceImp implements ConfiguracaoService {

    private final ConfiguracaoRepository configuracaoRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final ConcurrentMap<UUID, Configuracao> configuracoesPorUsuario = new ConcurrentHashMap<>();

    public ConfiguracaoServiceImp(ConfiguracaoRepository configuracaoRepository,
                                  UsuarioLogadoService usuarioLogadoService) {
        this.configuracaoRepository = configuracaoRepository;
        this.usuarioLogadoService = usuarioLogadoService;
    }

    @Override
    public Configuracao create(Configuracao configuracao) {
        configuracao.setUsuario(usuarioLogadoService.buscarUsuarioAtual());

        if (configuracao.getEmbalagemFixa() == null) {
            configuracao.setEmbalagemFixa(false);
        }

        if (configuracao.getMarkupFixo() == null) {
            configuracao.setMarkupFixo(false);
        }

        if (configuracao.getFormularioAvancado() == null) {
            configuracao.setFormularioAvancado(false);
        }

        Configuracao salva = configuracaoRepository.save(configuracao);
        configuracoesPorUsuario.put(salva.getUsuario().getId(), salva);
        return salva;
    }

    @Override
    public Optional<Configuracao> update(UUID id, Configuracao configuracao) {
        return fetchById(id).map(atual -> {
            if (configuracao.getMarkupPercentual() != null) {
                atual.setMarkupPercentual(configuracao.getMarkupPercentual());
            }

            if (configuracao.getMarkupFixo() != null) {
                atual.setMarkupFixo(configuracao.getMarkupFixo());
            }

            if (configuracao.getTarifaEnergiaKwh() != null) {
                atual.setTarifaEnergiaKwh(configuracao.getTarifaEnergiaKwh());
            }

            if (configuracao.getEmbalagemFixa() != null) {
                atual.setEmbalagemFixa(configuracao.getEmbalagemFixa());
            }

            if (configuracao.getCustoEmbalagemFixa() != null) {
                atual.setCustoEmbalagemFixa(configuracao.getCustoEmbalagemFixa());
            }

            if (configuracao.getFormularioAvancado() != null) {
                atual.setFormularioAvancado(configuracao.getFormularioAvancado());
            }

            if (configuracao.getMaoObraAtivada() != null) {
                atual.setMaoObraAtivada(configuracao.getMaoObraAtivada());
            }
            atual.setValorMaoObraHora(configuracao.getValorMaoObraHora());
            atual.setMaoObraTempoFixo(Boolean.TRUE.equals(configuracao.getMaoObraTempoFixo()));
            atual.setTempoMaoObraFixoMinutos(configuracao.getTempoMaoObraFixoMinutos() != null
                    ? configuracao.getTempoMaoObraFixoMinutos() : 0L);

            if (configuracao.getDepreciacaoAtivada() != null) {
                atual.setDepreciacaoAtivada(configuracao.getDepreciacaoAtivada());
            }
            if (configuracao.getTaxaFalhaAtivada() != null) {
                atual.setTaxaFalhaAtivada(configuracao.getTaxaFalhaAtivada());
            }
            atual.setPercentualFalha(configuracao.getPercentualFalha());
            atual.setPurgaGramasPorImpressao(configuracao.getPurgaGramasPorImpressao() != null
                    ? configuracao.getPurgaGramasPorImpressao() : BigDecimal.ZERO);

            Configuracao salva = configuracaoRepository.save(atual);
            configuracoesPorUsuario.put(usuarioLogadoService.buscarIdUsuarioAtual(), salva);
            return salva;
        });
    }

    @Override
    public Page<Configuracao> fetchAll(Pageable pageable) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return configuracaoRepository.findAllByUsuario_Id(usuarioId, pageable);
    }

    @Override
    public Optional<Configuracao> fetchById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return configuracaoRepository.findByIdAndUsuario_Id(id, usuarioId);
    }

    @Override
    public boolean existsById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        return configuracaoRepository.existsByIdAndUsuario_Id(id, usuarioId);
    }

    @Override
    public Optional<Configuracao> buscarConfiguracao() {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        Configuracao emCache = configuracoesPorUsuario.get(usuarioId);
        if (emCache != null) return Optional.of(emCache);
        return configuracaoRepository.findFirstByUsuario_Id(usuarioId).map(configuracao -> {
            Configuracao anterior = configuracoesPorUsuario.putIfAbsent(usuarioId, configuracao);
            return anterior == null ? configuracao : anterior;
        });
    }

    @Override
    public Configuracao buscarOuCriarConfiguracao() {
        return buscarConfiguracao().orElseGet(() -> {
            Configuracao configuracao = new Configuracao();

            configuracao.setUsuario(usuarioLogadoService.buscarUsuarioAtual());
            configuracao.setMarkupPercentual(BigDecimal.ZERO);
            configuracao.setMarkupFixo(false);
            configuracao.setTarifaEnergiaKwh(BigDecimal.ZERO);
            configuracao.setEmbalagemFixa(false);
            configuracao.setCustoEmbalagemFixa(BigDecimal.ZERO);
            configuracao.setFormularioAvancado(false);
            configuracao.setMaoObraAtivada(false);
            configuracao.setMaoObraTempoFixo(false);
            configuracao.setTempoMaoObraFixoMinutos(0L);
            configuracao.setDepreciacaoAtivada(false);
            configuracao.setTaxaFalhaAtivada(false);
            configuracao.setPercentualFalha(new BigDecimal("5.00"));
            configuracao.setPurgaGramasPorImpressao(BigDecimal.ZERO);

            Configuracao salva = configuracaoRepository.save(configuracao);
            configuracoesPorUsuario.put(usuarioLogadoService.buscarIdUsuarioAtual(), salva);
            return salva;
        });
    }
}
