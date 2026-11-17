package br.com.criare3d.dto;

import java.util.UUID;

public record SalvarComprador(UUID id, Long versao, String nome, String telefone, String email, String observacoes) {
}
