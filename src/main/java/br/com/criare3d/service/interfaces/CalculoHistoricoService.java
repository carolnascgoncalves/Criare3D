package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.CalculoHistorico;
import br.com.criare3d.models.CalculoFoto;
import br.com.criare3d.dto.FotoConteudo;


import java.math.BigDecimal;
import java.util.Optional;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import br.com.criare3d.dto.CalculoRequest;
import br.com.criare3d.dto.HistoricoExportacaoResponse;
import br.com.criare3d.dto.FilamentoHistoricoUsoResponse;
import br.com.criare3d.dto.CalculoRecenteResponse;
import java.util.List;

public interface CalculoHistoricoService {
    CalculoHistorico create(CalculoHistorico calculoHistorico);
    CalculoHistorico create(CalculoHistorico calculoHistorico, String tipoFoto, byte[] bytesFoto);
    Optional<CalculoFoto> buscarFoto(UUID calculoId);
    Optional<FotoConteudo> buscarConteudoFoto(UUID calculoId, boolean miniatura);
    List<CalculoRecenteResponse> fetchRecentes(int limite);
    long contarProduzidos();
    long contarVendidos();
    long contarUnidadesParaProduzir();
    BigDecimal somarCustoUnidadesEmEstoque();
    long contarUnidadesVendidasEntre(java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
    long contarLotesVendidosEntre(java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
    BigDecimal somarLucroVendidoEntre(java.time.LocalDateTime inicio, java.time.LocalDateTime fim);
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
    String atualizarStatusEmLote(Collection<UUID> ids, String acao);
    String registrarVendasEmLote(Collection<UUID> ids, Integer unidadesPorLote);
    String registrarVenda(UUID id, Integer unidades);
    String cancelarVendas(UUID id);
    CalculoHistorico calcular(CalculoRequest request);
    CalculoHistorico calcularComPrecoFixo(CalculoRequest request, BigDecimal precoVenda);
    List<HistoricoExportacaoResponse> exportarHistorico();
}
