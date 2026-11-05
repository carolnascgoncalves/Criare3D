
package br.com.criare3d.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ComparacaoPlataforma {

    private String nome;
    private String codigo;
    private BigDecimal precoVenda;
    private BigDecimal taxas;
    private BigDecimal lucro;
    private BigDecimal margem;
    private String aviso;
    private boolean melhor;
    private boolean selecionada;

    public boolean isCalculada() {
        return aviso == null && precoVenda != null;
    }
}
