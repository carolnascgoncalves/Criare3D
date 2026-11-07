package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.Embalagem;
import br.com.criare3d.models.Impressora;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;

public interface EmbalagemService {
    Embalagem create(Embalagem impressora);
    Optional<Embalagem> update(UUID id, Embalagem embalagem);
    void delete(UUID id);
    List<UUID> deleteUnused(Collection<UUID> ids);
    Page<Embalagem> fetchAll(Pageable pageable);
    long contar();
    Optional<Embalagem> fetchById(UUID id);
    boolean existsById(UUID id);
}
