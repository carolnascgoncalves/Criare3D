package br.com.criare3d.dto;

import java.util.UUID;

public interface ClienteAdminResumo {
    UUID getId();
    String getNome();
    String getSlug();
    Boolean getAtivo();
    String getCorPrimaria();
    String getCorDestaque();
    String getCorFundo();
    Long getTemaVersao();
    Boolean getTemLogo();
    UUID getUsuarioId();
    String getEmail();
}
