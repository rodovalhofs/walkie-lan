# Frontend Web

## Objetivo da web app

A web app existe para permitir entrada rapida por navegador, com foco especial em Safari/iOS, sem obrigar instalacao de app nativo.

## Stack

- React
- Vite
- `simple-peer`
- WebRTC
- WebSocket
- PWA via `vite-plugin-pwa`

## Estado geral da aplicacao

O arquivo `src/App.tsx` controla:

- URL do servidor
- apelido
- estado de busy
- erro
- sessao ativa
- snapshot
- status de conexao
- microfone pronto
- se esta transmitindo
- mensagem de aviso
- streams remotos

## Tela inicial

Implementada em `src/components/LandingPanel.tsx`.

Ela contem:

- hero principal
- campo de URL do servidor
- campo de apelido
- bloco para criar sala
- bloco para entrar por codigo
- banner de erro

## Tela da sala

Implementada em `src/components/RoomPanel.tsx`.

Ela contem:

- cabecalho da sala
- status de host e conexao
- status de microfone
- status de transmissao
- seletor de canais
- botao de habilitar microfone
- botao grande de PTT
- lista de presenca
- lista de eventos recentes
- `AudioDock` invisivel para os elementos `<audio>`

## Camadas internas

### `src/lib/api.ts`

Faz as chamadas REST:

- `createRoom`
- `joinRoom`

Tambem normaliza a `baseUrl`.

### `src/lib/signalingClient.ts`

Abre o WebSocket, envia `hello` e valida mensagens recebidas com `socketMessageSchema`.

### `src/lib/meshManager.ts`

Gerencia a malha WebRTC no navegador:

- habilita microfone com `getUserMedia`
- cria peers com `simple-peer`
- serializa `offer`
- serializa `answer`
- serializa `ice-candidate`
- normaliza ICE candidate em formatos diferentes
- adiciona e remove trilha de audio conforme os peers elegiveis
- destroi peers desconectados

## PWA

Configurada em `vite.config.ts`.

Hoje a web:

- registra service worker imediatamente
- define manifest
- roda em `display: standalone`
- define `theme_color`
- define `background_color`
- expoe nome e descricao da app

## Modo host web de debug

A web atual ainda pode criar sala em modo de debug.

Nesse modo:

- a web age como host
- concede ou nega PTT
- serve para validar o ecossistema antes do fluxo final centrado no APK Android

## Limitacoes atuais da web

- depende do servidor de apoio
- o melhor caso de uso no iPhone ainda e em primeiro plano
- nao ha descoberta automatica de salas LAN na web
- nao ha UI de configuracoes avancadas
