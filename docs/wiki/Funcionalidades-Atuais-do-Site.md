# Funcionalidades Atuais do Site

Esta pagina lista o que o site tem hoje, no estado atual do codigo da V2.

## Estados principais da interface

O site tem hoje dois estados de alto nivel:

- tela publica inicial
- tela de sala

## Tela publica inicial

Ela tem:

- faixa `Walkie LAN open source`
- titulo `Android protagonista. Navegador como apoio leve.`
- texto explicando o reposicionamento do produto
- botao para abrir/fechar `laboratorio avancado`
- link para a wiki do projeto
- tres cards de explicacao:
  - criar no Android
  - entrar na rede local
  - usar web como console
- card `Fluxo recomendado`
- card `Perfil local`
- card `Console auxiliar`

## Dados editaveis na tela publica

- apelido do operador

## Laboratorio avancado

Quando aberto, o site mostra:

- bloco `Conexao manual`
- campo `URL do servidor`
- bloco `Host web experimental`
- campo `Nome da sala`
- campo `Canais`
- preview dos canais
- botao `Host web experimental`
- bloco `Entrar por codigo`
- campo `Codigo da sala`
- botao `Entrar como console`
- botao `Entrar como web experimental`
- banner de erro, quando necessario

## Tela da sala

A tela da sala mostra:

- etiqueta de contexto:
  - `Console auxiliar`
  - `Web experimental`
  - ou `Sala ativa`
- nome da sala
- codigo da sala
- modo de transporte
- estado de conexao
- status do host
- endpoint local quando presente
- status badges de conexao, microfone e transmissao

## Area de controle da sala

A area principal da sala tem:

- lista de canais
- indicador de locutor por canal ou `Livre`
- painel informando o canal atual
- se a sessao for `console_only`:
  - aviso de console auxiliar
- se a sessao for `experimental_web_voice`:
  - botao `Habilitar microfone`
  - seletor de saida de audio quando suportado
  - mensagem de saida de audio
  - botao PTT grande

## Area de participantes

Ela mostra:

- apelido
- canal atual
- badge com papel do participante
- estado de conexao

## Area de eventos

Ela mostra:

- lista dos eventos mais recentes
- resumo do evento
- horario formatado em `pt-BR`

## Comportamentos atuais

- cria sala no laboratorio avancado
- entra por codigo no laboratorio avancado
- entra como `console_only`
- entra como `experimental_web_voice`
- abre WebSocket
- envia `hello`
- recebe snapshot
- atualiza presenca
- troca canal
- solicita e libera fala no modo experimental
- cria e destroi peers com `simple-peer`
- toca audio remoto em elementos `<audio>`
- tenta selecionar saida de audio quando suportado
- salva `deviceId` no `localStorage`
- registra service worker
- funciona como PWA

## O que o site nao faz como fluxo principal

- nao e o host oficial do produto
- nao e a experiencia principal de voz
- nao faz descoberta LAN como cliente oficial
- nao substitui o APK Android em PTT operacional
