package br.com.criare3d.service;

import java.util.Map;

public class CompradorDuplicadoException extends RuntimeException {
    private final Map<String, String> campos;

    public CompradorDuplicadoException(Map<String, String> campos) {
        super("Já existe um cliente com os dados informados.");
        this.campos = Map.copyOf(campos);
    }

    public Map<String, String> getCampos() {
        return campos;
    }
}
