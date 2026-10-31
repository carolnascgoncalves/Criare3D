package br.com.criare3d.repository;

import br.com.criare3d.models.Filamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FilamentoRepository extends JpaRepository<Filamento, UUID> {
    Page<Filamento> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}