package br.com.criare3d.service;

import br.com.criare3d.models.Peca;
import br.com.criare3d.repository.PecaRepository;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.service.interfaces.PecaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PecaServiceImp implements PecaService {

    private final PecaRepository pecaRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final CalculoHistoricoRepository calculoHistoricoRepository;

    public PecaServiceImp(PecaRepository pecaRepository,
                          UsuarioLogadoService usuarioLogadoService,
                          CalculoHistoricoRepository calculoHistoricoRepository) {
        this.pecaRepository = pecaRepository;
        this.usuarioLogadoService = usuarioLogadoService;
        this.calculoHistoricoRepository = calculoHistoricoRepository;
    }

    @Override
    public Peca create(Peca peca) {
        peca.setUsuario(usuarioLogadoService.buscarUsuarioAtual());
        return pecaRepository.save(peca);
    }

    @Override
    public Page<Peca> fetchAll(Pageable pageable) {
        return pecaRepository.findAll(pageable);
    }

    @Override
    public long contar() {
        return pecaRepository.count();
    }

    @Override
    public Optional<Peca> fetchById(UUID id) {
        return pecaRepository.findById(id);
    }

    @Override
    public void delete(UUID id) {
        fetchById(id).ifPresent(pecaRepository::delete);
    }

    @Override
    public List<UUID> deleteUnused(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<UUID> solicitados = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (solicitados.isEmpty()) return List.of();
        List<UUID> existentes = pecaRepository.findAllByIdIn(solicitados)
                .stream().map(Peca::getId).toList();
        if (existentes.isEmpty()) return List.of();
        Set<UUID> bloqueadas = Set.copyOf(calculoHistoricoRepository.buscarIdsPecaComHistorico(existentes));
        List<UUID> removiveis = existentes.stream().filter(id -> !bloqueadas.contains(id)).toList();
        if (!removiveis.isEmpty()) pecaRepository.deleteAllByIdInBatch(removiveis);
        return existentes.stream().filter(bloqueadas::contains).toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return pecaRepository.existsById(id);
    }

    @Override
    public Optional<Peca> update(UUID id, Peca patch) {
        return fetchById(id).map(existing -> {
            if (patch.getNome() != null)
                existing.setNome(patch.getNome());

            if (patch.getCategoria() != null)
                existing.setCategoria(patch.getCategoria());

            if (patch.getQuantidadeFilamentoGramas() != null)
                existing.setQuantidadeFilamentoGramas(patch.getQuantidadeFilamentoGramas());

            if (patch.getTempoImpressaoMinutos() != null)
                existing.setTempoImpressaoMinutos(patch.getTempoImpressaoMinutos());

            if (patch.getDadosArquivo3mf() != null && !patch.getDadosArquivo3mf().isBlank())
                existing.setDadosArquivo3mf(patch.getDadosArquivo3mf());


            return pecaRepository.save(existing);
        });
    }
}
