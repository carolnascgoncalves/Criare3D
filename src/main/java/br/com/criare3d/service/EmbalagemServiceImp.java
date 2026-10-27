package br.com.criare3d.service;

import br.com.criare3d.models.Embalagem;
import br.com.criare3d.repository.EmbalagemRepository;
import br.com.criare3d.service.interfaces.EmbalagemService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(propagation = Propagation.REQUIRED)
public class EmbalagemServiceImp implements EmbalagemService {
    @Autowired
    private EmbalagemRepository embalagemRepository;

    @Override
    public Embalagem create(Embalagem embalagem) {
        return embalagemRepository.save(embalagem);
    }

    @Override
    public Optional<Embalagem> update(UUID id, Embalagem patch) {
        return embalagemRepository.findById(id)
                .map(existing -> {
                    if(patch.getNome() != null)
                        existing.setNome(patch.getNome());

                    if(patch.getCustoUnitario() != null)
                        existing.setCustoUnitario(patch.getCustoUnitario());

                    return embalagemRepository.save(existing);
                });
    }

    @Override
    public void delete(UUID id){
        embalagemRepository.findById(id).ifPresent(entidade -> {
            embalagemRepository.delete(entidade);
        });
    }

    @Override
    public Page<Embalagem> fetchAll(Pageable pageable) {
        return embalagemRepository.findAll(pageable);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<Embalagem> fetchById(UUID id) {
        return embalagemRepository.findById(id);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean existsById(UUID id){
        return this.embalagemRepository.existsById(id);
    }
}
