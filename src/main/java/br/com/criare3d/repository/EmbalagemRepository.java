package br.com.criare3d.repository;

import br.com.criare3d.models.Embalagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EmbalagemRepository extends JpaRepository<Embalagem, UUID> {

    List<Embalagem> findAllByIdIn(Collection<UUID> ids);

    Page<Embalagem> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
