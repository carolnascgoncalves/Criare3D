# Melhor Envio no Criare3D

A integração implementa autorização por empresa, renovação de tokens e cotação de um pacote completo. A seleção preenche custo de envio e frete cobrado; o valor cobrado pode ser ajustado depois. Não compra etiquetas.

## Configurar

No aplicativo cadastrado em Integrações → Área Dev., solicite somente `shipping-calculate`. Configure as variáveis privadas no IntelliJ/IDE e no Render, sem colocá-las no Git:

```properties
MELHOR_ENVIO_AMBIENTE=sandbox
MELHOR_ENVIO_CLIENT_ID=ID_DO_APLICATIVO_SANDBOX
MELHOR_ENVIO_CLIENT_SECRET=SECRET_DO_APLICATIVO_SANDBOX
MELHOR_ENVIO_CALLBACK=https://criare3d.onrender.com/integracoes/melhor-envio/callback
MELHOR_ENVIO_EMAIL=SEU_EMAIL_TECNICO
MELHOR_ENVIO_CHAVE=CHAVE_BASE64_DE_32_BYTES
```

Para gerar a chave, no PowerShell:

```powershell
$chaveMelhorEnvio = New-Object byte[] 32
$geradorMelhorEnvio = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $geradorMelhorEnvio.GetBytes($chaveMelhorEnvio) } finally { $geradorMelhorEnvio.Dispose() }
[Convert]::ToBase64String($chaveMelhorEnvio)
```

Guarde a chave com as outras configurações privadas, inclusive para restaurar backups. Ela protege os tokens gravados no banco; não a regenere a cada inicialização. A chave nunca deve ir para JavaScript ou templates.

## Testar no localhost

O endereço de retorno precisa ser exatamente o cadastrado no aplicativo, e deve voltar ao mesmo servidor/navegador onde você iniciou a conexão. O aplicativo cadastrado com callback do Render devolve a autorização ao Render, não ao localhost.

Para desenvolvimento, use um aplicativo Sandbox separado e cadastre como callback `http://localhost:8080/integracoes/melhor-envio/callback`, caso o painel aceite endereço HTTP local. Use esse mesmo valor em `MELHOR_ENVIO_CALLBACK` no IntelliJ. Se o painel exigir HTTPS, use uma URL HTTPS de túnel que encaminhe para localhost:8080 e cadastre essa URL completa, com o mesmo caminho. Não use a URL do Render como ponte para o localhost.

Conta e aplicativo Sandbox são distintos dos de produção. Para fretes reais, crie/configure o aplicativo de produção e use `MELHOR_ENVIO_AMBIENTE=producao`. Tokens de ambientes diferentes ficam separados.

## Usar

1. Inicie o sistema para aplicar V38 e entre em sua conta.
2. Abra `/integracoes/melhor-envio` e clique em Conectar Melhor Envio.
3. Autorize a cotação na conta correspondente ao ambiente.
4. No orçamento, etapa Preço, abra Calcular com frete.
5. Informe CEP de origem/destino, peso completo em **kg** e medidas externas em **cm**.
6. Consulte e escolha um serviço. Confira o custo e o valor cobrado antes de enviar a proposta.

A cotação selecionada expira após dez minutos e é invalidada se o orçamento mudar. As permissões não incluem pagamentos ou etiquetas. Desconectar remove a conexão local; a autorização também pode ser revogada no painel do Melhor Envio.

## Conferência

- Conexão deve voltar ao ambiente de origem, sem aceitar autorizações iniciadas por outro usuário/empresa ou um `state` alterado.
- CEP inválido deve ser recusado sem consulta externa. Serviço indisponível não deve virar preço zero.
- Escolher o frete deve preencher os dois valores, sem comprar nada.
- Outra conta não pode usar a cotação ou os tokens da primeira.
- Renovar token deve preservar a conexão mesmo se uma cotação posterior falhar.
- Sem configuração, a tela deve explicar o que falta e o frete manual deve continuar funcionando.

Referências oficiais: [autorização](https://docs.melhorenvio.com.br/reference/fluxo-de-autoriza%C3%A7%C3%A3o), [cotação de fretes e pacotes](https://docs.melhorenvio.com.br/reference/calculo-de-fretes-por-produtos).
