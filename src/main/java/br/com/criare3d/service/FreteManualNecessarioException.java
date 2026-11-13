package br.com.criare3d.service;

/** A tabela de estimativas não cobre este preço; o usuário precisa informar o frete. */
public class FreteManualNecessarioException extends IllegalArgumentException {
    public FreteManualNecessarioException(String mensagem) {
        super(mensagem);
    }
}
