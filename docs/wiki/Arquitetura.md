# Arquitetura

## Visao de alto nivel

Na V2, o projeto passou a ter dois trilhos:

```text
Fluxo principal local-first

Android Host
  |- endpoint HTTP local
  |- WebSocket local
  |- anuncio NSD/mDNS
  |- QR para console local
  |
  +--> Android cliente entra por descoberta LAN
  +--> Navegador abre console auxiliar por QR/link local

Fluxo avancado de compatibilidade

Android / Web <----HTTP + WS----> Servidor Node de sinalizacao
```

## Componentes

### Android host

Responsavel por:

- subir a sala local
- anunciar a sala na LAN
- criar e entrar em sessao
- controlar PTT quando host
- negociar WebRTC
- exibir QR code
- operar notificacao de sessao

### Servidor Node

Responsavel por:

- manter o fluxo legado/manual
- criar salas no modo avancado
- autenticar peers por token
- encaminhar mensagens de sinalizacao
- preservar compatibilidade entre clientes

### Web app

Responsavel por:

- apresentar o projeto publicamente
- orientar o fluxo de uso
- oferecer laboratorio avancado
- acompanhar salas como console auxiliar
- permitir voz web apenas em modo experimental

### Protocolo compartilhado

Responsavel por:

- unificar requests e responses
- definir mensagens de socket
- explicitar `transportMode`
- explicitar `clientRole`
- explicitar capacidades do cliente e da sala

## Modelo de transporte

O sistema diferencia:

- `local_lan`
- `remote_signaling`

Com isso, a UI e os clientes sabem se a sessao:

- esta no fluxo oficial local
- ou em fluxo de compatibilidade manual

## Modelo de capacidades

Cada peer tem capacidades como:

- `canTransmitAudio`
- `canReceiveAudio`
- `supportsLocalJoin`
- `supportsAdvancedWebRtc`

Cada sala tem capacidades como:

- `allowsConsoleClients`
- `allowsExperimentalWebVoice`
- `localFirst`

## Modelo de sessao

Uma sala contem:

- `roomId`
- `roomCode`
- `roomName`
- `channels`
- `members`
- `activeSpeakerByChannel`
- `hostStatus`
- `transportMode`
- `hostEndpoint`
- `roomCapabilities`
- `eventLog`
- `capacity`

## Modelo de audio

O projeto continua usando WebRTC audio-only.

Mas a distribuicao segue orientada por canal:

- o locutor atual transmite apenas para os peers elegiveis
- peers elegiveis sao os conectados no mesmo canal
- um canal tem um locutor por vez

## Modelo de PTT

O fluxo central segue:

1. cliente envia `talk_request`
2. host decide
3. host responde `talk_granted` ou `talk_denied`
4. ao soltar, cliente envia `talk_release_request`
5. host responde `talk_released`

## Regra operacional importante

O caminho oficial de voz e o APK Android.

A web agora e tratada assim:

- `console_only` quando entra como console auxiliar
- `experimental_web_voice` quando entra pelo laboratorio avancado

Isso evita prometer ao usuario que o navegador substitui a experiencia principal do APK.
