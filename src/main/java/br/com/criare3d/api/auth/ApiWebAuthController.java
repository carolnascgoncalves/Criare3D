package br.com.criare3d.api.auth;

import br.com.criare3d.api.ApiProblemas;
import br.com.criare3d.exception.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;

/** O refresh da web nunca é devolvido ao JavaScript. Origin e cabeçalho customizado protegem contra CSRF. */
@RestController
@RequestMapping("/api/v1/auth/web")
public class ApiWebAuthController {
    private static final String COOKIE = "cost3d_refresh";
    private static final String CAMINHO = "/api/v1/auth/web";
    private final ApiAuthService auth;
    private final ApiWebOrigins origens;
    private final Clock clock;
    public ApiWebAuthController(ApiAuthService auth, ApiWebOrigins origens, Clock clock) {
        this.auth=auth; this.origens=origens; this.clock=clock;
    }
    public record SessaoWeb(String accessToken, String tokenType, long expiresIn) {}

    @PostMapping("/login")
    public ResponseEntity<SessaoWeb> login(@Valid @RequestBody ApiAuthController.Login dados, HttpServletRequest request) {
        origens.exigirNavegador(request);
        return resposta(auth.login(dados.email(), dados.senha(), request.getRemoteAddr()), request);
    }
    @PostMapping("/refresh")
    public ResponseEntity<SessaoWeb> renovar(@CookieValue(name=COOKIE, required=false) String refresh, HttpServletRequest request) {
        origens.exigirNavegador(request);
        if (refresh == null || !refresh.matches("[A-Za-z0-9_-]{43}")) throw ApiAuthService.invalido();
        return resposta(auth.refresh(refresh), request);
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> sair(@CookieValue(name=COOKIE, required=false) String refresh, HttpServletRequest request) {
        origens.exigirNavegador(request);
        if (refresh != null && refresh.matches("[A-Za-z0-9_-]{43}")) auth.revogarPeloRefresh(refresh);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO, request).toString()).build();
    }
    private ResponseEntity<SessaoWeb> resposta(ApiAuthService.Tokens tokens, HttpServletRequest request) {
        Duration validade = Duration.between(clock.instant(), tokens.refreshExpiraEm().toInstant());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .header(HttpHeaders.SET_COOKIE, cookie(tokens.refreshToken(), validade, request).toString())
            .body(new SessaoWeb(tokens.accessToken(), tokens.tokenType(), tokens.expiresIn()));
    }
    private ResponseCookie cookie(String valor, Duration validade, HttpServletRequest request) {
        return ResponseCookie.from(COOKIE, valor).httpOnly(true).secure(request.isSecure())
            .sameSite("Strict").path(CAMINHO).maxAge(validade).build();
    }
    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ProblemDetail> erro(RegraNegocioException erro, HttpServletRequest request) {
        var resposta = ResponseEntity.status(erro.getStatus());
        if (erro.getStatus() == HttpStatus.UNAUTHORIZED)
            resposta.header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO, request).toString());
        if (erro instanceof ApiAuthService.LoginLimitado limitado)
            resposta.header("Retry-After", Long.toString(limitado.getSegundos()));
        return resposta.body(ApiProblemas.problema(erro.getStatus(), erro.getCodigo(), erro.getMessage()));
    }
}
