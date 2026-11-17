package br.com.criare3d.api.auth;

import br.com.criare3d.dto.AlterarSenhaRequest;
import br.com.criare3d.security.UsuarioPrincipal;
import br.com.criare3d.service.interfaces.UsuarioService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class ApiAuthController {
    private final ApiAuthService auth;
    private final UsuarioService usuarios;

    public ApiAuthController(ApiAuthService auth, UsuarioService usuarios) {
        this.auth = auth;
        this.usuarios = usuarios;
    }

    public record Login(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 72) String senha) {}

    public record Renovar(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String refreshToken) {}

    public record Conta(
            UUID id, UUID tenantId, String email, String perfil, boolean trocaSenhaObrigatoria) {}

    @PostMapping("/login")
    public ApiAuthService.Tokens login(
            @Valid @RequestBody Login dados, HttpServletRequest request) {
        return auth.login(dados.email(), dados.senha(), request.getRemoteAddr());
    }

    @PostMapping("/refresh")
    public ApiAuthService.Tokens refresh(@Valid @RequestBody Renovar dados) {
        return auth.refresh(dados.refreshToken());
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public Conta me(@AuthenticationPrincipal UsuarioPrincipal usuario) {
        return new Conta(
                usuario.getUsuarioId(),
                usuario.getTenantId(),
                usuario.getUsername(),
                usuario.getPerfil().name(),
                usuario.isTrocaSenhaObrigatoria());
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UsuarioPrincipal usuario, HttpServletRequest request) {
        auth.revogar((UUID) request.getAttribute("api.familia"), usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/senha")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> senha(
            @Valid @RequestBody AlterarSenhaRequest dados,
            @AuthenticationPrincipal UsuarioPrincipal usuario) {
        if (!dados.getNovaSenha().equals(dados.getConfirmarNovaSenha()))
            throw new IllegalArgumentException("As senhas não coincidem.");
        usuarios.alterarSenhaAtual(dados.getSenhaAtual(), dados.getNovaSenha());
        auth.revogarUsuario(usuario.getUsuarioId());
        return ResponseEntity.noContent().build();
    }
}
