package br.com.criare3d.controller;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Fotografias são produzidas pelo servidor; formulários só informam os parâmetros editáveis.
 */
@ControllerAdvice
public class CustosInternosBindingAdvice {
    @InitBinder
    public void proteger(WebDataBinder binder) {
        binder.setDisallowedFields("referenciaCustosHistoricos", "custosPreservados", "custosPreservados.*", "precosAtuaisNoOrcamento");
    }
}
