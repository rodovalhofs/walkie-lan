# Arquitetura

## Visao de alto nivel

```text
Android Host <----HTTP----> Servidor de sinalizacao <----HTTP----> Web / Android cliente
     |                          |
     |                          +---- WebSocket para eventos e sinalizacao
     |
     +------------------------- WebRTC audio ------------------------------+
```

## Componentes

### Android host

Responsavel por:

- criar sala
- entrar em sala
- manter estado local da sessao
- agir como host autoritativo do PTT
- iniciar conexoes WebRTC com outros peers
- ativar notificacao de foreground service

### Servidor de sinalizacao

Responsavel por:

- validar payloads com Zod
- criar e localizar salas
- gerar codigo curto
- autenticar peers por token
- enviar snapshots e eventos
- encaminhar envelopes de sinalizacao

### Web app

Responsavel por:

- criar sala em modo host de debug
- entrar em sala por codigo
- habilitar microfone no navegador
- manter conexao WebSocket
- negociar WebRTC com `simple-peer`
- tocar os streams remotos

### Protocolo compartilhado

Responsavel por:

- definir contratos de request e response
- definir formato das mensagens de socket
- centralizar defaults e limites
- reduzir incompatibilidades entre web e servidor

## Modelo de sessao

Uma sala contem:

- `roomId`
- `roomCode`
- `roomName`
- canais
- membros
- `activeSpeakerByChannel`
- status do host
- log de eventos
- limite de capacidade

Cada membro contem:

- `peerId`
- apelido
- tipo do cliente
- `deviceId`
- canal selecionado
- indicador de host
- indicador de conexao
- datas de entrada e ultimo contato

## Modelo de audio

O projeto usa WebRTC audio-only.

O audio nao e distribuido para todos o tempo inteiro. Em vez disso:

- cada peer mantem conexoes com os outros peers conectados
- o locutor atual so anexa sua trilha de audio aos peers elegiveis
- peers elegiveis sao os participantes conectados no mesmo canal

Isso reduz envio desnecessario e combina com o modelo PTT.

## Modelo de PTT

O PTT funciona assim:

1. Um cliente envia `talk_request`
2. O host decide
3. O host responde com `talk_granted` ou `talk_denied`
4. Enquanto houver permissao, o locutor transmite
5. Ao soltar o botao, o cliente envia `talk_release_request`
6. O host responde com `talk_released`

## Regra importante de encerramento

Se o host sai da sala:

- o servidor marca a sala como fechada
- envia `room_closed`
- remove a sala da memoria

Hoje nao existe migracao automatica de host.

## Compatibilidade Android + web

Ao longo da implementacao, o projeto passou a aceitar formatos diferentes de ICE candidate e campos opcionais nulos. Isso foi importante para estabilizar a interoperacao entre:

- Android WebRTC nativo
- `simple-peer` no navegador
- validacao Zod no servidor
