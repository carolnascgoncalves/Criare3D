package br.com.criare3d.exception;

import org.springframework.http.HttpStatus;

public class RegraNegocioException extends IllegalArgumentException {
    private final String codigo;
    private final HttpStatus status;

    public RegraNegocioException(String codigo, String mensagem, HttpStatus status) {
        super(mensagem);
        this.codigo = codigo;
        this.status = status;
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
