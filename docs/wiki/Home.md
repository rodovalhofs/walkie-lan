# Walkie LAN Wiki

Bem-vindo a wiki do Walkie LAN.

Esta documentacao acompanha a V2 do projeto, em que o produto passou a ser `local-first`:

- o APK Android e o host principal e caminho oficial
- a rede local virou o fluxo prioritario
- o site virou vitrine publica, onboarding e console auxiliar
- o fluxo antigo com servidor manual ficou em modo avancado

## Comece por aqui

- [Visao geral](Visao-Geral.md)
- [Arquitetura](Arquitetura.md)
- [Como rodar](Como-Rodar.md)
- [Android host](Android-Host.md)
- [Frontend web](Frontend-Web.md)
- [Funcionalidades atuais do site](Funcionalidades-Atuais-do-Site.md)

## O que mudou na V2

- Android agora pode hospedar a sala localmente
- Android anuncia a sala por NSD/mDNS
- outro Android entra por descoberta local
- o host gera QR code para abrir o console no navegador
- a web publica nao fica mais centrada em "host no navegador"
- o laboratorio web continua existindo, mas fora do fluxo principal

## Papeis principais do sistema

### APK Android

- criar sala
- descobrir salas LAN
- entrar na sala
- controlar PTT
- exibir QR/local console
- trocar saida de audio

### Site / PWA

- explicar o produto
- orientar instalacao e uso
- abrir fluxo avancado de compatibilidade
- acompanhar a sala como console auxiliar

### Servidor Node

- manter compatibilidade com o modo avancado
- criar sala e sinalizar no fluxo legado
- servir laboratorio remoto/manual quando necessario

## Outras paginas importantes

- [Servidor de sinalizacao](Servidor-de-Sinalizacao.md)
- [Protocolo compartilhado](Protocolo-Compartilhado.md)
- [Fluxo de sessao e PTT](Fluxo-de-Sessao-e-PTT.md)
- [Mapa do codigo](Mapa-do-Codigo.md)
- [Troubleshooting](Troubleshooting.md)
- [Roadmap](Roadmap.md)
