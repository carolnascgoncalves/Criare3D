
package br.com.criare3d.repository;

import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    Optional<Usuario> findFirstByTenant_IdAndPerfil(UUID tenantId, Perfil perfil);

    boolean existsByPerfil(Perfil perfil);
}
