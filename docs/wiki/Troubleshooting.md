# Troubleshooting

## O telefone nao conecta no servidor

Verifique:

- se o servidor esta rodando
- se voce usou o IP do computador e nao `localhost`
- se ambos estao na mesma rede
- se a porta `8787` esta liberada no firewall

Teste no navegador do telefone:

```text
http://SEU_IP:8787/health
```

## O emulador nao acha o servidor

Use:

```text
http://10.0.2.2:8787
```

`localhost` no emulador nao aponta para o computador host.

## O navegador parece estar com codigo antigo

Tente:

- reiniciar `npm run dev:server`
- reiniciar `npm run dev:web`
- fazer reload forcado
- remover a PWA da tela inicial e abrir de novo

## O Android fecha sozinho

Capture log com `adb`:

```powershell
C:\Users\yurir\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat
```

Pontos que ja exigiram correcao neste projeto:

- permissao e tipo de foreground service
- `wsUrl` apontando para `localhost`
- formatos diferentes de ICE candidate
- campos nulos em envelopes de sinalizacao

## O servidor responde com erro de validacao

O servidor valida payloads com Zod.

Se aparecer `invalid_type` ou outro erro parecido:

- confira se servidor, web e APK estao na mesma versao de codigo
- confira se a web nao esta usando cache antigo
- confira se os campos opcionais nulos estao sendo aceitos pela versao atual do protocolo

## O iPhone nao libera o microfone

Lembretes importantes:

- Safari prefere origem segura HTTPS para uso real do microfone
- a experiencia pensada hoje e em primeiro plano
- para testes locais simples, a web ainda pode funcionar no desktop e em ambientes controlados

## O host saiu e todo mundo caiu

Isso e o comportamento atual esperado.

Hoje:

- se o host desconecta, a sala e encerrada
- nao existe eleicao automatica de novo host
