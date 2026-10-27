package br.com.criare3d.repository;

import br.com.criare3d.models.Configuracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConfiguracaoRepository extends JpaRepository<Configuracao, UUID> {
}
