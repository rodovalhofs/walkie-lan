# Walkie LAN Wiki

Bem-vindo a wiki do Walkie LAN.

Este projeto e um walkie-talkie hibrido para rede local. O Android e o host principal da sala. Outros participantes podem entrar por outro Android ou pela web app, inclusive no iPhone pelo navegador.

## O que voce encontra aqui

- [Visao geral](Visao-Geral.md)
- [Arquitetura](Arquitetura.md)
- [Estrutura do repositorio](Estrutura-do-Repositorio.md)
- [Como rodar](Como-Rodar.md)
- [Android host](Android-Host.md)
- [Frontend web](Frontend-Web.md)
- [Servidor de sinalizacao](Servidor-de-Sinalizacao.md)
- [Protocolo compartilhado](Protocolo-Compartilhado.md)
- [Fluxo de sessao e PTT](Fluxo-de-Sessao-e-PTT.md)
- [Funcionalidades atuais do site](Funcionalidades-Atuais-do-Site.md)
- [Troubleshooting](Troubleshooting.md)
- [Mapa do codigo](Mapa-do-Codigo.md)
- [Roadmap](Roadmap.md)

## Resumo rapido

- O repositorio e um monorepo com Android, web, servidor e pacote de protocolo.
- O servidor cria salas, gera codigo curto e faz a sinalizacao via WebSocket.
- O Android e o host autoritativo do PTT na experiencia principal.
- A web permite entrada por codigo curto e audio via WebRTC.
- O audio e distribuido de forma seletiva por canal, com um locutor por canal de cada vez.

## Fluxo atual em uma frase

Um Android cria a sala, recebe um codigo curto, os outros participantes entram pelo navegador ou por outro app, e o host controla quem pode falar em cada canal.

## Estado atual do projeto

Hoje o projeto ja consegue:

- criar sala
- entrar por codigo curto
- manter presenca
- trocar de canal
- fazer PTT
- enviar sinalizacao WebRTC entre Android e web
- tocar audio remoto no navegador
- manter uma notificacao de sessao no Android

## Limites atuais

- ainda depende de um servidor de apoio para bootstrap e sinalizacao
- a experiencia web no iPhone e pensada para primeiro plano
- ainda nao existe criptografia ponta a ponta
- ainda nao existe deploy de producao pronto no repositorio

## Pagina recomendada para comecar

Se voce quer entender o todo primeiro, comece em [Arquitetura](Arquitetura.md).

Se voce quer rodar logo, comece em [Como rodar](Como-Rodar.md).
