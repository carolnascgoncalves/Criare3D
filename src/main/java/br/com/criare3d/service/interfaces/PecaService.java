package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.Peca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface PecaService {
    Peca create(Peca peca);
    Page<Peca> fetchAll(Pageable pageable);
    Optional<Peca> fetchById(UUID id);
    void delete(UUID id);
    boolean existsById(UUID id);
    Optional<Peca> update(UUID id, Peca peca);
}
