package br.com.criare3d.repository;

import br.com.criare3d.models.Impressora;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImpressoraRepository extends JpaRepository<Impressora, UUID> {
}
