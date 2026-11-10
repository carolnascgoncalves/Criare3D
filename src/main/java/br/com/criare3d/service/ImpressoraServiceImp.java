package br.com.criare3d.service;

import br.com.criare3d.models.Impressora;
import br.com.criare3d.repository.ImpressoraRepository;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.repository.ManutencaoImpressoraRepository;
import br.com.criare3d.service.interfaces.ImpressoraService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class ImpressoraServiceImp implements ImpressoraService {

    private final ImpressoraRepository impressoraRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final CalculoHistoricoRepository calculoHistoricoRepository;
    private final ManutencaoImpressoraRepository manutencaoImpressoraRepository;

    public ImpressoraServiceImp(ImpressoraRepository impressoraRepository,
                                UsuarioLogadoService usuarioLogadoService,
                                CalculoHistoricoRepository calculoHistoricoRepository,
                                ManutencaoImpressoraRepository manutencaoImpressoraRepository) {
        this.impressoraRepository = impressoraRepository;
        this.usuarioLogadoService = usuarioLogadoService;
        this.calculoHistoricoRepository = calculoHistoricoRepository;
        this.manutencaoImpressoraRepository = manutencaoImpressoraRepository;
    }

    @Override
    public Impressora create(Impressora impressora) {
        impressora.setUsuario(usuarioLogadoService.buscarUsuarioAtual());
        return impressoraRepository.save(impressora);
    }

    @Override
    public Optional<Impressora> update(UUID id, Impressora patch) {
        return fetchById(id).map(existing -> {
            if (patch.getNome() != null)
                existing.setNome(patch.getNome());

            if (patch.getModelo() != null)
                existing.setModelo(patch.getModelo());

            if (patch.getPotenciaWatts() != null)
                existing.setPotenciaWatts(patch.getPotenciaWatts());

            existing.setValorCompra(patch.getValorCompra());
            existing.setVidaUtilHoras(patch.getVidaUtilHoras());
            existing.setDepreciacaoPorHora(patch.getDepreciacaoPorHora());
            existing.setDepreciacaoOrigem(patch.getDepreciacaoOrigem());
            existing.setAnunciosNovos(patch.getAnunciosNovos());
            existing.setAnunciosUsados(patch.getAnunciosUsados());
            existing.setDepreciacaoManual(Boolean.TRUE.equals(patch.getDepreciacaoManual()));
            existing.setHorasMensaisUso(patch.getHorasMensaisUso());
            existing.setHorasMensaisManual(Boolean.TRUE.equals(patch.getHorasMensaisManual()));
            existing.setTempoAquecimentoMinutos(patch.getTempoAquecimentoMinutos() == null
                    ? 0 : patch.getTempoAquecimentoMinutos());
            // A taxa direta tem prioridade; preço de compra e horas continuam como fallback legado.

            return impressoraRepository.save(existing);
        });
    }

    @Override
    public void delete(UUID id) {
        fetchById(id).ifPresent(impressoraRepository::delete);
    }

    @Override
    public List<UUID> deleteUnused(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<UUID> solicitados = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (solicitados.isEmpty()) return List.of();
        List<UUID> existentes = impressoraRepository.findAllByIdIn(solicitados)
                .stream().map(Impressora::getId).toList();
        if (existentes.isEmpty()) return List.of();
        Set<UUID> bloqueadas = new java.util.HashSet<>(calculoHistoricoRepository.buscarIdsImpressoraComHistorico(existentes));
        bloqueadas.addAll(manutencaoImpressoraRepository.buscarIdsImpressoraComManutencao(existentes));
        List<UUID> removiveis = existentes.stream().filter(id -> !bloqueadas.contains(id)).toList();
        if (!removiveis.isEmpty()) impressoraRepository.deleteAllByIdInBatch(removiveis);
        return existentes.stream().filter(bloqueadas::contains).toList();
    }

    @Override
    public Page<Impressora> fetchAll(Pageable pageable) {
        return impressoraRepository.findAll(pageable);
    }

    @Override
    public long contar() {
        return impressoraRepository.count();
    }

    @Override
    public Optional<Impressora> fetchById(UUID id) {
        return impressoraRepository.findById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return impressoraRepository.existsById(id);
    }
}
