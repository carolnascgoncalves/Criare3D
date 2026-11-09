package br.com.criare3d.service;

import br.com.criare3d.dto.ClienteAdminRequest;
import br.com.criare3d.dto.ClienteAdminResumo;
import br.com.criare3d.models.Tenant;
import br.com.criare3d.models.Usuario;
import br.com.criare3d.models.enums.Perfil;
import br.com.criare3d.repository.TenantRepository;
import br.com.criare3d.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class ClienteAdminService {
    private static final int LOGO_MAX_BYTES = 1_000_000;
    private static final int LOGO_MAX_LADO = 1024;
    private static final SecureRandom GERADOR = new SecureRandom();

    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public ClienteAdminService(TenantRepository tenantRepository,
                               UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder,
                               JdbcTemplate jdbcTemplate) {
        this.tenantRepository = tenantRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public List<ClienteAdminResumo> listar() {
        return tenantRepository.listarClientesAdmin(Perfil.USER);
    }

    @Transactional(readOnly = true)
    public ClienteAdminRequest carregarFormulario(UUID id) {
        Tenant tenant = buscarTenant(id);
        Usuario usuario = usuarioRepository.findByTenant_IdAndPerfil(id, Perfil.USER)
                .orElseThrow(() -> new IllegalStateException("Este cliente ainda não possui uma conta de acesso."));

        ClienteAdminRequest request = new ClienteAdminRequest();
        request.setNome(tenant.getNome());
        request.setSlug(tenant.getSlug());
        request.setEmail(usuario.getEmail());
        request.setCorPrimaria(tenant.getCorPrimaria());
        request.setCorDestaque(tenant.getCorDestaque());
        request.setCorFundo(tenant.getCorFundo());
        request.setAtivo(tenant.isAtivo());
        return request;
    }

    @Transactional(readOnly = true)
    public boolean temLogo(UUID id) {
        Tenant tenant = buscarTenant(id);
        return tenant.getLogo() != null && tenant.getLogo().length > 0;
    }

    public ClienteCriado criar(ClienteAdminRequest request, MultipartFile arquivoLogo) {
        normalizar(request);
        validarTema(request);
        validarNovoSlug(request.getSlug());
        validarNovoEmail(request.getEmail());

        Tenant tenant = new Tenant();
        atualizarTema(tenant, request);
        tenant.setSlug(request.getSlug());
        tenant.setAtivo(request.isAtivo());
        tenant.setTemaVersao(1L);
        if (arquivoPresente(arquivoLogo)) tenant.setLogo(lerLogoPng(arquivoLogo));
        Tenant tenantSalvo = tenantRepository.saveAndFlush(tenant);

        String senhaTemporaria = gerarSenhaTemporaria();
        Usuario usuario = Usuario.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(senhaTemporaria))
                .perfil(Perfil.USER)
                .trocaSenhaObrigatoria(true)
                .tenant(tenantSalvo)
                .build();
        usuarioRepository.save(usuario);

        return new ClienteCriado(tenantSalvo.getId(), senhaTemporaria);
    }

    public void atualizar(UUID id, ClienteAdminRequest request, MultipartFile arquivoLogo,
                          boolean removerLogo) {
        normalizar(request);
        validarTema(request);
        Tenant tenant = buscarTenant(id);
        validarSlugParaEdicao(request.getSlug(), tenant);

        Usuario usuario = usuarioRepository.findByTenant_IdAndPerfil(id, Perfil.USER)
                .orElseThrow(() -> new IllegalStateException("Este cliente ainda não possui uma conta de acesso."));
        validarEmailParaEdicao(request.getEmail(), usuario);

        byte[] logoAnterior = tenant.getLogo();
        byte[] logoNovo = logoAnterior;
        if (removerLogo) logoNovo = null;
        else if (arquivoPresente(arquivoLogo)) logoNovo = lerLogoPng(arquivoLogo);

        boolean alterouIdentidade = !mesmo(tenant.getNome(), request.getNome())
                || !mesmo(tenant.getCorPrimaria(), request.getCorPrimaria())
                || !mesmo(tenant.getCorDestaque(), request.getCorDestaque())
                || !mesmo(tenant.getCorFundo(), request.getCorFundo())
                || !Arrays.equals(logoAnterior, logoNovo);

        tenant.setNome(request.getNome());
        tenant.setSlug(request.getSlug());
        tenant.setCorPrimaria(request.getCorPrimaria());
        tenant.setCorDestaque(request.getCorDestaque());
        tenant.setCorFundo(request.getCorFundo());
        tenant.setAtivo(request.isAtivo());
        tenant.setLogo(logoNovo);
        if (alterouIdentidade) tenant.setTemaVersao(Math.incrementExact(tenant.getTemaVersao()));

        usuario.setEmail(request.getEmail());
        tenantRepository.save(tenant);
        usuarioRepository.save(usuario);
    }

    public boolean alternarAtivo(UUID id) {
        Tenant tenant = buscarTenant(id);
        tenant.setAtivo(!tenant.isAtivo());
        tenantRepository.save(tenant);
        return tenant.isAtivo();
    }

    public String excluir(UUID id, String nomeConfirmacao) {
        Tenant tenant = buscarTenant(id);
        if (nomeConfirmacao == null || !tenant.getNome().equals(nomeConfirmacao.trim())) {
            throw new IllegalArgumentException("O nome digitado não corresponde ao cliente. Nenhum dado foi apagado.");
        }

        Usuario usuario = usuarioRepository.findByTenant_IdAndPerfil(id, Perfil.USER)
                .orElseThrow(() -> new IllegalStateException("Não foi possível localizar a conta deste cliente."));

        // Usa SQL explícito com tenant_id porque o ADMIN não possui tenant e o filtro
        // Hibernate @TenantId não deve ser desativado para executar esta operação.
        jdbcTemplate.update("DELETE FROM calculo_foto WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM item_calculo_historico WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM manutencao_impressora WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM calculo_historico WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM configuracao WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM embalagem WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM filamento WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM impressora WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM peca WHERE tenant_id = ?", id);
        jdbcTemplate.update("DELETE FROM usuario WHERE id = ? AND tenant_id = ?", usuario.getId(), id);
        jdbcTemplate.update("DELETE FROM tenant WHERE id = ?", id);
        return tenant.getNome();
    }

    public String gerarNovaSenhaTemporaria(UUID id) {
        buscarTenant(id);
        Usuario usuario = usuarioRepository.findByTenant_IdAndPerfil(id, Perfil.USER)
                .orElseThrow(() -> new IllegalStateException("Este cliente ainda não possui uma conta de acesso."));
        String senhaTemporaria = gerarSenhaTemporaria();
        usuario.setPassword(passwordEncoder.encode(senhaTemporaria));
        usuario.setTrocaSenhaObrigatoria(true);
        usuarioRepository.save(usuario);
        return senhaTemporaria;
    }

    @Transactional(readOnly = true)
    public LogoCliente buscarLogo(UUID id) {
        byte[] logo = buscarTenant(id).getLogo();
        if (logo == null || logo.length == 0) {
            throw new IllegalArgumentException("Este cliente não possui um logo válido.");
        }
        return new LogoCliente(logo, tipoImagem(logo));
    }

    private Tenant buscarTenant(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
    }

    private void normalizar(ClienteAdminRequest request) {
        if (request.getNome() != null) request.setNome(request.getNome().trim());
        if (request.getSlug() != null) request.setSlug(request.getSlug().trim().toLowerCase(Locale.ROOT));
        if (request.getEmail() != null) request.setEmail(request.getEmail().trim().toLowerCase(Locale.ROOT));
        if (request.getCorPrimaria() != null) request.setCorPrimaria(request.getCorPrimaria().toLowerCase(Locale.ROOT));
        if (request.getCorDestaque() != null) request.setCorDestaque(request.getCorDestaque().toLowerCase(Locale.ROOT));
        if (request.getCorFundo() != null) request.setCorFundo(request.getCorFundo().toLowerCase(Locale.ROOT));
    }

    private void validarTema(ClienteAdminRequest request) {
        if (!corValida(request.getCorPrimaria()) || !corValida(request.getCorFundo())) return;
        if (contraste(request.getCorPrimaria(), request.getCorFundo()) < 4.5) {
            throw new IllegalArgumentException(
                    "A cor principal e a cor de fundo precisam ter contraste de pelo menos 4,5:1 para o texto ficar legível.");
        }
    }

    private void validarNovoSlug(String slug) {
        if (tenantRepository.existsBySlug(slug)) throw new IllegalArgumentException("Este identificador já está em uso.");
    }

    private void validarSlugParaEdicao(String slug, Tenant atual) {
        if (!slug.equals(atual.getSlug()) && tenantRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("Este identificador já está em uso.");
        }
    }

    private void validarNovoEmail(String email) {
        if (usuarioRepository.existsByEmail(email)) throw new IllegalArgumentException("Este e-mail já está cadastrado.");
    }

    private void validarEmailParaEdicao(String email, Usuario atual) {
        if (!email.equalsIgnoreCase(atual.getEmail()) && usuarioRepository.existsByEmailAndIdNot(email, atual.getId())) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }
    }

    private void atualizarTema(Tenant tenant, ClienteAdminRequest request) {
        tenant.setNome(request.getNome());
        tenant.setCorPrimaria(request.getCorPrimaria());
        tenant.setCorDestaque(request.getCorDestaque());
        tenant.setCorFundo(request.getCorFundo());
    }

    private boolean arquivoPresente(MultipartFile arquivo) {
        return arquivo != null && !arquivo.isEmpty();
    }

    private byte[] lerLogoPng(MultipartFile arquivo) {
        if (arquivo.getSize() > LOGO_MAX_BYTES) {
            throw new IllegalArgumentException("O logo deve ter no máximo 1 MB.");
        }
        try {
            byte[] bytes = arquivo.getBytes();
            tipoImagem(bytes);
            validarDimensoesImagem(bytes);
            return bytes;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo do logo.");
        }
    }

    private void validarDimensoesImagem(byte[] bytes) throws IOException {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (entrada == null) throw new IllegalArgumentException("Envie uma imagem PNG ou JPG/JPEG válida.");
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) throw new IllegalArgumentException("Envie uma imagem PNG ou JPG/JPEG válida.");
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                int largura = leitor.getWidth(0);
                int altura = leitor.getHeight(0);
                if (largura < 1 || altura < 1 || largura > LOGO_MAX_LADO || altura > LOGO_MAX_LADO) {
                    throw new IllegalArgumentException("O logo deve ter no máximo 1024 × 1024 pixels.");
                }
            } finally {
                leitor.dispose();
            }
        }
    }

    private org.springframework.http.MediaType tipoImagem(byte[] bytes) {
        byte[] assinatura = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length >= assinatura.length) {
            boolean png = true;
            for (int i = 0; i < assinatura.length; i++) png &= bytes[i] == assinatura[i];
            if (png) return org.springframework.http.MediaType.IMAGE_PNG;
        }
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF) {
            return org.springframework.http.MediaType.IMAGE_JPEG;
        }
        throw new IllegalArgumentException("Envie uma imagem PNG ou JPG/JPEG válida.");
    }

    private boolean corValida(String cor) {
        return cor != null && cor.matches("#[0-9a-f]{6}");
    }

    private double contraste(String corA, String corB) {
        double a = luminancia(corA);
        double b = luminancia(corB);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    private double luminancia(String hex) {
        double r = canalLinear(Integer.parseInt(hex.substring(1, 3), 16));
        double g = canalLinear(Integer.parseInt(hex.substring(3, 5), 16));
        double b = canalLinear(Integer.parseInt(hex.substring(5, 7), 16));
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private double canalLinear(int valor) {
        double canal = valor / 255.0;
        return canal <= 0.04045 ? canal / 12.92 : Math.pow((canal + 0.055) / 1.055, 2.4);
    }

    private String gerarSenhaTemporaria() {
        byte[] bytes = new byte[24];
        GERADOR.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean mesmo(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    public record ClienteCriado(UUID tenantId, String senhaTemporaria) { }
    public record LogoCliente(byte[] bytes, org.springframework.http.MediaType tipo) { }
}
