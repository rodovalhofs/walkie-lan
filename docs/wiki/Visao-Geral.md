# Visao Geral

## Proposito

O Walkie LAN existe para entregar uma comunicacao de voz simples em rede local, com cara de walkie-talkie:

- codigo curto para entrar na sala
- canais fixos
- push-to-talk
- pouca friccao para teste

## Modelo atual

O modelo implementado hoje e hibrido:

- Android nativo e o host principal
- o servidor faz bootstrap e sinalizacao
- a web app entra como cliente e troca audio por WebRTC

Isso significa que o Android hospeda a sessao e as decisoes de PTT, mas nao elimina totalmente o backend.

## Porque existe um servidor

O servidor atual resolve problemas importantes:

- cria a sala e gera o codigo curto
- autoriza a entrada por codigo
- entrega o `wsUrl` correto para cada cliente
- autentica a conexao WebSocket inicial
- retransmite mensagens de sinalizacao e eventos de sessao

## Tipos de cliente

O protocolo reconhece hoje tres tipos:

- `android_native`
- `ios_web`
- `android_web_debug`

## O que ja esta implementado

- criacao de sala via API
- entrada em sala via codigo curto
- snapshot de sala
- eventos de presenca
- eventos de troca de canal
- eventos de inicio e fim de fala
- fluxo de `talk_request`, `talk_granted`, `talk_denied` e `talk_released`
- WebRTC com `offer`, `answer` e `ice-candidate`
- app Android com Compose
- web app em React
- PWA basica para a web

## O que ainda nao existe

- contas de usuario
- lista de salas publica
- descoberta LAN no cliente web
- gravacao de audio
- chat de texto
- moderacao com papeis
- deploy automatizado de producao

## Filosofia tecnica

O projeto privilegia:

- simplicidade
- facilidade de teste local
- codigo compartilhado no protocolo
- compatibilidade entre Android e navegador

## Capacidade atual

O limite de sala definido hoje no protocolo e `10` participantes.

O limite de eventos retidos no snapshot e `200`.

Os canais padrao sao:

- `Geral`
- `Operacao`
- `Suporte`
