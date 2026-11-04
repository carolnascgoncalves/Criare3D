package br.com.criare3d.service;

import br.com.criare3d.models.Configuracao;
import br.com.criare3d.repository.ConfiguracaoRepository;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ConfiguracaoServiceImp implements ConfiguracaoService {

    @Autowired
    private ConfiguracaoRepository configuracaoRepository;

    @Autowired
    private UsuarioLogadoService usuarioLogadoService;

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

        return configuracaoRepository.save(configuracao);
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

            return configuracaoRepository.save(atual);
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

        return configuracaoRepository.findFirstByUsuario_Id(usuarioId);
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

            return configuracaoRepository.save(configuracao);
        });
    }
}