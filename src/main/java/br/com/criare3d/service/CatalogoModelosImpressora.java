package br.com.criare3d.service;

import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CatalogoModelosImpressora {
    public record Modelo(String nome, BigDecimal potenciaMediaPLA, Map<String, BigDecimal> potenciasOutrosMateriais,
                         String potenciaMaxima, String fonte) {
    }

    private final List<Modelo> modelos;

    public CatalogoModelosImpressora() {
        try (var arquivo = getClass().getResourceAsStream("/referencias/modelos-impressora.json")) {
            if (arquivo == null) throw new IllegalStateException("Catálogo de modelos não encontrado.");
            modelos = List.copyOf(Arrays.asList(JsonMapper.builder().build().readValue(arquivo, Modelo[].class)));
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o catálogo de modelos.", e);
        }
    }

    public List<Modelo> listar() {
        return modelos;
    }

    public Optional<Modelo> buscar(String nome) {
        return nome == null ? Optional.empty() : modelos.stream()
            .filter(modelo -> modelo.nome().equalsIgnoreCase(nome.strip())).findFirst();
    }
}
