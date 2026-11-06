package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.CalculoHistorico;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;
import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.FilamentoHistoricoUsoResponse;
import java.util.List;

public interface CalculoHistoricoService {
    CalculoHistorico create(CalculoHistorico calculoHistorico);
    Page<CalculoHistorico> fetchAll(Pageable pageable);
    Optional<CalculoHistorico> fetchById(UUID id);
    boolean existsById(UUID id);
    long contarPorFilamento(UUID filamentoId);
    List<FilamentoHistoricoUsoResponse> listarUsosPorFilamento(UUID filamentoId);
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
