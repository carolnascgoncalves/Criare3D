package br.com.criare3d.repository;

import br.com.criare3d.models.Comprador;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CompradorRepository extends JpaRepository<Comprador, UUID> {
    // HQL garante o discriminador de tenant também no carregamento por identificador.
    @Override
    @Query("select c from Comprador c where c.id=:id")
    java.util.Optional<Comprador> findById(@Param("id") UUID id);

    @Query("""
        select c from Comprador c where c.arquivado=:arquivados and (:busca=''
        or lower(c.nome) like lower(concat('%',:busca,'%'))
        or lower(c.email) like lower(concat('%',:busca,'%'))
        or c.telefone like concat('%',:busca,'%')
        or (:digitos<>'' and c.telefoneDigitos like concat('%',:digitos,'%')))
        """)
    Page<Comprador> pesquisar(@Param("busca") String busca, @Param("digitos") String digitos,
                             @Param("arquivados") boolean arquivados, Pageable pagina);

    @Query("select c from Comprador c where lower(c.email)=lower(:email)")
    List<Comprador> porEmail(@Param("email") String email);

    @Query("select c from Comprador c where c.telefoneDigitos=:digitos")
    List<Comprador> porTelefone(@Param("digitos") String digitos);

    interface Contadores {
        long getAtivos();
        long getArquivados();
        long getComAndamento();
        long getSemOrcamento();
    }

    // SQL nativo precisa filtrar a conta também nas subconsultas.
    @Query(value = """
        select count(*) filter (where not c.arquivado) as ativos,
               count(*) filter (where c.arquivado) as arquivados,
               count(*) filter (where exists (
                   select 1 from pedido_orcamento p
                   where p.tenant_id=:tenant and p.comprador_id=c.id and not p.arquivado
                   and p.status in ('ENVIADO','APROVADO','PRONTO')
               )) as comAndamento,
               count(*) filter (where not c.arquivado and not exists (
                   select 1 from pedido_orcamento p
                   where p.tenant_id=:tenant and p.comprador_id=c.id and not p.arquivado
                   and p.criado_em>=:corte
               )) as semOrcamento
        from comprador c where c.tenant_id=:tenant
        """, nativeQuery = true)
    Contadores contadores(@Param("tenant") UUID tenant, @Param("corte") LocalDateTime corte);
}
