package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.Filamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FilamentoService {
    Filamento create(Filamento filamento);
    Optional<Filamento> update(UUID id, Filamento filamento);
    void delete(UUID id);
    List<UUID> deleteUnused(Collection<UUID> ids);
    Page<Filamento> fetchAll(Pageable pageable);
    Page<Filamento> fetchAll(Pageable pageable, String busca);
    long contar();
    Optional<Filamento> fetchById(UUID id);
    List<Filamento> fetchByIds(Collection<UUID> ids);
    List<Filamento> fetchRecentes(int limite);
    Optional<Filamento> fetchEstoqueCritico();
    boolean existsById(UUID id);
}
