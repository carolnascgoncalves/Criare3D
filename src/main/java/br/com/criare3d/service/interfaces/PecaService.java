package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.Peca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;

public interface PecaService {
    Peca create(Peca peca);
    Page<Peca> fetchAll(Pageable pageable);
    long contar();
    Optional<Peca> fetchById(UUID id);
    void delete(UUID id);
    List<UUID> deleteUnused(Collection<UUID> ids);
    boolean existsById(UUID id);
    Optional<Peca> update(UUID id, Peca peca);
}
