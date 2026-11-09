package br.com.criare3d.dto;

public interface TenantTemaProjection {
    String getNome();
    String getCorPrimaria();
    String getCorDestaque();
    String getCorFundo();
    Long getTemaVersao();
    Boolean getAtivo();
    Boolean getTemLogo();
}
