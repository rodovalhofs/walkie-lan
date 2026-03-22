# Android Host

## Papel do app Android

O app Android e o centro da experiencia principal do projeto.

Ele:

- cria sala
- entra em sala
- mantem estado local da UI
- controla o PTT quando e o host
- negocia WebRTC
- mostra presenca e eventos
- mantem um foreground service de sessao

## Arquivos principais

### `MainActivity.kt`

Ponto de entrada do app. Cria a tela Compose principal.

### `MainViewModel.kt`

E o cerebro do app. Ele:

- chama a API REST
- conecta no WebSocket
- interpreta mensagens de socket
- gerencia estado de UI
- faz a automacao de host para `talk_request` e `talk_release_request`
- decide quando habilitar ou desabilitar o microfone
- atualiza o roteamento de audio para peers elegiveis

### `UiState.kt`

Define o estado observado pela UI:

- URL do servidor
- apelido
- nome da sala
- canais
- codigo
- snapshot
- sessao ativa
- conectado
- microfone pronto
- falando ou escutando
- mensagem de aviso
- erro
- busy

### `WalkieScreen.kt`

Define a interface Compose. Tem dois estados:

- `SetupScreen`
- `ActiveRoomScreen`

No `SetupScreen`, o usuario:

- informa endereco do servidor
- informa apelido
- define nome da sala
- define canais
- cria sala
- entra por codigo

No `ActiveRoomScreen`, o usuario:

- ve codigo da sala
- ve status do host
- ve status do microfone
- troca de canal
- habilita microfone
- segura para falar
- encerra a sessao
- acompanha presenca e eventos

### `SignalingApi.kt`

Cliente REST Android.

Tem dois metodos principais:

- `createRoom`
- `joinRoom`

Usa OkHttp e `kotlinx.serialization`.

### `SignalingSocket.kt`

Cliente WebSocket Android.

Ao abrir a conexao:

- conecta em `session.wsUrl`
- envia `hello`
- decodifica mensagens do servidor em `SocketMessage`

### `ProtocolModels.kt`

Replica no Android os contratos usados no servidor e na web.

Pontos importantes:

- `explicitNulls = false` na serializacao
- `ignoreUnknownKeys = true`
- discriminador `kind` para mensagens de socket

### `WebRtcController.kt`

Camada WebRTC Android.

Responsabilidades:

- inicializar `PeerConnectionFactory`
- criar `AudioTrack`
- criar `PeerConnection` por peer remoto
- enviar `offer`
- responder `offer` com `answer`
- aplicar `answer`
- codificar e decodificar ICE candidates
- anexar ou remover audio dos peers elegiveis

### `WalkieSessionService.kt`

Foreground service da sessao.

Responsabilidades:

- criar canal de notificacao
- manter notificacao persistente
- atualizar titulo da notificacao com a sala ativa

## Permissoes Android

Declaradas no `AndroidManifest.xml`:

- `INTERNET`
- `RECORD_AUDIO`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_DATA_SYNC`
- `FOREGROUND_SERVICE_MICROPHONE`
- `POST_NOTIFICATIONS`
- `ACCESS_NETWORK_STATE`
- `WAKE_LOCK`

## Defaults importantes

- no emulador, a URL padrao e `http://10.0.2.2:8787`
- fora do emulador, o campo do servidor comeca vazio
- apelido padrao: `Operador Android`
- sala padrao: `Equipe LAN`
- canais padrao: `Geral, Operacao, Suporte`

## Comportamento de host

Quando o Android esta como host:

- recebe `talk_request`
- verifica se o canal esta livre
- envia `talk_granted` ou `talk_denied`
- quando recebe `talk_release_request`, envia `talk_released`

Esse fluxo esta concentrado em `maybeHandleHostAutomation` no `MainViewModel.kt`.
