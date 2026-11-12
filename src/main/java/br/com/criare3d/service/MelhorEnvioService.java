package br.com.criare3d.service;

import br.com.criare3d.dto.FreteRequest;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.math.*;
import java.util.*;

@Service
public class MelhorEnvioService {
 private final JdbcTemplate jdbc;private final UsuarioLogadoService usuario;private final Clock clock;
 private final String clientId,secret,callback,ambiente,agente,base;private final SegredoIntegracao cofre;
 private final JsonMapper json=JsonMapper.builder().build();
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 public MelhorEnvioService(JdbcTemplate jdbc,UsuarioLogadoService usuario,Clock clock,
  @Value("${app.melhor-envio.client-id:}")String clientId,@Value("${app.melhor-envio.client-secret:}")String secret,
  @Value("${app.melhor-envio.callback:}")String callback,@Value("${app.melhor-envio.ambiente:sandbox}")String ambiente,
  @Value("${app.melhor-envio.email:}")String email,@Value("${app.melhor-envio.chave:}")String chave){
  this.jdbc=jdbc;this.usuario=usuario;this.clock=clock;this.clientId=clientId;this.secret=secret;this.callback=callback;
  if(!Set.of("sandbox","producao").contains(ambiente))throw new IllegalArgumentException("Ambiente Melhor Envio inválido.");
  this.ambiente=ambiente;this.base=ambiente.equals("sandbox")?"https://sandbox.melhorenvio.com.br":"https://melhorenvio.com.br";
  this.agente="Criare3D ("+email.replaceAll("[\\r\\n]","")+")";this.cofre=chave.isBlank()?null:new SegredoIntegracao(chave);
 }
 public boolean configurado(){return !clientId.isBlank()&&!secret.isBlank()&&!callback.isBlank()&&cofre!=null&&agente.contains("@");}
 public String ambiente(){return ambiente;}
 public String callback(){return callback;}
 private UUID tenant(){return Objects.requireNonNull(usuario.buscarTenantIdAtual());}
 public boolean conectado(){return configurado()&&Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM melhor_envio_conexao WHERE tenant_id=? AND ambiente=?)",Boolean.class,tenant(),ambiente));}
 public String autorizar(String state){exigirConfiguracao();return base+"/oauth/authorize?client_id="+enc(clientId)+"&redirect_uri="+enc(callback)+"&response_type=code&scope=shipping-calculate&state="+enc(state);}
 @Transactional public void conectar(String code){exigirConfiguracao();var resposta=token(Map.of("grant_type","authorization_code","code",code,"redirect_uri",callback));salvar(resposta);}
 @Transactional public void desconectar(){jdbc.update("DELETE FROM melhor_envio_conexao WHERE tenant_id=? AND ambiente=?",tenant(),ambiente);}
 private void salvar(JsonNode r){
  String acesso=r.path("access_token").asText(),refresh=r.path("refresh_token").asText();long segundos=r.path("expires_in").asLong(2592000);
  if(acesso.isBlank()||refresh.isBlank()||segundos<60)throw new IllegalArgumentException("A autorização não retornou uma conexão válida.");
  var id=tenant();String contexto=id+":"+ambiente;
  jdbc.update("INSERT INTO melhor_envio_conexao(tenant_id,ambiente,acesso,renovacao,expira_em) VALUES (?,?,?,?,?) ON CONFLICT(tenant_id,ambiente) DO UPDATE SET acesso=EXCLUDED.acesso,renovacao=EXCLUDED.renovacao,expira_em=EXCLUDED.expira_em",id,ambiente,cofre.cifrar(acesso,contexto),cofre.cifrar(refresh,contexto),java.sql.Timestamp.from(clock.instant().plusSeconds(Math.min(segundos,2592000))));
 }
 private JsonNode token(Map<String,String> dados){var campos=new HashMap<>(dados);campos.put("client_id",clientId);campos.put("client_secret",secret);String corpo=campos.entrySet().stream().map(e->enc(e.getKey())+"="+enc(e.getValue())).reduce((a,b)->a+"&"+b).orElseThrow();return chamar("/oauth/token",corpo,null,true);}
 @Transactional(noRollbackFor=IllegalArgumentException.class) public List<Opcao> cotar(FreteRequest d,BigDecimal valor){
  exigirConfiguracao();var id=tenant();var linhas=jdbc.query("SELECT acesso,renovacao,expira_em FROM melhor_envio_conexao WHERE tenant_id=? AND ambiente=? FOR UPDATE",(rs,n)->new Conexao(rs.getString(1),rs.getString(2),rs.getTimestamp(3).toInstant()),id,ambiente);
  if(linhas.isEmpty())throw new IllegalArgumentException("Conecte sua conta do Melhor Envio antes de calcular.");
  var c=linhas.getFirst();String contexto=id+":"+ambiente;String acesso;
  if(c.expira().isBefore(clock.instant().plusSeconds(120))){var novo=token(Map.of("grant_type","refresh_token","refresh_token",cofre.decifrar(c.renovacao(),contexto)));salvar(novo);acesso=novo.path("access_token").asText();}else acesso=cofre.decifrar(c.acesso(),contexto);
  var volume=Map.of("width",d.getLargura(),"height",d.getAltura(),"length",d.getComprimento(),"weight",d.getPeso(),"insurance",valor.setScale(2,RoundingMode.HALF_UP));
  var corpo=Map.of("from",Map.of("postal_code",d.getOrigem().replace("-","")),"to",Map.of("postal_code",d.getDestino().replace("-","")),"volumes",List.of(volume),"options",Map.of("receipt",false,"own_hand",false));
  return opcoes(chamar("/api/v2/me/shipment/calculate",json.writeValueAsString(corpo),acesso,false));
 }
 public static List<Opcao> opcoes(JsonNode resposta){
  var lista=new ArrayList<Opcao>();if(!resposta.isArray())throw new IllegalArgumentException("O Melhor Envio não retornou opções para esse pacote.");
  for(var r:resposta){if(!r.path("error").asText().isBlank())continue;try{String preco=r.path("custom_price").asText();if(preco.isBlank())preco=r.path("price").asText();var valor=new BigDecimal(preco);if(valor.signum()<0)continue;int dias=r.path("custom_delivery_time").asInt(r.path("delivery_time").asInt());lista.add(new Opcao(r.path("company").path("name").asText()+" · "+r.path("name").asText(),valor.setScale(2,RoundingMode.HALF_UP),dias));}catch(NumberFormatException ignorado){}}
  if(lista.isEmpty())throw new IllegalArgumentException("Nenhum serviço disponível. Confira CEPs, peso e medidas.");return List.copyOf(lista);
 }
 private JsonNode chamar(String caminho,String corpo,String acesso,boolean formulario){
  try{var b=HttpRequest.newBuilder(URI.create(base+caminho)).timeout(Duration.ofSeconds(15)).header("User-Agent",agente).header("Accept","application/json").header("Content-Type",formulario?"application/x-www-form-urlencoded":"application/json").POST(HttpRequest.BodyPublishers.ofString(corpo));if(acesso!=null)b.header("Authorization","Bearer "+acesso);
   var r=http.send(b.build(),HttpResponse.BodyHandlers.ofInputStream());try(var stream=r.body()){byte[] bytes=stream.readNBytes(262145);if(bytes.length>262144)throw new IllegalArgumentException("A resposta do Melhor Envio excedeu o limite.");if(r.statusCode()<200||r.statusCode()>299)throw new IllegalArgumentException((r.statusCode()==401 || formulario)?"A conexão expirou ou foi revogada. Conecte o Melhor Envio novamente.":"O Melhor Envio não aceitou a consulta. Confira os dados e tente novamente.");return json.readTree(bytes);}
  }catch(tools.jackson.core.JacksonException e){throw new IllegalArgumentException("O Melhor Envio retornou uma resposta inválida. Tente novamente.");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalArgumentException("A consulta foi interrompida. Tente novamente.");}catch(java.io.IOException e){throw new IllegalArgumentException("Não foi possível consultar o Melhor Envio agora. Tente novamente ou use o frete manual.");}
 }
 private void exigirConfiguracao(){if(!configurado())throw new IllegalArgumentException("A integração Melhor Envio ainda precisa ser configurada no servidor.");}
 private static String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
 private record Conexao(String acesso,String renovacao,Instant expira){}
 public record Opcao(String nome,BigDecimal preco,int dias){}
}
