package br.com.criare3d.service;

import br.com.criare3d.models.Embalagem;
import br.com.criare3d.repository.EmbalagemRepository;
import br.com.criare3d.service.interfaces.EmbalagemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EmbalagemServiceImp implements EmbalagemService {

    @Autowired
    private EmbalagemRepository embalagemRepository;

    @Autowired
    private UsuarioLogadoService usuarioLogadoService;

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
    public Page<Embalagem> fetchAll(Pageable pageable) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return embalagemRepository.findAllByUsuario_Id(usuarioId, pageable);
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