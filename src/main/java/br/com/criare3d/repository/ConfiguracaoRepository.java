package br.com.criare3d.repository;

import br.com.criare3d.models.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, UUID> {

    Optional<Configuracao> findFirstByTenantId(UUID tenantId);
}
