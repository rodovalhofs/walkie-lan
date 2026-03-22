# Android Host

## Papel do app Android

O APK Android e o centro da experiencia principal do Walkie LAN.

Ele agora faz muito mais do que a versao inicial:

- cria sala local
- sobe endpoint HTTP/WebSocket local
- anuncia a sala na LAN
- descobre salas locais
- entra em salas pela descoberta
- exibe QR para console auxiliar
- opera PTT
- troca saida de audio

## Arquivos principais

### `MainViewModel.kt`

E o centro da orquestracao do app. Hoje ele:

- carrega preferencias locais
- inicia descoberta LAN
- cria sala local com `LocalHostRuntime`
- entra em sala por descoberta
- mantem o fluxo legado do modo avancado
- gerencia sessao, snapshot e mensagens de socket
- automatiza o host para `talk_request` e `talk_release_request`

### `WalkieScreen.kt`

Define a interface Compose em dois blocos:

- tela de setup com `Modo simples` e `Modo avancado`
- tela ativa de operacao

Na tela ativa o usuario ve:

- nome da sala
- codigo grande
- status do host
- participantes
- QR do console
- canais
- PTT central
- saida de audio
- eventos

### `UiState.kt`

Hoje o estado da UI inclui:

- modo de setup
- apelido
- sala
- canais
- codigo da sala
- salas descobertas na LAN
- endpoint local
- URL do console auxiliar
- sessao ativa
- snapshot
- estado de audio e microfone

### `local/`

Esse pacote cuida do fluxo local:

- `LocalPreferenceStore.kt`
- `LanAddressResolver.kt`
- `NsdRoomAdvertiser.kt`
- `NsdRoomDiscovery.kt`

### `localserver/`

Esse pacote implementa o host local Android:

- `LocalHostRuntime.kt`
- `LocalHostServer.kt`
- `LocalRoomRegistry.kt`

O host local expoe:

- `GET /health`
- `POST /api/rooms`
- `POST /api/rooms/join`
- `GET /api/rooms/{roomCode}`
- `GET /api/pairing`
- `GET /console`
- `ws://.../ws`

### `audio/AudioRouteController.kt`

Controla rotas de audio da sessao:

- `Alto-falante`
- `Auricular`
- `Fone com fio`
- `Bluetooth`

## Permissoes importantes

No `AndroidManifest.xml`, alem das permissoes basicas, a V2 usa:

- `ACCESS_WIFI_STATE`
- `CHANGE_WIFI_MULTICAST_STATE`

Essas permissoes ajudam no fluxo local e na descoberta da sala.

## Defaults operacionais

- apelido padrao: `Operador Android`
- sala padrao: `Equipe LAN`
- canais padrao: `Geral, Operacao, Suporte`
- rota de audio padrao ao entrar: `Alto-falante`

## O que e oficial no Android

O caminho oficial do produto hoje e:

- criar e hospedar localmente no Android
- entrar por descoberta local no Android
- usar o navegador como console auxiliar

O modo avancado continua existindo, mas nao e mais o centro da experiencia.
