package br.com.criare3d.repository;

import br.com.criare3d.models.Embalagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmbalagemRepository extends JpaRepository<Embalagem, UUID> {
}
