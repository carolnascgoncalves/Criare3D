package br.com.criare3d.dto;
import lombok.*;
import jakarta.validation.constraints.Size;
import java.util.*;
@Getter @Setter
public class SelecaoPedidoRequest {
 @jakarta.validation.constraints.Size(max=150) private String cliente;
 @jakarta.validation.constraints.Size(max=150) private String contato;
 @jakarta.validation.constraints.Size(max=150) private String titulo;

 @Size(max=100) private List<Item> itens=new ArrayList<>();
 @Getter @Setter public static class Item {private UUID referencia;private boolean selecionado;private int quantidade=1;private int estoque;}
}
