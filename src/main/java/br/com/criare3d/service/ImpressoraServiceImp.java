package br.com.criare3d.service;

import br.com.criare3d.models.Impressora;
import br.com.criare3d.repository.ImpressoraRepository;
import br.com.criare3d.service.interfaces.ImpressoraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ImpressoraServiceImp implements ImpressoraService {

    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Autowired
    private UsuarioLogadoService usuarioLogadoService;

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
            // Mantém o campo legado no banco para não apagar dados; ele não participa mais dos cálculos.

            return impressoraRepository.save(existing);
        });
    }

    @Override
    public void delete(UUID id) {
        fetchById(id).ifPresent(impressoraRepository::delete);
    }

    @Override
    public Page<Impressora> fetchAll(Pageable pageable) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return impressoraRepository.findAllByUsuario_Id(usuarioId, pageable);
    }

    @Override
    public Optional<Impressora> fetchById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return impressoraRepository.findByIdAndUsuario_Id(id, usuarioId);
    }

    @Override
    public boolean existsById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return impressoraRepository.existsByIdAndUsuario_Id(id, usuarioId);
    }
}
