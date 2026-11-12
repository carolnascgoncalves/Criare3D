package br.com.criare3d.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ClienteAdminResumo {
    UUID getId();
    java.time.Instant getExcluidoEm();
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
    LocalDateTime getUltimoAcesso();
    Long getQuantidadeCalculos();
}
