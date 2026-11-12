package br.com.criare3d.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "cor_primaria", nullable = false, length = 7)
    private String corPrimaria;

    @Column(name = "cor_destaque", nullable = false, length = 7)
    private String corDestaque;

    @Column(name = "cor_fundo", nullable = false, length = 7)
    private String corFundo;

    @Column(columnDefinition = "bytea")
    private byte[] logo;

    @Column(name = "excluido_em")
    private java.time.Instant excluidoEm;

    @Column(name = "tema_versao", nullable = false)
    private long temaVersao = 1;
}
