package br.com.criare3d.repository;

import br.com.criare3d.models.ItemCalculoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface ItemCalculoHistoricoRepository extends JpaRepository<ItemCalculoHistorico, UUID> {

    @Modifying
    @Query("""
        DELETE FROM ItemCalculoHistorico item
        WHERE item.calculoHistorico.id IN :calculoIds
        """)
    int excluirItensDosCalculos(@Param("calculoIds") Collection<UUID> calculoIds);
}
