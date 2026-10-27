package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.Filamento;
import br.com.criare3d.models.Impressora;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface FilamentoService {
    Filamento create(Filamento filamento);
    Optional<Filamento> update(UUID id, Filamento filamento);
    void delete(UUID id);
    Page<Filamento> fetchAll(Pageable pageable);
    Optional<Filamento> fetchById(UUID id);
    boolean existsById(UUID id);
}
