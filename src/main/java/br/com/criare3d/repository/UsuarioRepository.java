
package br.com.criare3d.repository;

import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    Optional<Usuario> findByTenant_IdAndPerfil(UUID tenantId, Perfil perfil);

    boolean existsByPerfil(Perfil perfil);

    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.ultimoAcesso = :instante WHERE u.id = :id")
    int registrarUltimoAcesso(@Param("id") UUID id, @Param("instante") LocalDateTime instante);
}
