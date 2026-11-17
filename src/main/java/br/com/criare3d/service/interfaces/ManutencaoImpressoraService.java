package br.com.criare3d.service.interfaces;

import br.com.criare3d.dto.ManutencaoImpressoraRequest;
import br.com.criare3d.dto.ResumoManutencaoImpressora;
import br.com.criare3d.models.ManutencaoImpressora;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ManutencaoImpressoraService {
    Optional<ManutencaoImpressora> registrar(UUID impressoraId, ManutencaoImpressoraRequest request);

    Optional<ResumoManutencaoImpressora> resumo(UUID impressoraId);

    void excluir(UUID impressoraId, UUID manutencaoId);

    long contar(UUID impressoraId);

    BigDecimal custoPorHora(UUID impressoraId);
}
