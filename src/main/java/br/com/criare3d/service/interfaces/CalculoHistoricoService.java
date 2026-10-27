package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.Peca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CalculoHistoricoService {
    CalculoHistorico create(CalculoHistorico calculoHistorico);
    Page<CalculoHistorico> fetchAll(Pageable pageable);
    Optional<CalculoHistorico> fetchById(UUID id);
    boolean existsById(UUID id);
    long contarPorFilamento(UUID filamentoId);
    long contarPorEmbalagem(UUID embalagemId);
    long contarPorImpressora(UUID impressoraId);
    long contarPorPeca(UUID pecaId);
    void delete(UUID id);
}
