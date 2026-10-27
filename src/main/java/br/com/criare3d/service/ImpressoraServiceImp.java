package br.com.criare3d.service;

import br.com.criare3d.models.Impressora;
import br.com.criare3d.repository.ImpressoraRepository;
import br.com.criare3d.service.interfaces.ImpressoraService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;
import java.util.UUID;

@Service
@Transactional( propagation = Propagation.REQUIRED)
public class ImpressoraServiceImp implements ImpressoraService {
    @Autowired
    private ImpressoraRepository impressoraRepository;

    @Override
    public Impressora create(Impressora impressora) {
        return this.impressoraRepository.save(impressora);
    }

    @Override
    public Optional<Impressora> update(UUID id, Impressora patch) {
        return impressoraRepository.findById(id)
                .map(existing -> {
                    if(patch.getNome() != null)
                        existing.setNome(patch.getNome());

                    if(patch.getModelo() != null)
                        existing.setModelo(patch.getModelo());

                    if(patch.getPotenciaWatts() != null)
                        existing.setPotenciaWatts(patch.getPotenciaWatts());

                    return impressoraRepository.save(existing);
                });
    }

    @Override
    public void delete(UUID id){
        impressoraRepository.findById(id).ifPresent(entidade -> {
            impressoraRepository.delete(entidade);
        });
    }

    @Override
    public Page<Impressora> fetchAll(Pageable pageable) {
        return this.impressoraRepository.findAll(pageable);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<Impressora> fetchById(UUID id) {
        return this.impressoraRepository.findById(id);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean existsById(UUID id){
        return this.impressoraRepository.existsById(id);
    }
}
