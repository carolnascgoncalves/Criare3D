package br.com.criare3d.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class TratamentoErroGlobal {
    private static final Logger log = LoggerFactory.getLogger(TratamentoErroGlobal.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String requisicaoInvalida(IllegalArgumentException erro, Model model) {
        model.addAttribute("codigo", 400);
        model.addAttribute("mensagem", mensagemSegura(erro.getMessage(), "Não foi possível concluir essa ação."));
        return "erro";
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public String estadoIndisponivel(IllegalStateException erro, Model model) {
        log.warn("Estado da aplicação impediu a solicitação", erro);
        model.addAttribute("codigo", 503);
        model.addAttribute("mensagem", "Não foi possível carregar esta página agora. Atualize e tente novamente.");
        return "erro";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> recursoNaoEncontrado(NoResourceFoundException erro,
                                                       HttpServletRequest request) {
        if ("/favicon.ico".equals(request.getRequestURI())) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String erroInesperado(Exception erro, HttpServletRequest request, Model model) {
        log.error("Erro não tratado na rota {}", request.getRequestURI(), erro);
        model.addAttribute("codigo", 500);
        model.addAttribute("mensagem", "Ocorreu um erro inesperado. Tente novamente em alguns instantes.");
        return "erro";
    }

    private String mensagemSegura(String mensagem, String padrao) {
        return mensagem == null || mensagem.isBlank() ? padrao : mensagem;
    }
}
