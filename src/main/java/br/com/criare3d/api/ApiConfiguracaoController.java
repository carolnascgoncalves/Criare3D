package br.com.criare3d.api;

import br.com.criare3d.dto.ConfiguracaoRequest;
import br.com.criare3d.dto.TarifaEnergiaReferenciaResponse;
import br.com.criare3d.exception.RegraNegocioException;
import br.com.criare3d.service.TarifaEnergiaReferenciaService;
import br.com.criare3d.service.interfaces.ConfiguracaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/configuracao")
@SecurityRequirement(name="bearerAuth")
@RequiredArgsConstructor
public class ApiConfiguracaoController {
    private final ConfiguracaoService configuracoes;
    private final TarifaEnergiaReferenciaService tarifas;

    @GetMapping
    public ConfiguracaoRequest abrir() {
        return ConfiguracaoRequest.toDto(configuracoes.buscarOuCriarConfiguracao());
    }

    @PutMapping
    public ConfiguracaoRequest salvar(@Valid @RequestBody ConfiguracaoRequest request) {
        var atual = configuracoes.buscarOuCriarConfiguracao();
        var salva = configuracoes.update(atual.getId(), ConfiguracaoRequest.toEntity(request)).orElseThrow();
        return ConfiguracaoRequest.toDto(salva);
    }

    @GetMapping("/tarifas")
    public List<TarifaEnergiaReferenciaResponse> tarifas() {
        try {
            return tarifas.listar();
        } catch (IllegalStateException e) {
            throw new RegraNegocioException("tarifas_indisponiveis", "Não foi possível consultar a ANEEL agora.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public record Localizacao(@NotNull Double latitude, @NotNull Double longitude) {}

    @PostMapping("/cidade")
    public Map<String,String> cidade(@Valid @RequestBody Localizacao localizacao) {
        try {
            return tarifas.localizarCidade(localizacao.latitude(), localizacao.longitude());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RegraNegocioException("cidade_indisponivel", "Não foi possível identificar a cidade agora.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
