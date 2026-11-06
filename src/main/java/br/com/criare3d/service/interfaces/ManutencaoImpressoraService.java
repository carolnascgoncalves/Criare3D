package br.com.criare3d.service.interfaces;

import br.com.criare3d.dto.ManutencaoImpressoraRequest;
import br.com.criare3d.models.Impressora;
import br.com.criare3d.models.ManutencaoImpressora;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManutencaoImpressoraService {
    List<ManutencaoImpressora> listar(UUID impressoraId);
    ManutencaoImpressora registrar(UUID impressoraId, ManutencaoImpressoraRequest request);
    void excluir(UUID impressoraId, UUID manutencaoId);
    long contar(UUID impressoraId);
    BigDecimal totalGasto(UUID impressoraId);
    long minutosImpressos(UUID impressoraId);
    BigDecimal custoPorHora(UUID impressoraId, BigDecimal fallbackManual);
}
