
package br.com.criare3d.service;

import br.com.criare3d.models.Configuracao;
import br.com.criare3d.repository.ConfiguracaoRepository;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;


@Service
@Transactional(propagation = Propagation.REQUIRED)
public class ConfiguracaoServiceImp implements ConfiguracaoService {

    @Autowired
    private ConfiguracaoRepository configuracaoRepository;

    @Override
    public Configuracao create(Configuracao configuracao) {
        if (configuracao.getEmbalagemFixa() == null) {
            configuracao.setEmbalagemFixa(false);
        }

        if (configuracao.getTarifaEnergiaKwh() == null) {
            configuracao.setTarifaEnergiaKwh(BigDecimal.ZERO);
        }

        if (configuracao.getCustoEmbalagemFixa() == null) {
            configuracao.setCustoEmbalagemFixa(BigDecimal.ZERO);
        }

        return configuracaoRepository.save(configuracao);
    }

    @Override
    public Optional<Configuracao> update(UUID id, Configuracao configuracao) {
        return configuracaoRepository.findById(id).map(atual -> {
            if (configuracao.getTarifaEnergiaKwh() != null) {
                atual.setTarifaEnergiaKwh(configuracao.getTarifaEnergiaKwh());
            }

            if (configuracao.getEmbalagemFixa() != null) {
                atual.setEmbalagemFixa(configuracao.getEmbalagemFixa());
            }

            if (configuracao.getCustoEmbalagemFixa() != null) {
                atual.setCustoEmbalagemFixa(configuracao.getCustoEmbalagemFixa());
            }

            return configuracaoRepository.save(atual);
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean existsById(UUID id) {
        return configuracaoRepository.existsById(id);
    }

    @Override
    public Optional<Configuracao> buscarConfiguracao() {
        return configuracaoRepository.findAll()
                .stream()
                .findFirst();
    }

    @Override
    public Configuracao buscarOuCriarConfiguracao() {
        return buscarConfiguracao().orElseGet(() -> {
            Configuracao configuracao = new Configuracao();

            configuracao.setTarifaEnergiaKwh(BigDecimal.ZERO);
            configuracao.setEmbalagemFixa(false);
            configuracao.setCustoEmbalagemFixa(BigDecimal.ZERO);

            return configuracaoRepository.save(configuracao);
        });
    }
}

