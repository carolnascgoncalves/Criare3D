package br.com.criare3d.api.auth;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

public final class ApiRespostaSeguranca {
    private ApiRespostaSeguranca() {}

    public static void enviar(
            HttpServletResponse response, HttpStatus status, String codigo, String detalhe)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        if (status == HttpStatus.UNAUTHORIZED) response.setHeader("WWW-Authenticate", "Bearer");
        response.getWriter()
                .write(
                        JsonMapper.builder()
                                .build()
                                .writeValueAsString(
                                        Map.of(
                                                "type",
                                                "urn:cost3d:problema:" + codigo,
                                                "title",
                                                status.getReasonPhrase(),
                                                "status",
                                                status.value(),
                                                "detail",
                                                detalhe,
                                                "codigo",
                                                codigo)));
    }
}
