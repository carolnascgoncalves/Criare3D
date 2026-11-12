package br.com.criare3d.service;

import br.com.criare3d.models.CalculoFoto;
import br.com.criare3d.repository.CalculoFotoRepository;
import br.com.criare3d.repository.CalculoHistoricoRepository;
import java.util.Optional;
import java.util.UUID;

/** Persistência e miniaturas executadas na transação da fachada do histórico. */
@org.springframework.stereotype.Service
@org.springframework.transaction.annotation.Transactional
public class FotoCalculoService {
    private final CalculoFotoRepository calculoFotoRepository;
    private final CalculoHistoricoRepository historicos;

    public FotoCalculoService(CalculoFotoRepository fotos, CalculoHistoricoRepository historicos) {
        this.calculoFotoRepository = fotos;
        this.historicos = historicos;
    }

    public void salvar(UUID id, String tipo, byte[] bytes) {
        if (bytes == null || bytes.length == 0) return;
        var foto = new CalculoFoto();
        foto.setCalculoId(id);
        foto.setTipo(tipo);
        foto.setBytes(bytes);
        calculoFotoRepository.save(foto);
    }

    public Optional<CalculoFoto> buscarFoto(UUID calculoId) {
        if (!historicos.existsById(calculoId)) return Optional.empty();
        return calculoFotoRepository.findById(calculoId);
    }

    public Optional<br.com.criare3d.dto.FotoConteudo> buscarConteudoFoto(UUID calculoId, boolean miniatura) {
        if (!historicos.existsById(calculoId)) return Optional.empty();
        if (miniatura) {
            var miniSalva = calculoFotoRepository.buscarMiniatura(calculoId);
            if (miniSalva.isPresent()) return Optional.of(new br.com.criare3d.dto.FotoConteudo("image/jpeg", miniSalva.get()));
        }
        return calculoFotoRepository.findById(calculoId).map(foto -> {
            if (!miniatura) return new br.com.criare3d.dto.FotoConteudo(foto.getTipo(), foto.getBytes());
            byte[] mini = gerarMiniatura(foto.getBytes());
            if (mini == null) return new br.com.criare3d.dto.FotoConteudo(foto.getTipo(), foto.getBytes());
            foto.setMiniatura(mini);
            calculoFotoRepository.save(foto);
            return new br.com.criare3d.dto.FotoConteudo("image/jpeg", mini);
        });
    }

    private byte[] gerarMiniatura(byte[] originalBytes) {
        try {
            java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(
                    new java.io.ByteArrayInputStream(originalBytes));
            if (original == null) return null;
            double escala = Math.min(1d, 320d / Math.max(original.getWidth(), original.getHeight()));
            int largura = Math.max(1, (int) Math.round(original.getWidth() * escala));
            int altura = Math.max(1, (int) Math.round(original.getHeight() * escala));
            java.awt.image.BufferedImage mini = new java.awt.image.BufferedImage(
                    largura, altura, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = mini.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, largura, altura);
            g.drawImage(original, 0, 0, largura, altura, null);
            g.dispose();
            java.io.ByteArrayOutputStream saida = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(mini, "jpg", saida);
            return saida.toByteArray();
        } catch (java.io.IOException ignored) {
            return null;
        }
    }

}
