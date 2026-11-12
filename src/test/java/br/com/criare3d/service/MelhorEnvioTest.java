package br.com.criare3d.service;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class MelhorEnvioTest {
 @Test void tokensUsamNonceAleatorioEVinculoComTenant(){
  var cofre=new SegredoIntegracao(Base64.getEncoder().encodeToString(new byte[32]));var a=cofre.cifrar("token-privado","tenantA:sandbox");var b=cofre.cifrar("token-privado","tenantA:sandbox");
  assertThat(a).isNotEqualTo(b).doesNotContain("token-privado");assertThat(cofre.decifrar(a,"tenantA:sandbox")).isEqualTo("token-privado");
  assertThatIllegalArgumentException().isThrownBy(()->cofre.decifrar(a,"tenantB:sandbox"));
 }
 @Test void respeitaPrecoPersonalizadoEIgnoraTransportadoraSemServico(){
  var r=JsonMapper.builder().build().readTree("""
   [{"name":"SEDEX","company":{"name":"Correios"},"price":"30.00","custom_price":"25.71","delivery_time":5,"custom_delivery_time":3},
    {"name":"Indisponível","error":"fora de cobertura"}]
   """);var opcoes=MelhorEnvioService.opcoes(r);assertThat(opcoes).hasSize(1);assertThat(opcoes.getFirst().preco()).isEqualByComparingTo("25.71");assertThat(opcoes.getFirst().dias()).isEqualTo(3);
 }
 @Test void respostaSemPrecoValidoNaoViraFreteGratis(){assertThatIllegalArgumentException().isThrownBy(()->MelhorEnvioService.opcoes(JsonMapper.builder().build().readTree("[{\"name\":\"SEDEX\"}]")));}
}
