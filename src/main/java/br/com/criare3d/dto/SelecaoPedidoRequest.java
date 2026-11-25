package br.com.criare3d.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SelecaoPedidoRequest {
    @jakarta.validation.constraints.Size(max = 150)
    private String cliente;
    private java.util.UUID compradorId;
    @jakarta.validation.constraints.Size(max = 150)
    private String contato;
    @jakarta.validation.constraints.Email
    @jakarta.validation.constraints.Size(max = 150)
    private String email;
    @jakarta.validation.constraints.Size(max = 150)
    private String titulo;

    private boolean custosAtuais;
    private UUID embalagemAtualId;
    @Size(max = 100)
    private List<Item> itens = new ArrayList<>();

    @Getter
    @Setter
    @io.swagger.v3.oas.annotations.media.Schema(name="SelecaoPedidoItem")
    public static class Item {
        private UUID referencia;
        private boolean selecionado;
        private int quantidade = 1;
        private int estoque;
    }
}
