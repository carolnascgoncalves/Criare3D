package br.com.criare3d.api;

import br.com.criare3d.exception.RegraNegocioException;
import br.com.criare3d.service.CompradorDuplicadoException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.*;

@RestControllerAdvice(basePackages = "br.com.criare3d.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiProblemas {
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(ApiProblemas.class);

    public static ProblemDetail problema(HttpStatus status, String codigo, String mensagem) {
        var p = ProblemDetail.forStatusAndDetail(status, mensagem);
        p.setType(URI.create("urn:cost3d:problema:" + codigo));
        p.setTitle(status.getReasonPhrase());
        p.setProperty("codigo", codigo);
        return p;
    }

    @ExceptionHandler(br.com.criare3d.api.auth.ApiAuthService.LoginLimitado.class)
    ResponseEntity<ProblemDetail> limitado(
            br.com.criare3d.api.auth.ApiAuthService.LoginLimitado e) {
        return ResponseEntity.status(429)
                .header("Retry-After", Long.toString(e.getSegundos()))
                .body(problema(e.getStatus(), e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ProblemDetail> regra(RegraNegocioException e) {
        return ResponseEntity.status(e.getStatus())
                .body(problema(e.getStatus(), e.getCodigo(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException e) {
        var p =
                problema(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "campos_invalidos",
                        "Confira os campos informados.");
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult()
                .getFieldErrors()
                .forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        p.setProperty("campos", campos);
        return ResponseEntity.unprocessableEntity().body(p);
    }

    @ExceptionHandler(br.com.criare3d.service.FreteManualNecessarioException.class)
    ResponseEntity<ProblemDetail> freteManual(br.com.criare3d.service.FreteManualNecessarioException e) {
        return ResponseEntity.unprocessableEntity().body(
                problema(HttpStatus.UNPROCESSABLE_ENTITY, "frete_manual", e.getMessage()));
    }

    @ExceptionHandler(CompradorDuplicadoException.class)
    ResponseEntity<ProblemDetail> duplicado(CompradorDuplicadoException e) {
        var p = problema(HttpStatus.CONFLICT, "cliente_duplicado", e.getMessage());
        p.setProperty("campos", e.getCampos());
        return ResponseEntity.status(409).body(p);
    }

    @ExceptionHandler({
        NoSuchElementException.class,
        NoResourceFoundException.class,
        NoHandlerFoundException.class
    })
    ResponseEntity<ProblemDetail> ausente(Exception e) {
        return ResponseEntity.status(404)
                .body(problema(HttpStatus.NOT_FOUND, "nao_encontrado", "Registro não encontrado."));
    }

    @ExceptionHandler({
        OptimisticLockingFailureException.class,
        DataIntegrityViolationException.class
    })
    ResponseEntity<ProblemDetail> conflito(Exception e) {
        return ResponseEntity.status(409)
                .body(
                        problema(
                                HttpStatus.CONFLICT,
                                "conflito",
                                "Os dados foram alterados. Atualize antes de tentar novamente."));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class
    })
    ResponseEntity<ProblemDetail> formato(Exception e) {
        return ResponseEntity.badRequest()
                .body(
                        problema(
                                HttpStatus.BAD_REQUEST,
                                "formato_invalido",
                                "Confira o formato dos parâmetros e do JSON."));
    }

    @ExceptionHandler(
            org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> parametros(Exception e) {
        return ResponseEntity.unprocessableEntity()
                .body(
                        problema(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "parametros_invalidos",
                                "Confira os parâmetros informados."));
    }

    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ResponseEntity<ProblemDetail> entrada(Exception e) {
        return ResponseEntity.unprocessableEntity()
                .body(
                        problema(
                                HttpStatus.UNPROCESSABLE_ENTITY,
                                "entrada_invalida",
                                "Confira os dados informados."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> metodo(Exception e) {
        return ResponseEntity.status(405)
                .body(
                        problema(
                                HttpStatus.METHOD_NOT_ALLOWED,
                                "metodo_invalido",
                                "Método não permitido nesta rota."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> midia(Exception e) {
        return ResponseEntity.status(415)
                .body(
                        problema(
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                                "midia_invalida",
                                "Envie o conteúdo em JSON."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> inesperado(Exception e, HttpServletRequest request) {
        LOG.error("Erro na API {}", request.getRequestURI(), e);
        return ResponseEntity.internalServerError()
                .body(
                        problema(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "erro_interno",
                                "Não foi possível concluir a operação."));
    }
}
