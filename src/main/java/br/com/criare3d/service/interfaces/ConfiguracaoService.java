package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.Configuracao;
import br.com.criare3d.models.Impressora;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ConfiguracaoService {
    Configuracao create(Configuracao configuracao);
    Optional<Configuracao> update(UUID id, Configuracao configuracao);
    Page<Configuracao> fetchAll(Pageable pageable);
    Optional<Configuracao> fetchById(UUID id);
    boolean existsById(UUID id);
    Optional<Configuracao> buscarConfiguracao();
    Configuracao buscarOuCriarConfiguracao();
}
