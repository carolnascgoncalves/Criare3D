package br.com.criare3d.service;

import br.com.criare3d.models.Filamento;
import br.com.criare3d.repository.FilamentoRepository;
import br.com.criare3d.service.interfaces.FilamentoService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class FilamentoServiceImp implements FilamentoService {

    @Autowired
    private FilamentoRepository filamentoRepository;

    @Autowired
    private UsuarioLogadoService usuarioLogadoService;

    @Override
    public Filamento create(Filamento filamento) {
        if (filamento.getEstoqueGramas() == null) {
            filamento.setEstoqueGramas(filamento.getPesoRoloGramas());
        }
        filamento.setUsuario(usuarioLogadoService.buscarUsuarioAtual());
        return filamentoRepository.save(filamento);
    }

    @Override
    public Optional<Filamento> update(UUID id, Filamento patch) {
        return fetchById(id).map(existing -> {
            if (patch.getNome() != null)
                existing.setNome(patch.getNome());

            if (patch.getCor() != null)
                existing.setCor(patch.getCor());

            if (patch.getCodigoCor() != null)
                existing.setCodigoCor(patch.getCodigoCor());

            if (patch.getMarca() != null)
                existing.setMarca(patch.getMarca());

            if (patch.getPesoRoloGramas() != null)
                existing.setPesoRoloGramas(patch.getPesoRoloGramas());

            if (patch.getEstoqueGramas() != null)
                existing.setEstoqueGramas(patch.getEstoqueGramas());

            if (patch.getPrecoRolo() != null)
                existing.setPrecoRolo(patch.getPrecoRolo());

            return filamentoRepository.save(existing);
        });
    }

    @Override
    public void delete(UUID id) {
        fetchById(id).ifPresent(filamentoRepository::delete);
    }

    @Override
    public Page<Filamento> fetchAll(Pageable pageable) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return filamentoRepository.findAllByUsuario_Id(usuarioId, pageable);
    }

    @Override
    public Page<Filamento> fetchAll(Pageable pageable, String busca) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();

        if (busca == null || busca.isBlank()) {
            return filamentoRepository.findAllByUsuario_Id(usuarioId, pageable);
        }

        String[] termos = busca.trim()
                .toLowerCase(Locale.ROOT)
                .split("\\s+");

        Specification<Filamento> especificacao = (root, query, criteriaBuilder) -> {
            List<Predicate> filtros = new ArrayList<>();

            filtros.add(criteriaBuilder.equal(
                    root.get("usuario").get("id"), usuarioId
            ));

            for (String termo : termos) {
                String padrao = "%" + termo + "%";

                Predicate correspondeNome = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("nome")), padrao
                );

                Predicate correspondeCor = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("cor")), padrao
                );

                filtros.add(criteriaBuilder.or(
                        correspondeNome, correspondeCor
                ));
            }

            return criteriaBuilder.and(filtros.toArray(new Predicate[0]));
        };

        return filamentoRepository.findAll(especificacao, pageable);
    }

    @Override
    public Optional<Filamento> fetchById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return filamentoRepository.findByIdAndUsuario_Id(id, usuarioId);
    }

    @Override
    public List<Filamento> fetchByIds(java.util.Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return filamentoRepository.findAllByIdInAndUsuario_Id(ids, usuarioId);
    }

    @Override
    public List<Filamento> fetchRecentes(int limite) {
        if (limite <= 0) {
            return List.of();
        }
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return filamentoRepository.findAllByUsuario_Id(usuarioId,
                        PageRequest.of(0, limite, Sort.by(Sort.Order.desc("dataCadastro"),
                                Sort.Order.asc("nome"))))
                .getContent();
    }

    @Override
    public boolean existsById(UUID id) {
        UUID usuarioId = usuarioLogadoService.buscarIdUsuarioAtual();
        return filamentoRepository.existsByIdAndUsuario_Id(id, usuarioId);
    }
}
