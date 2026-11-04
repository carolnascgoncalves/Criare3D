package br.com.criare3d.service;

import br.com.criare3d.models.Peca;
import br.com.criare3d.repository.PecaRepository;
import br.com.criare3d.service.interfaces.PecaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PecaServiceImp implements PecaService {

    @Autowired
    private PecaRepository pecaRepository;

    @Autowired
    private UsuarioLogadoService usuarioLogadoService;

    @Override
    public Peca create(Peca peca) {
        peca.setUsuario(usuarioLogadoService.buscarUsuarioAtual());
        return pecaRepository.save(peca);
    }

    @Override
    public Page<Peca> fetchAll(Pageable pageable) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return pecaRepository.findAllByUsuario_Id(usuarioId, pageable);
    }

    @Override
    public Optional<Peca> fetchById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return pecaRepository.findByIdAndUsuario_Id(id, usuarioId);
    }

    @Override
    public void delete(UUID id) {
        fetchById(id).ifPresent(pecaRepository::delete);
    }

    @Override
    public boolean existsById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return pecaRepository.existsByIdAndUsuario_Id(id, usuarioId);
    }

    @Override
    public Optional<Peca> update(UUID id, Peca patch) {
        return fetchById(id).map(existing -> {
            if (patch.getNome() != null)
                existing.setNome(patch.getNome());

            if (patch.getQuantidadeFilamentoGramas() != null)
                existing.setQuantidadeFilamentoGramas(patch.getQuantidadeFilamentoGramas());

            if (patch.getTempoImpressaoMinutos() != null)
                existing.setTempoImpressaoMinutos(patch.getTempoImpressaoMinutos());

            return pecaRepository.save(existing);
        });
    }
}