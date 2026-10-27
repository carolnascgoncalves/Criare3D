package br.com.criare3d.service.interfaces;

import br.com.criare3d.models.Impressora;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ImpressoraService {
    Impressora create(Impressora impressora);
    Optional<Impressora> update(UUID id, Impressora impressora);
    void delete(UUID id);
    Page<Impressora> fetchAll(Pageable pageable);
    Optional<Impressora> fetchById(UUID id);
    boolean existsById(UUID id);
}
