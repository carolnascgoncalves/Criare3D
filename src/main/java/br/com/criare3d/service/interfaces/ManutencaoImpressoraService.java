package br.com.criare3d.service.interfaces;

import br.com.criare3d.dto.ManutencaoImpressoraRequest;
import br.com.criare3d.models.Impressora;
import br.com.criare3d.models.ManutencaoImpressora;
import br.com.criare3d.dto.ResumoManutencaoImpressora;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface ManutencaoImpressoraService {
    Optional<ManutencaoImpressora> registrar(UUID impressoraId, ManutencaoImpressoraRequest request);
    Optional<ResumoManutencaoImpressora> resumo(UUID impressoraId);
    void excluir(UUID impressoraId, UUID manutencaoId);
    long contar(UUID impressoraId);
    BigDecimal custoPorHora(UUID impressoraId);
}
