package br.com.criare3d.service;

import br.com.criare3d.dto.Modelo3mfResponse;
import br.com.criare3d.dto.Modelo3mfFilamentoResponse;
import br.com.criare3d.dto.Modelo3mfPlacaResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class Modelo3mfParser {
    private static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024;
    private static final int MAX_METADATA_BYTES = 4 * 1024 * 1024;
    private static final int MAX_PREVIEW_BYTES = 2 * 1024 * 1024;
    private static final int MAX_STATISTICS_BYTES = 8 * 1024 * 1024;
    private static final int MAX_TOTAL_STATISTICS_BYTES = 32 * 1024 * 1024;
    private static final int MAX_PLACAS_PREVIEW = 8;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public Modelo3mfResponse analisar(MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Selecione um arquivo .3mf.");
        }
        if (arquivo.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("O arquivo deve ter no máximo 50 MB.");
        }
        String nomeArquivo = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename();
        if (!nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".3mf")) {
            throw new IllegalArgumentException("Envie um arquivo de projeto ou impressão fatiada com extensão .3mf.");
        }

        Path temporario = Files.createTempFile("criare-modelo-", ".3mf");
        try {
            arquivo.transferTo(temporario);
            try (ZipFile zip = new ZipFile(temporario.toFile())) {
                ZipEntry modelo = zip.getEntry("3D/3dmodel.model");
                if (modelo == null) throw new IllegalArgumentException("Esse arquivo não parece ser um projeto 3MF válido.");
                DocumentoModelo dadosModelo = lerModelo(lerLimitado(zip, modelo));
                JsonNode configuracoes = null;
                ZipEntry settings = zip.getEntry("Metadata/project_settings.config");
                if (settings != null) configuracoes = jsonMapper.readTree(lerLimitado(zip, settings));
                Map<Integer, String> previews = lerPreviewsPlacas(zip);
                Map<Integer, DadosEstatisticaPlaca> estatisticas = lerEstatisticas(zip, configuracoes);
                // Bambu Studio grava as estatísticas em slice_info.config (XML). Orca
                // pode trazer esse arquivo também, mas o JSON por placa continua sendo
                // a fonte preferencial quando os dois formatos estão presentes.
                lerEstatisticasBambu(zip, configuracoes).forEach(estatisticas::putIfAbsent);
                boolean jaFatiado = temResultadoFatiamento(zip) || !estatisticas.isEmpty();
                int maiorNumeroPreview = previews.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
                int maiorNumeroEstatistica = estatisticas.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
                int numeroPlacas = Math.max(Math.max(maiorNumeroPreview, maiorNumeroEstatistica), contarPlacas(zip));
                if (numeroPlacas == 0) numeroPlacas = 1;
                List<Modelo3mfPlacaResponse> placas = montarPlacas(numeroPlacas, previews, estatisticas);
                List<Modelo3mfFilamentoResponse> filamentosUsados = somarFilamentos(estatisticas);
                Double tempoTotalSegundos = estatisticas.isEmpty() ? null : estatisticas.values().stream()
                        .mapToDouble(DadosEstatisticaPlaca::tempoSegundos).sum();
                Double pesoTotalGramas = estatisticas.isEmpty() ? null : estatisticas.values().stream()
                        .mapToDouble(DadosEstatisticaPlaca::pesoGramas).sum();

                return new Modelo3mfResponse(
                        limitar(primeiro(dadosModelo.nome(), removerExtensao(nomeArquivo)), 150),
                        texto(configuracoes, "printer_model"),
                        vetor(configuracoes, "filament_type"),
                        vetor(configuracoes, "filament_colour"),
                        texto(configuracoes, "layer_height"),
                        vetor(configuracoes, "nozzle_diameter"),
                        primeiro(dadosModelo.perfil(), texto(configuracoes, "print_settings_id")),
                        primeiro(dadosModelo.fatiador(), "Bambu Studio / OrcaSlicer"),
                        jaFatiado,
                        !estatisticas.isEmpty(),
                        !estatisticas.isEmpty() && estatisticas.size() >= numeroPlacas,
                        estatisticas.size(),
                        tempoTotalSegundos,
                        pesoTotalGramas,
                        filamentosUsados,
                        numeroPlacas,
                        placas
                );
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("Não consegui ler os metadados deste .3mf. Tente exportar o projeto novamente pelo fatiador.");
            }
        } finally {
            Files.deleteIfExists(temporario);
        }
    }

    private Map<Integer, String> lerPreviewsPlacas(ZipFile zip) {
        TreeMap<Integer, String> imagens = new TreeMap<>();
        Pattern padrao = Pattern.compile("(?i)^Metadata/plate_(\\d+)(?:_small)?\\.(?:png|jpe?g|webp)$");
        Enumeration<? extends ZipEntry> entradas = zip.entries();
        while (entradas.hasMoreElements()) {
            ZipEntry entrada = entradas.nextElement();
            Matcher matcher = padrao.matcher(entrada.getName());
            if (!matcher.matches()) continue;
            int numero = Integer.parseInt(matcher.group(1));
            if (numero < 1 || numero > 100 || imagens.containsKey(numero) || imagens.size() >= MAX_PLACAS_PREVIEW) continue;
            String imagem = imagemComoDataUri(zip, entrada);
            if (imagem != null) imagens.put(numero, imagem);
        }
        if (imagens.isEmpty()) {
            String imagemGeral = imagemComoDataUri(zip, zip.getEntry("Auxiliaries/.thumbnails/thumbnail_middle.png"));
            if (imagemGeral == null) imagemGeral = imagemComoDataUri(zip, zip.getEntry("Auxiliaries/.thumbnails/thumbnail_3mf.png"));
            if (imagemGeral != null) imagens.put(1, imagemGeral);
        }
        return imagens;
    }

    private Map<Integer, DadosEstatisticaPlaca> lerEstatisticas(ZipFile zip, JsonNode configuracoes) {
        Map<Integer, DadosEstatisticaPlaca> resultado = new LinkedHashMap<>();
        int totalBytes = 0;
        Pattern padrao = Pattern.compile("(?i)^Metadata/plate_(\\d+)\\.statistics\\.json$");
        Enumeration<? extends ZipEntry> entradas = zip.entries();
        while (entradas.hasMoreElements()) {
            ZipEntry entrada = entradas.nextElement();
            Matcher matcher = padrao.matcher(entrada.getName());
            if (!matcher.matches()) continue;
            try {
                int numero = Integer.parseInt(matcher.group(1));
                if (numero < 1 || numero > 100 || totalBytes >= MAX_TOTAL_STATISTICS_BYTES) continue;
                byte[] conteudo = lerLimitado(zip, entrada, MAX_STATISTICS_BYTES);
                totalBytes += conteudo.length;
                if (totalBytes > MAX_TOTAL_STATISTICS_BYTES) continue;
                JsonNode raiz = jsonMapper.readTree(conteudo);
                JsonNode resumo = raiz.path("summary");
                if (resumo.path("time").isMissingNode() || resumo.path("time").isNull()) continue;
                double tempo = valorNumerico(resumo, "time");
                List<Modelo3mfFilamentoResponse> filamentos = lerFilamentos(resumo, configuracoes);
                double peso = valorNumerico(resumo, "weight");
                if (peso <= 0) peso = filamentos.stream().mapToDouble(Modelo3mfFilamentoResponse::pesoGramas).sum();
                if (tempo >= 0 && peso >= 0) resultado.put(numero, new DadosEstatisticaPlaca(tempo, peso, filamentos));
            } catch (Exception ignorado) {
                // Um arquivo de estatísticas inválido não impede a leitura das demais placas.
            }
        }
        return resultado;
    }

    private Map<Integer, DadosEstatisticaPlaca> lerEstatisticasBambu(ZipFile zip, JsonNode configuracoes) {
        Map<Integer, DadosEstatisticaPlaca> resultado = new TreeMap<>();
        ZipEntry entrada = zip.getEntry("Metadata/slice_info.config");
        if (entrada == null) return resultado;
        try {
            Document documento = criarDocumentoXml(lerLimitado(zip, entrada));
            NodeList placas = documento.getElementsByTagName("plate");
            for (int i = 0; i < placas.getLength(); i++) {
                Element placa = (Element) placas.item(i);
                Map<String, String> metadados = new HashMap<>();
                NodeList elementosMetadata = placa.getElementsByTagName("metadata");
                for (int j = 0; j < elementosMetadata.getLength(); j++) {
                    Element item = (Element) elementosMetadata.item(j);
                    metadados.put(item.getAttribute("key"), item.getAttribute("value"));
                }

                int numero = inteiroPositivo(metadados.get("index"), i + 1);
                numero = inteiroPositivo(metadados.get("plater_id"), numero);
                if (numero > 100) continue;

                double tempo = numero(metadados.get("prediction")); // Bambu grava segundos.
                double pesoDeclarado = numero(metadados.get("weight"));
                List<Modelo3mfFilamentoResponse> filamentos = lerFilamentosBambu(placa, configuracoes);
                double peso = pesoDeclarado > 0 ? pesoDeclarado
                        : filamentos.stream().mapToDouble(Modelo3mfFilamentoResponse::pesoGramas).sum();
                if (tempo <= 0 || peso < 0) continue;
                resultado.put(numero, new DadosEstatisticaPlaca(tempo, peso, filamentos));
            }
        } catch (Exception ignorado) {
            // Metadados de fatiamento malformados não impedem a leitura do modelo.
        }
        return resultado;
    }

    private List<Modelo3mfFilamentoResponse> lerFilamentosBambu(Element placa, JsonNode configuracoes) {
        List<Modelo3mfFilamentoResponse> resultado = new ArrayList<>();
        NodeList elementos = placa.getElementsByTagName("filament");
        for (int i = 0; i < elementos.getLength(); i++) {
            Element item = (Element) elementos.item(i);
            int id = inteiroPositivo(item.getAttribute("id"), 0);
            if (id < 1 || id > 100) continue;
            String material = primeiro(item.getAttribute("type"), valorVetor(configuracoes, "filament_type", id - 1));
            String cor = primeiro(item.getAttribute("color"), valorVetor(configuracoes, "filament_colour", id - 1));
            double peso = numero(item.getAttribute("used_g"));
            if (peso < 0) continue;
            resultado.add(new Modelo3mfFilamentoResponse(id, material, cor, peso));
        }
        return resultado;
    }

    private Document criarDocumentoXml(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private int inteiroPositivo(String valor, int alternativa) {
        if (valor == null || valor.isBlank()) return alternativa;
        try {
            int numero = Integer.parseInt(valor.trim());
            return numero > 0 ? numero : alternativa;
        } catch (NumberFormatException ignorado) {
            return alternativa;
        }
    }

    private double numero(String valor) {
        if (valor == null || valor.isBlank()) return 0;
        try {
            double numero = Double.parseDouble(valor.trim());
            return Double.isFinite(numero) && numero >= 0 ? numero : 0;
        } catch (NumberFormatException ignorado) {
            return 0;
        }
    }

    private List<Modelo3mfPlacaResponse> montarPlacas(
            int quantidade,
            Map<Integer, String> previews,
            Map<Integer, DadosEstatisticaPlaca> estatisticas
    ) {
        List<Modelo3mfPlacaResponse> placas = new ArrayList<>();
        for (int numero = 1; numero <= Math.min(quantidade, MAX_PLACAS_PREVIEW); numero++) {
            DadosEstatisticaPlaca dados = estatisticas.get(numero);
            placas.add(new Modelo3mfPlacaResponse(
                    numero,
                    previews.getOrDefault(numero, ""),
                    dados == null ? null : dados.tempoSegundos(),
                    dados == null ? null : dados.pesoGramas(),
                    dados == null ? List.of() : dados.filamentos()
            ));
        }
        return placas;
    }

    private List<Modelo3mfFilamentoResponse> somarFilamentos(
            Map<Integer, DadosEstatisticaPlaca> estatisticas
    ) {
        Map<Integer, Modelo3mfFilamentoResponse> soma = new HashMap<>();
        for (DadosEstatisticaPlaca placa : estatisticas.values()) {
            for (Modelo3mfFilamentoResponse filamento : placa.filamentos()) {
                Modelo3mfFilamentoResponse atual = soma.get(filamento.id());
                soma.put(filamento.id(), new Modelo3mfFilamentoResponse(
                        filamento.id(),
                        primeiro(filamento.material(), atual == null ? "" : atual.material()),
                        primeiro(filamento.cor(), atual == null ? "" : atual.cor()),
                        filamento.pesoGramas() + (atual == null ? 0 : atual.pesoGramas())
                ));
            }
        }
        if (!soma.isEmpty()) return soma.values().stream().sorted(java.util.Comparator.comparingInt(Modelo3mfFilamentoResponse::id)).toList();
        return List.of();
    }

    private List<Modelo3mfFilamentoResponse> lerFilamentos(JsonNode resumo, JsonNode configuracoes) {
        List<Modelo3mfFilamentoResponse> resultado = new ArrayList<>();
        JsonNode filamentos = resumo.path("filaments");
        if (!filamentos.isArray()) return resultado;
        for (JsonNode item : filamentos) {
            int id = (int) valorNumerico(item, "id");
            if (id < 1 || id > 100) continue;
            resultado.add(new Modelo3mfFilamentoResponse(
                    id,
                    valorVetor(configuracoes, "filament_type", id - 1),
                    valorVetor(configuracoes, "filament_colour", id - 1),
                    valorNumerico(item, "weight")
            ));
        }
        return resultado;
    }

    private String valorVetor(JsonNode raiz, String chave, int indice) {
        if (raiz == null) return "";
        JsonNode vetor = raiz.path(chave);
        if (vetor.isArray()) {
            JsonNode item = vetor.path(indice);
            return item.isMissingNode() || item.isNull() ? "" : item.asText("").trim();
        }
        return indice == 0 ? vetor.asText("").trim() : "";
    }

    private double valorNumerico(JsonNode raiz, String chave) {
        JsonNode valor = raiz.path(chave);
        if (valor.isNumber()) return valor.asDouble();
        if (valor.isTextual()) {
            try { return Double.parseDouble(valor.asText()); }
            catch (NumberFormatException ignored) { return 0; }
        }
        return 0;
    }

    private int contarPlacas(ZipFile zip) {
        int maiorNumero = 0;
        for (String caminho : List.of("Metadata/model_settings.config", "Metadata/slice_info.config")) {
            ZipEntry entrada = zip.getEntry(caminho);
            if (entrada == null) continue;
            try {
                byte[] xml = lerLimitado(zip, entrada);
                Document documento = criarDocumentoXml(xml);
                NodeList placas = documento.getElementsByTagName("plate");
                for (int i = 0; i < placas.getLength(); i++) {
                    Element placa = (Element) placas.item(i);
                    NodeList metadados = placa.getElementsByTagName("metadata");
                    int numero = i + 1;
                    for (int j = 0; j < metadados.getLength(); j++) {
                        Element metadata = (Element) metadados.item(j);
                        if ("plater_id".equals(metadata.getAttribute("key"))) {
                            try { numero = Integer.parseInt(metadata.getAttribute("value")); }
                            catch (NumberFormatException ignored) { }
                            break;
                        }
                    }
                    maiorNumero = Math.max(maiorNumero, Math.min(numero, 100));
                }
            } catch (Exception ignored) { }
        }
        return maiorNumero;
    }

    private String imagemComoDataUri(ZipFile zip, ZipEntry entrada) {
        if (entrada == null || entrada.isDirectory() || entrada.getSize() > MAX_PREVIEW_BYTES) return null;
        String caminho = entrada.getName().toLowerCase(Locale.ROOT);
        if (!(caminho.endsWith(".png") || caminho.endsWith(".jpg")
                || caminho.endsWith(".jpeg") || caminho.endsWith(".webp"))) return null;
        try {
            byte[] bytes = lerLimitado(zip, entrada, MAX_PREVIEW_BYTES);
            String mime = detectarMimeImagem(bytes);
            if (mime == null) return null;
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String detectarMimeImagem(byte[] bytes) {
        if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4e && bytes[3] == 0x47) return "image/png";
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return null;
    }

    private DocumentoModelo lerModelo(byte[] xml) throws Exception {
        Document documento = criarDocumentoXml(xml);
        String nome = "";
        String perfil = "";
        String fatiador = "";
        NodeList metadados = documento.getElementsByTagName("metadata");
        for (int i = 0; i < metadados.getLength(); i++) {
            Element metadata = (Element) metadados.item(i);
            String chave = metadata.getAttribute("name");
            String valor = metadata.getTextContent().trim();
            if ("Title".equalsIgnoreCase(chave)) nome = valor;
            else if ("ProfileTitle".equalsIgnoreCase(chave)) perfil = valor;
            else if ("Application".equalsIgnoreCase(chave)) fatiador = valor;
        }
        return new DocumentoModelo(nome, perfil, fatiador);
    }

    private boolean temResultadoFatiamento(ZipFile zip) {
        boolean possuiGcode = zip.stream().anyMatch(entrada ->
                entrada.getName().matches("(?i)Metadata/plate_\\d+\\.gcode"));
        boolean possuiEstatisticas = zip.stream().anyMatch(entrada ->
                entrada.getName().matches("(?i)Metadata/plate_\\d+\\.statistics\\.json"));
        if (possuiGcode || possuiEstatisticas) return true;
        ZipEntry resumo = zip.getEntry("Metadata/slice_info.config");
        if (resumo == null) return false;
        try {
            String xml = new String(lerLimitado(zip, resumo), java.nio.charset.StandardCharsets.UTF_8);
            return xml.contains("<plate") && xml.contains("prediction");
        } catch (Exception ignored) {
            return false;
        }
    }

    private byte[] lerLimitado(ZipFile zip, ZipEntry entrada) throws IOException {
        return lerLimitado(zip, entrada, MAX_METADATA_BYTES);
    }

    private byte[] lerLimitado(ZipFile zip, ZipEntry entrada, int limite) throws IOException {
        if (entrada.getSize() > limite) throw new IllegalArgumentException("Os metadados do arquivo são grandes demais para analisar.");
        try (var input = zip.getInputStream(entrada); var output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int lidos;
            while ((lidos = input.read(buffer)) != -1) {
                total += lidos;
                if (total > limite) throw new IllegalArgumentException("Os metadados do arquivo são grandes demais para analisar.");
                output.write(buffer, 0, lidos);
            }
            return output.toByteArray();
        }
    }

    private String texto(JsonNode raiz, String chave) {
        if (raiz == null) return "";
        JsonNode valor = raiz.path(chave);
        if (valor.isMissingNode() || valor.isNull()) return "";
        if (valor.isArray()) return vetor(raiz, chave);
        return valor.asText("").trim();
    }

    private String vetor(JsonNode raiz, String chave) {
        if (raiz == null) return "";
        JsonNode valor = raiz.path(chave);
        if (valor.isArray()) {
            StringBuilder combinado = new StringBuilder();
            for (JsonNode item : valor) {
                String texto = item.asText("").trim();
                if (texto.isEmpty()) continue;
                if (!combinado.isEmpty()) combinado.append(", ");
                combinado.append(texto);
            }
            return combinado.toString();
        }
        return valor.isMissingNode() || valor.isNull() ? "" : valor.asText("").trim();
    }

    private String primeiro(String valor, String alternativa) {
        return valor == null || valor.isBlank() ? alternativa : valor;
    }

    private String removerExtensao(String nome) {
        return nome.replaceFirst("(?i)(?:\\.gcode)?\\.3mf$", "").trim();
    }

    private String limitar(String valor, int limite) {
        return valor.length() <= limite ? valor : valor.substring(0, limite).trim();
    }

    private record DocumentoModelo(String nome, String perfil, String fatiador) { }
    private record DadosEstatisticaPlaca(
            double tempoSegundos,
            double pesoGramas,
            List<Modelo3mfFilamentoResponse> filamentos
    ) { }
}
