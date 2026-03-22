# Fluxo de Sessao e PTT

## Criacao de sala

1. O host chama `POST /api/rooms`
2. O servidor cria `roomId`, `roomCode`, `hostPeerId` e `hostSessionToken`
3. O servidor devolve tambem o `wsUrl`
4. O host conecta no WebSocket
5. O host envia `hello`
6. O servidor autentica e envia `room_snapshot`

## Entrada em sala

1. Um cliente chama `POST /api/rooms/join`
2. O servidor confere se a sala esta aberta e se o host esta online
3. O servidor gera `peerId` e `peerToken`
4. O cliente conecta no WebSocket
5. O cliente envia `hello`
6. O servidor registra o peer, emite `peer_joined` e atualiza o snapshot

## Troca de canal

1. O cliente envia `channel_select`
2. O servidor atualiza `selectedChannelId`
3. O servidor gera evento de `channel_change`
4. Todos os clientes atualizam presenca

## Fluxo de fala

### Solicitar fala

1. O cliente envia `talk_request`
2. O servidor encaminha ao host
3. O host verifica se o canal esta livre

### Se o canal estiver livre

1. O host envia `talk_granted`
2. Todos atualizam o `activeSpeakerByChannel`
3. O locutor atual habilita sua trilha de audio
4. O locutor anexa a trilha apenas aos peers elegiveis

### Se o canal estiver ocupado

1. O host envia `talk_denied`
2. O solicitante recebe a mensagem de erro amigavel

### Soltar para falar

1. O cliente envia `talk_release_request`
2. O host responde com `talk_released`
3. Todos limpam o locutor ativo do canal
4. O locutor remove ou desabilita a trilha de audio

## WebRTC

O fluxo de sinalizacao usa:

- `offer`
- `answer`
- `ice-candidate`

Esses envelopes trafegam dentro da mensagem `signal`.

## Audio por canal

O projeto nao faz multicast real nem SFU.

Hoje ele usa:

- conexoes peer-to-peer
- roteamento seletivo de trilha
- um locutor por canal

Na pratica:

- se voce esta falando no canal A, so os peers conectados no canal A recebem sua trilha

## Encerramento da sala

Se o host desconecta:

- o servidor envia `room_closed`
- a sala e removida da memoria
- os outros peers perdem a sessao
