package br.com.criare3d.service;

import br.com.criare3d.models.Embalagem;
import br.com.criare3d.repository.EmbalagemRepository;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import br.com.criare3d.service.interfaces.EmbalagemService;
import org.springframework.beans.factory.annotation.Autowired;
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
public class EmbalagemServiceImp implements EmbalagemService {

    @Autowired
    private EmbalagemRepository embalagemRepository;

    @Autowired
    private UsuarioLogadoService usuarioLogadoService;

    @Autowired
    private CalculoHistoricoRepository calculoHistoricoRepository;

    @Override
    public Embalagem create(Embalagem embalagem) {
        embalagem.setUsuario(usuarioLogadoService.buscarUsuarioAtual());
        return embalagemRepository.save(embalagem);
    }

    @Override
    public Optional<Embalagem> update(UUID id, Embalagem patch) {
        return fetchById(id).map(existing -> {
            if (patch.getNome() != null)
                existing.setNome(patch.getNome());

            if (patch.getCustoUnitario() != null)
                existing.setCustoUnitario(patch.getCustoUnitario());

            return embalagemRepository.save(existing);
        });
    }

    @Override
    public void delete(UUID id) {
        fetchById(id).ifPresent(embalagemRepository::delete);
    }

    @Override
    public List<UUID> deleteUnused(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        List<UUID> solicitados = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (solicitados.isEmpty()) return List.of();
        List<UUID> existentes = embalagemRepository.findAllByIdInAndUsuario_Id(solicitados, usuarioId)
                .stream().map(Embalagem::getId).toList();
        if (existentes.isEmpty()) return List.of();
        Set<UUID> bloqueadas = Set.copyOf(calculoHistoricoRepository.buscarIdsEmbalagemComHistorico(usuarioId, existentes));
        List<UUID> removiveis = existentes.stream().filter(id -> !bloqueadas.contains(id)).toList();
        if (!removiveis.isEmpty()) embalagemRepository.deleteAllByIdInBatch(removiveis);
        return existentes.stream().filter(bloqueadas::contains).toList();
    }

    @Override
    public Page<Embalagem> fetchAll(Pageable pageable) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return embalagemRepository.findAllByUsuario_Id(usuarioId, pageable);
    }

    @Override
    public long contar() {
        return embalagemRepository.countByUsuario_Id(usuarioLogadoService.buscarIdUsuarioAtual());
    }

    @Override
    public Optional<Embalagem> fetchById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return embalagemRepository.findByIdAndUsuario_Id(id, usuarioId);
    }

    @Override
    public boolean existsById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return embalagemRepository.existsByIdAndUsuario_Id(id, usuarioId);
    }
}
