package br.com.criare3d.service;

import br.com.criare3d.models.Filamento;
import br.com.criare3d.models.Impressora;
import br.com.criare3d.repository.FilamentoRepository;
import br.com.criare3d.service.interfaces.FilamentoService;
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
@Transactional( propagation = Propagation.REQUIRED)
public class FilamentoServiceImp implements FilamentoService {
    @Autowired
    private FilamentoRepository filamentoRepository;

    @Override
    public Filamento create(Filamento filamento) {
        return filamentoRepository.save(filamento);
    }

    @Override
    public Optional<Filamento> update(UUID id, Filamento patch) {
        return filamentoRepository.findById(id)
                .map(existing -> {
                   if(patch.getNome() != null)
                       existing.setNome(patch.getNome());

                   if(patch.getCor() != null)
                       existing.setCor(patch.getCor());

                   if(patch.getCodigoCor() != null)
                       existing.setCodigoCor(patch.getCodigoCor());

                   if(patch.getMarca() != null)
                       existing.setMarca(patch.getMarca());

                   if(patch.getPesoRoloGramas() != null)
                       existing.setPesoRoloGramas(patch.getPesoRoloGramas());

                   if(patch.getPrecoRolo() != null)
                       existing.setPrecoRolo(patch.getPrecoRolo());

                    return filamentoRepository.save(existing);
                });
    }

    @Override
    public void delete(UUID id){
        filamentoRepository.findById(id).ifPresent(entidade -> {
            filamentoRepository.delete(entidade);
        });
    }

    @Override
    public Page<Filamento> fetchAll(Pageable pageable) {
        return filamentoRepository.findAll(pageable);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<Filamento> fetchById(UUID id) {
        return filamentoRepository.findById(id);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean existsById(UUID id){
        return this.filamentoRepository.existsById(id);
    }
}
