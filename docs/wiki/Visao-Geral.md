# Visao Geral

## Proposito

O Walkie LAN existe para entregar comunicacao de voz simples em rede local com cara de walkie-talkie:

- criar rapido
- entrar sem conta
- usar canais fixos
- operar com PTT
- funcionar bem em pequenos grupos

## Modelo atual

Na V2, o modelo principal passou a ser `local-first`:

- Android nativo e o host oficial da sala
- a sala pode ser exposta localmente pelo proprio Android
- descoberta local no Android acontece por NSD/mDNS
- navegadores entram como console auxiliar ou laboratorio experimental

O fluxo antigo com backend externo ainda existe, mas ficou no `Modo avancado`.

## Fluxos suportados

### Fluxo principal

1. Android cria a sala
2. Android sobe o endpoint local
3. Android anuncia a sala na LAN
4. Outro Android descobre e entra
5. iPhone ou desktop abrem o console local por QR/link

### Fluxo avancado

1. Servidor Node cria a sala
2. Android ou web entram por URL manual
3. laboratorio web pode atuar como host experimental

## Tipos de cliente

O protocolo reconhece hoje:

- `android_native`
- `ios_web`
- `android_web_debug`

## Papeis de cliente

O protocolo tambem diferencia o papel de cada cliente:

- `full_voice`
- `console_only`
- `experimental_web_voice`

Isso permite que a UI e a logica decidam claramente quem pode:

- transmitir audio
- receber audio
- entrar por fluxo local
- usar WebRTC avancado

## O que ja esta implementado

- host local no Android
- descoberta LAN no Android
- QR code para console auxiliar
- home do APK em modo simples e avancado
- tela operacional de PTT
- seletor de saida de audio no Android
- landing publica nova no site
- laboratorio web isolado do fluxo principal
- protocolo com `transportMode`, `hostEndpoint` e capacidades

## O que ainda nao e foco principal

- deploy publico definitivo
- experiencia web como caminho oficial de voz
- criptografia ponta a ponta
- contas de usuario
- gravacao de audio

## Filosofia tecnica

O projeto privilegia:

- simplicidade
- uso real em LAN
- Android como experiencia principal
- web honesta sobre suas limitacoes
- compatibilidade via protocolo compartilhado
