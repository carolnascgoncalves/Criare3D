package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.CalculoHistorico;


import java.math.BigDecimal;
import java.util.Optional;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.FilamentoHistoricoUsoResponse;
import br.com.criare3d.dto.CalculoRecenteResponse;
import java.util.List;

public interface CalculoHistoricoService {
    CalculoHistorico create(CalculoHistorico calculoHistorico);
    List<CalculoRecenteResponse> fetchRecentes(int limite);
    long contarProduzidos();
    long contarVendidos();
    Optional<CalculoHistorico> fetchById(UUID id);
    boolean existsById(UUID id);
    long contarPorFilamento(UUID filamentoId);
    List<FilamentoHistoricoUsoResponse> listarUsosPorFilamento(UUID filamentoId);
    Set<UUID> buscarFilamentosComHistoricoProduzido(Collection<UUID> filamentoIds);
    long contarPorEmbalagem(UUID embalagemId);
    long contarPorImpressora(UUID impressoraId);
    long contarPorPeca(UUID pecaId);
    void delete(UUID id);
    void deleteAll(Collection<UUID> ids);
    String atualizarProduzido(UUID id, boolean produzido);
    String atualizarVendido(UUID id, boolean vendido);
    CalculoHistorico calcular(CalculoRequest request);
    CalculoHistorico calcularComPrecoFixo(CalculoRequest request, BigDecimal precoVenda);
}
