package br.com.criare3d.service;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/** Tokens nunca são persistidos em texto aberto. O tenant compõe os dados autenticados. */
public final class SegredoIntegracao {
 private final byte[] chave;
 public SegredoIntegracao(String base64){chave=Base64.getDecoder().decode(base64);if(chave.length!=32)throw new IllegalArgumentException("A chave da integração deve ter 32 bytes em Base64.");}
 public String cifrar(String texto,String contexto){try{byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);var c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(chave,"AES"),new GCMParameterSpec(128,nonce));c.updateAAD(contexto.getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(nonce)+"."+Base64.getEncoder().encodeToString(c.doFinal(texto.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Não foi possível proteger a conexão.");}}
 public String decifrar(String texto,String contexto){try{var p=texto.split("\\.");var c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(chave,"AES"),new GCMParameterSpec(128,Base64.getDecoder().decode(p[0])));c.updateAAD(contexto.getBytes(StandardCharsets.UTF_8));return new String(c.doFinal(Base64.getDecoder().decode(p[1])),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalArgumentException("Não foi possível ler a conexão. Conecte o Melhor Envio novamente.");}}
}
