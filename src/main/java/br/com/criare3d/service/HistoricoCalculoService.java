package br.com.criare3d.service;

import br.com.criare3d.dto.AjusteEstoqueCalculoResponse;
import br.com.criare3d.dto.CalculoRecenteResponse;
import br.com.criare3d.dto.FilamentoHistoricoUsoResponse;
import br.com.criare3d.dto.HistoricoExportacaoResponse;
import br.com.criare3d.models.*;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.repository.ItemCalculoHistoricoRepository;
import br.com.criare3d.repository.VendaCalculoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Service
@Transactional
public class HistoricoCalculoService {
    private final CalculoHistoricoRepository calculoHistoricoRepository;
    private final ItemCalculoHistoricoRepository itemCalculoHistoricoRepository;
    private final VendaCalculoRepository vendaCalculoRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final FotoCalculoService fotos;
    @org.springframework.beans.factory.annotation.Autowired private br.com.criare3d.repository.FalhaProducaoRepository falhaProducaoRepository;
    @org.springframework.beans.factory.annotation.Autowired private br.com.criare3d.repository.PlanejamentoProducaoRepository planejamentoProducaoRepository;
    @org.springframework.beans.factory.annotation.Autowired private br.com.criare3d.repository.CustoPersonalizadoRepository custoPersonalizadoRepository;
    public HistoricoCalculoService(CalculoHistoricoRepository calculoHistoricoRepository, ItemCalculoHistoricoRepository itemCalculoHistoricoRepository, VendaCalculoRepository vendaCalculoRepository, UsuarioLogadoService usuarioLogadoService, FotoCalculoService fotos) {
        this.calculoHistoricoRepository = calculoHistoricoRepository;
        this.itemCalculoHistoricoRepository = itemCalculoHistoricoRepository;
        this.vendaCalculoRepository = vendaCalculoRepository;
        this.usuarioLogadoService = usuarioLogadoService;
        this.fotos = fotos;
    }

    public long contarPorFilamento(UUID filamentoId) {
        return calculoHistoricoRepository.contarUsosDosItensPorFilamento(filamentoId)
                + (falhaProducaoRepository == null ? 0 : falhaProducaoRepository.countByFilamento_Id(filamentoId));
    }

    public List<FilamentoHistoricoUsoResponse> listarUsosPorFilamento(UUID filamentoId) {
        return calculoHistoricoRepository.buscarUsosDoFilamento(filamentoId);
    }

    @Transactional(readOnly = true)
    public Set<UUID> buscarFilamentosComHistoricoProduzido(Collection<UUID> filamentoIds) {
        if (filamentoIds == null || filamentoIds.isEmpty()) return Set.of();
        return Set.copyOf(calculoHistoricoRepository.buscarIdsFilamentoComHistoricoProduzido(
                List.copyOf(filamentoIds)));
    }

    public long contarPorEmbalagem(UUID embalagemId) {
        return calculoHistoricoRepository.countByEmbalagem_Id(embalagemId);
    }

    public long contarPorImpressora(UUID impressoraId) {
        return calculoHistoricoRepository.countByImpressora_Id(impressoraId);
    }

    public long contarPorPeca(UUID pecaId) {
        return calculoHistoricoRepository.countByPeca_Id(pecaId);
    }

    public CalculoHistorico create(CalculoHistorico calculoHistorico) {
        calculoHistorico.setUsuario(usuarioLogadoService.buscarUsuarioAtual());

        return calculoHistoricoRepository.save(calculoHistorico);
    }

    public CalculoHistorico create(CalculoHistorico calculoHistorico, String tipoFoto, byte[] bytesFoto) {
        CalculoHistorico salvo = create(calculoHistorico);
        fotos.salvar(salvo.getId(), tipoFoto, bytesFoto);
        return salvo;
    }

    @Transactional(readOnly = true)
    public List<HistoricoExportacaoResponse> exportarHistorico() {
        return calculoHistoricoRepository.buscarHistoricoParaExportacao();
    }

    public void delete(UUID id) {
        if (id != null) deleteAll(List.of(id));
    }

    public void deleteAll(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return;

        List<UUID> solicitados = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (solicitados.isEmpty()) return;
        for (UUID id : solicitados.stream().sorted().toList()) {
            var h = calculoHistoricoRepository.bloquearPorId(id).orElse(null);
            if (h != null && (Boolean.TRUE.equals(h.getProduzido()) || vendaCalculoRepository.existsByCalculo_Id(id)
                    || (falhaProducaoRepository != null && !falhaProducaoRepository.findByCalculo_IdOrderByDataRegistroDesc(id).isEmpty())
                    || (custoPersonalizadoRepository != null && custoPersonalizadoRepository.existsByCalculo_Id(id))
                    || (planejamentoProducaoRepository != null && planejamentoProducaoRepository.existsByCalculo_Id(id)))) {
                throw new IllegalArgumentException("Este cálculo possui produção, vendas, perdas ou planejamento. Preserve o histórico e gerencie as operações em Vendas e estoque.");
            }
        }

        List<AjusteEstoqueCalculoResponse> dados =
                calculoHistoricoRepository.buscarDadosParaExclusaoEmLote(solicitados);
        if (dados.isEmpty()) return;

        List<UUID> encontrados = dados.stream()
                .map(AjusteEstoqueCalculoResponse::calculoId).distinct().toList();
        itemCalculoHistoricoRepository.excluirItensDosCalculos(encontrados);
        calculoHistoricoRepository.deleteAllByIdInBatch(encontrados);
    }

    @Transactional(readOnly = true)
    public java.util.List<CalculoRecenteResponse> fetchRecentes(int limite) {
        if (limite <= 0) return java.util.List.of();
        return calculoHistoricoRepository.buscarCalculosRecentes(
                PageRequest.of(0, limite)
        );
    }

    public Optional<CalculoHistorico> fetchById(UUID id) {
        return calculoHistoricoRepository.buscarPorIdComItens(id);
    }

    public boolean existsById(UUID id) {
        return calculoHistoricoRepository.existsById(id);
    }
}
