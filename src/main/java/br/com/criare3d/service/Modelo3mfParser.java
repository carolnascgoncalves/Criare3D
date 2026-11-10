package br.com.criare3d.service;

import br.com.criare3d.dto.Modelo3mfResponse;
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
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class Modelo3mfParser {
    private static final long MAX_UPLOAD_BYTES = 20L * 1024 * 1024;
    private static final int MAX_METADATA_BYTES = 4 * 1024 * 1024;
    private static final int MAX_PREVIEW_BYTES = 2 * 1024 * 1024;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public Modelo3mfResponse analisar(MultipartFile arquivo) throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Selecione um arquivo .3mf.");
        }
        if (arquivo.getSize() > MAX_UPLOAD_BYTES) {
            throw new IllegalArgumentException("O arquivo deve ter no máximo 20 MB.");
        }
        String nomeArquivo = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename();
        if (!nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".3mf")
                || nomeArquivo.toLowerCase(Locale.ROOT).endsWith(".gcode.3mf")) {
            throw new IllegalArgumentException("Envie o projeto .3mf do modelo. Arquivos .gcode.3mf serão tratados em uma etapa posterior.");
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
                boolean jaFatiado = temResultadoFatiamento(zip);
                String imagemPreview = lerImagemPreview(zip);

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
                        imagemPreview
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

    private String lerImagemPreview(ZipFile zip) {
        String[] prioritarios = {
                "Metadata/plate_1.png",
                "Metadata/plate_1_small.png",
                "Auxiliaries/.thumbnails/thumbnail_middle.png",
                "Auxiliaries/.thumbnails/thumbnail_3mf.png"
        };
        for (String caminho : prioritarios) {
            String dataUri = imagemComoDataUri(zip, zip.getEntry(caminho));
            if (dataUri != null) return dataUri;
        }
        Enumeration<? extends ZipEntry> entradas = zip.entries();
        while (entradas.hasMoreElements()) {
            ZipEntry entrada = entradas.nextElement();
            String caminho = entrada.getName().toLowerCase(Locale.ROOT);
            if (caminho.startsWith("auxiliaries/model pictures/")
                    || caminho.startsWith("metadata/")) {
                String dataUri = imagemComoDataUri(zip, entrada);
                if (dataUri != null) return dataUri;
            }
        }
        return "";
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
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document documento = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
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
        return nome.replaceFirst("(?i)\\.3mf$", "").trim();
    }

    private String limitar(String valor, int limite) {
        return valor.length() <= limite ? valor : valor.substring(0, limite).trim();
    }

    private record DocumentoModelo(String nome, String perfil, String fatiador) { }
}
