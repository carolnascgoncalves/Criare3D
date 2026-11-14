package br.com.criare3d.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;

@Service
public class IdempotenciaService {
 @Autowired private JdbcTemplate jdbc;
 @Autowired private UsuarioLogadoService usuario;
 @Transactional
 public String executar(UUID chave,String operacao,String parametros,Supplier<String> executar) {
  Objects.requireNonNull(chave,"Identificação da operação obrigatória.");UUID tenant=usuario.buscarTenantIdAtual();String resumo;
  try{resumo=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((operacao+"|"+parametros).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  int criada=jdbc.update("INSERT INTO operacao_confirmada(tenant_id,chave,resumo,criado_em) VALUES(?,?,?,CURRENT_TIMESTAMP) ON CONFLICT(tenant_id,chave) DO NOTHING",tenant,chave,resumo);
  if(criada==0){var anterior=jdbc.queryForMap("SELECT resumo,resultado FROM operacao_confirmada WHERE tenant_id=? AND chave=?",tenant,chave);
   if(!resumo.equals(anterior.get("resumo")))throw new IllegalArgumentException("Esta confirmação já foi usada para outra venda. Reabra o formulário.");
   if(anterior.get("resultado")==null)throw new IllegalStateException("Aguarde a confirmação anterior.");return anterior.get("resultado").toString();}
  String resultado=executar.get();jdbc.update("UPDATE operacao_confirmada SET resultado=? WHERE tenant_id=? AND chave=?",resultado,tenant,chave);return resultado;
 }
}
