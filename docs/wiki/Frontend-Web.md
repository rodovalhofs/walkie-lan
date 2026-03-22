# Frontend Web

## Novo papel da web

Na V2, a web deixou de ser apresentada como caminho principal de voz.

Hoje ela existe em duas camadas:

- site publico
- laboratorio avancado

## Site publico

A landing publica foi redesenhada para:

- explicar que o Android e o host oficial
- orientar o fluxo local-first
- mostrar que o navegador entra melhor como console auxiliar
- apontar para a wiki e para o modo avancado apenas quando necessario

Arquivos principais:

- `src/App.tsx`
- `src/components/LandingPanel.tsx`
- `src/styles.css`

## Laboratorio avancado

O laboratorio avancado concentra o que ficou fora do fluxo principal:

- URL manual do servidor
- host web experimental
- entrada por codigo como `console_only`
- entrada por codigo como `experimental_web_voice`

Isso reduz confusao para quem abre o site pela primeira vez.

## Tela da sala

A tela de sala agora diferencia melhor o papel do navegador:

- se entrou como `console_only`, a UI assume console auxiliar
- se entrou como `experimental_web_voice`, a UI permite fluxo experimental de audio
- o status da sala mostra transporte, endpoint local e contexto da sessao

Arquivo principal:

- `src/components/RoomPanel.tsx`

## Audio na web

A web continua usando:

- WebRTC
- WebSocket
- `simple-peer`

Mas a promessa mudou:

- receber audio e acompanhar sala continuam validos
- falar pela web existe apenas em laboratorio experimental
- o navegador nao substitui o APK Android como caminho oficial

## Seletor de saida de audio

Quando o navegador suporta:

- `selectAudioOutput`
- `setSinkId`

a web permite escolher a saida de audio.

Quando nao suporta, mostra que esta usando a saida padrao do aparelho.

## PWA

O PWA continua existindo como apoio:

- abrir rapido o site publico
- abrir laboratorio avancado
- acompanhar sala no console auxiliar

## O que a web nao faz no caminho principal

- hospedar a experiencia oficial
- substituir descoberta LAN no Android
- prometer PTT robusto no iPhone
- esconder limitacoes do navegador
