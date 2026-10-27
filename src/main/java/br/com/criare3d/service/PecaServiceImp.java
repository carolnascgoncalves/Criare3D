package br.com.criare3d.service;

import br.com.criare3d.models.Embalagem;
import br.com.criare3d.models.Peca;
import br.com.criare3d.repository.PecaRepository;
import br.com.criare3d.service.interfaces.PecaService;
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
public class PecaServiceImp implements PecaService {
    @Autowired
    private PecaRepository pecaRepository;

    @Override
    public Peca create(Peca peca){
        return this.pecaRepository.save(peca);
    }

    @Override
    public Page<Peca> fetchAll(Pageable pageable){
        return this.pecaRepository.findAll(pageable);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<Peca> fetchById(UUID id){
        return this.pecaRepository.findById(id);
    }

    @Override
    public void delete(UUID id){
        pecaRepository.findById(id).ifPresent(entidade -> {
            pecaRepository.delete(entidade);
        });
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean existsById(UUID id){
        return this.pecaRepository.existsById(id);
    }


    @Override
    public Optional<Peca> update(UUID id, Peca patch) {
        return pecaRepository.findById(id)
                .map(existing -> {
                    if(patch.getNome() != null)
                        existing.setNome(patch.getNome());

                    if(patch.getQuantidadeFilamentoGramas() != null)
                        existing.setQuantidadeFilamentoGramas(patch.getQuantidadeFilamentoGramas());

                    if(patch.getTempoImpressaoMinutos() != null)
                        existing.setTempoImpressaoMinutos(patch.getTempoImpressaoMinutos());
                    return pecaRepository.save(existing);
                });
    }
}
