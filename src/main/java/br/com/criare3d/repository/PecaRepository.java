package br.com.criare3d.repository;

import br.com.criare3d.models.Peca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PecaRepository extends JpaRepository<Peca, UUID> {
}
