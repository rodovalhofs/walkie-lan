# Funcionalidades Atuais do Site

Esta pagina lista o que a web app tem hoje, literalmente no estado atual do codigo.

## Estados de interface

A web tem hoje dois estados principais:

- tela inicial
- tela de sala

## Tela inicial

A tela inicial tem:

- faixa `Walkie-Talkie LAN Hibrido`
- titulo `Android hospeda. iPhone entra pelo navegador.`
- texto de apoio sobre Wi-Fi, codigo curto, canais fixos, PTT e WebRTC
- lista de destaques
- campo `URL do servidor`
- campo `Seu apelido`
- area `Criar sala`
- campo `Nome da sala`
- campo `Canais`
- preview `Canais previstos`
- botao `Host no navegador`
- texto explicando que esse host web e um modo de debug
- area `Entrar por codigo`
- campo `Codigo da sala`
- botao `Entrar da web app`
- texto explicando foco em Safari/iPhone com HTTPS
- banner de erro quando algo falha

## Valores padrao

- URL do servidor: `http://<host-atual>:8787`
- apelido: `Operador`
- nome da sala: `Equipe LAN`
- canais: `Geral, Operacao, Suporte`
- codigo da sala: vazio

## Tela da sala

A tela da sala tem:

- etiqueta `Sala ativa`
- nome da sala
- codigo da sala
- status do host
- status de conexao
- indicador `Microfone pronto` ou `Microfone pendente`
- indicador `Transmitindo` ou `Escuta`
- lista de botoes de canal
- nome de quem esta falando em cada canal, ou `Livre`
- botao `Habilitar microfone` ou `Revalidar microfone`
- botao grande `Aperte para falar`
- aviso textual dinamico dentro da sala
- area `Presenca`
- lista de participantes
- canal atual de cada participante
- badge do participante, como `Host`, `ios_web` ou `android_web_debug`
- area `Eventos recentes`
- lista dos 8 eventos mais recentes
- `AudioDock` escondido para os audios remotos

## Comportamentos atuais

- cria sala pela API
- entra por codigo pela API
- abre WebSocket
- envia `hello`
- recebe snapshot
- atualiza presenca
- troca de canal
- solicita fala
- libera fala
- recebe `offer`
- recebe `answer`
- recebe `ice-candidate`
- cria e destroi peers com `simple-peer`
- reproduz audio remoto em elementos `<audio>`
- salva `deviceId` no `localStorage`
- registra service worker
- funciona como PWA

## Modo host web

O site ainda pode ser host em modo de debug.

Nesse modo:

- cria a sala
- age como host autoritativo para PTT
- responde `talk_granted`
- responde `talk_denied`
- responde `talk_released`

## O que o site nao tem hoje

- login
- senha
- cadastro
- lista publica de salas
- chat de texto
- anexos
- tela de configuracoes
- criptografia ponta a ponta exposta na UI
- descoberta automatica LAN na web
- gravacao de audio
- controle de volume por participante
