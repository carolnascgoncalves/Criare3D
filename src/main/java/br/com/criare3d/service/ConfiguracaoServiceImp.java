package br.com.criare3d.service;

import br.com.criare3d.models.Configuracao;
import br.com.criare3d.repository.ConfiguracaoRepository;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ConfiguracaoServiceImp implements ConfiguracaoService {

    private final ConfiguracaoRepository configuracaoRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final Cache<UUID, Optional<Configuracao>> configuracoesPorTenant = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

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
        invalidarCacheDepoisDoCommit(usuarioLogadoService.buscarTenantIdAtual());
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

            if (configuracao.getMarkupOpcao1() != null) atual.setMarkupOpcao1(configuracao.getMarkupOpcao1());
            if (configuracao.getMarkupOpcao2() != null) atual.setMarkupOpcao2(configuracao.getMarkupOpcao2());
            if (configuracao.getMarkupOpcao3() != null) atual.setMarkupOpcao3(configuracao.getMarkupOpcao3());

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
            if (configuracao.getValorMaoObraHora() != null) {
                atual.setValorMaoObraHora(configuracao.getValorMaoObraHora());
            }
            atual.setMaoObraTempoFixo(Boolean.TRUE.equals(configuracao.getMaoObraTempoFixo()));
            if (configuracao.getTempoMaoObraFixoMinutos() != null) {
                atual.setTempoMaoObraFixoMinutos(configuracao.getTempoMaoObraFixoMinutos());
            }

            if (configuracao.getDepreciacaoAtivada() != null) {
                atual.setDepreciacaoAtivada(configuracao.getDepreciacaoAtivada());
            }
            if (configuracao.getTaxaFalhaAtivada() != null) {
                atual.setTaxaFalhaAtivada(configuracao.getTaxaFalhaAtivada());
            }
            if (configuracao.getPercentualFalha() != null) {
                atual.setPercentualFalha(configuracao.getPercentualFalha());
            }
            if (configuracao.getPurgaGramasPorImpressao() != null) {
                atual.setPurgaGramasPorImpressao(configuracao.getPurgaGramasPorImpressao());
            }

            Configuracao salva = configuracaoRepository.save(atual);
            invalidarCacheDepoisDoCommit(usuarioLogadoService.buscarTenantIdAtual());
            return salva;
        });
    }

    @Override
    public Page<Configuracao> fetchAll(Pageable pageable) {
        return configuracaoRepository.findAll(pageable);
    }

    @Override
    public Optional<Configuracao> fetchById(UUID id) {
        return configuracaoRepository.findById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return configuracaoRepository.existsById(id);
    }

    @Override
    public Optional<Configuracao> buscarConfiguracao() {
        UUID tenantId = usuarioLogadoService.buscarTenantIdAtual();
        if (tenantId == null) return Optional.empty();
        return configuracoesPorTenant.get(tenantId,
                ignorado -> configuracaoRepository.findFirstByTenantId(tenantId));
    }

    @Override
    public Configuracao buscarOuCriarConfiguracao() {
        if (usuarioLogadoService.buscarTenantIdAtual() == null) {
            throw new IllegalStateException("Configurações estão disponíveis somente para contas de cliente.");
        }
        return buscarConfiguracao().orElseGet(() -> {
            Configuracao configuracao = ConfiguracaoPadrao.criar();
            configuracao.setUsuario(usuarioLogadoService.buscarUsuarioAtual());
            Configuracao salva = configuracaoRepository.save(configuracao);
            UUID tenantId = usuarioLogadoService.buscarTenantIdAtual();
            invalidarCacheDepoisDoCommit(tenantId);
            return salva;
        });
    }

    private void invalidarCache(UUID tenantId) {
        if (tenantId != null) configuracoesPorTenant.invalidate(tenantId);
    }

    private void invalidarCacheDepoisDoCommit(UUID tenantId) {
        if (tenantId == null) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            invalidarCache(tenantId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                invalidarCache(tenantId);
            }
        });
    }
}
