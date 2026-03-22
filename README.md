# Walkie LAN

Walkie-talkie híbrido em rede local, com Android como host principal e entrada web por código curto.

Este projeto foi criado para cenários simples de operação em LAN/Wi-Fi, em que uma pessoa no Android cria a sala e outros participantes entram pelo navegador ou por outro Android. A proposta é entregar uma experiência de push-to-talk enxuta, aberta e fácil de testar.

## Visão geral

O fluxo atual funciona assim:

1. O Android cria uma sala.
2. O servidor de sinalização gera um código curto.
3. Outro participante entra pelo navegador usando esse código.
4. O host continua autoritativo no controle dos canais e da fala.

Hoje o projeto já é suficiente para:

- criar sala com código curto
- entrar na sala por navegador
- ver presença, canais e eventos
- usar a base de push-to-talk com um locutor por canal
- testar Android + web no mesmo ambiente local

## O que este projeto é

- um MVP funcional
- um projeto open source
- uma base para evoluir um rádio LAN moderno
- um ponto de partida para testes com Android e iPhone via navegador

## O que este projeto ainda não é

- um produto pronto para lojas
- uma solução com criptografia ponta a ponta
- uma solução pronta para uso 100% sem backend de apoio
- uma solução finalizada de produção com deploy automatizado

## Estrutura do repositório

```text
apps/
  android-host/       App Android em Kotlin + Jetpack Compose
  signaling-server/   Servidor REST + WebSocket para bootstrap e sinalização
  web-client/         Cliente web/PWA em React + Vite
packages/
  protocol/           Tipos e contratos compartilhados entre web, servidor e Android
```

## Tecnologias usadas

- Android: Kotlin, Jetpack Compose, OkHttp, WebRTC
- Web: React, Vite, simple-peer, WebRTC
- Servidor: Node.js, Express, WebSocket, Zod
- Monorepo: npm workspaces

## Requisitos

Para rodar o projeto localmente, você vai precisar de:

- Node.js 20 ou superior
- npm 10 ou superior
- JDK 17 ou superior
- Android Studio + Android SDK para gerar APK

## Como iniciar rapidamente

### 1. Instalar dependências

```bash
npm install
```

Depois do `npm install`, o pacote compartilhado `@walkie/protocol` é gerado automaticamente para que web e servidor já funcionem em um clone limpo.

### 2. Iniciar o servidor de sinalização

```bash
npm run dev:server
```

Endereço padrão:

```text
http://localhost:8787
```

Teste rápido de saúde:

```text
http://localhost:8787/health
```

### 3. Iniciar a aplicação web

Em outro terminal:

```bash
npm run dev:web
```

Endereço padrão:

```text
http://localhost:5173
```

### 4. Gerar a APK Android

```powershell
cd apps/android-host
.\gradlew.bat :app:assembleDebug
```

Saída da APK:

```text
apps/android-host/app/build/outputs/apk/debug/app-debug.apk
```

## Como testar localmente

## Cenário 1: emulador Android + servidor no PC

- abra o app Android no emulador
- use `http://10.0.2.2:8787` como endereço do servidor
- crie uma sala

## Cenário 2: Android real + navegador em outro dispositivo

1. conecte tudo na mesma rede Wi-Fi
2. descubra o IP do seu computador
3. rode o servidor e a web no seu computador
4. no Android, use `http://SEU_IP:8787`
5. no navegador do outro aparelho, abra `http://SEU_IP:5173`

Exemplo:

```text
http://192.168.1.29:8787
http://192.168.1.29:5173
```

## Como descobrir o IP do computador no Windows

```powershell
ipconfig
```

Procure por `Endereço IPv4` no adaptador da rede Wi-Fi.

## Variáveis de ambiente

O servidor aceita:

- `PORT`
- `PUBLIC_HTTP_BASE_URL`
- `PUBLIC_WS_BASE_URL`

Veja o arquivo [.env.example](.env.example).

## Comandos úteis

Build geral:

```bash
npm run build
```

Testes:

```bash
npm test
```

APK debug:

```powershell
cd apps/android-host
.\gradlew.bat :app:assembleDebug
```

Instalar APK com `adb`:

```powershell
C:\Users\yurir\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r apps\android-host\app\build\outputs\apk\debug\app-debug.apk
```

## Como funciona a arquitetura

### Android

O Android é o host principal da sala e entrega a melhor experiência operacional. Ele mantém o estado da sessão, participa do fluxo de sinalização e atua como referência de controle para o push-to-talk.

### Servidor

O servidor é pequeno e tem foco em bootstrap e sinalização:

- cria salas
- gera código curto
- recebe entrada por código
- mantém o canal WebSocket
- repassa mensagens de sinalização entre os peers

### Web

A web existe para ampliar compatibilidade, especialmente para testes e entrada por navegador em iPhone. Ela não substitui a experiência nativa Android, mas permite que mais pessoas entrem na sala sem instalar app nativo.

## Limitações atuais

- iPhone com microfone na web funciona melhor em origem segura HTTPS
- ainda não há criptografia ponta a ponta
- ainda não há deploy de produção configurado
- o fluxo PTT ainda é MVP e pode receber refinamentos

## Solução de problemas

### O Android real não conecta no servidor

Confira:

- se o servidor está rodando
- se você usou o IP do computador e não `localhost`
- se computador e telefone estão na mesma rede
- se a porta `8787` está liberada no firewall

### O navegador parece estar carregando versão antiga

Faça:

- pare `npm run dev:server` e `npm run dev:web`
- suba os dois de novo
- recarregue a página com força
- se for PWA instalada, remova e abra novamente

### O app Android fecha sozinho

Capture o log:

```powershell
C:\Users\yurir\AppData\Local\Android\Sdk\platform-tools\adb.exe logcat
```

### O emulador Android não acha o servidor

Use:

```text
http://10.0.2.2:8787
```

## Roadmap sugerido

- melhorar estabilidade do áudio/PTT
- publicar backend de sinalização
- servir web por HTTPS para testes reais em iPhone
- gerar build release assinada
- melhorar UX da tela principal

## Open source

Licença:

- [LICENSE](LICENSE)

Como contribuir:

- [CONTRIBUTING.md](CONTRIBUTING.md)

Conduta da comunidade:

- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)

CI:

- [GitHub Actions](.github/workflows/ci.yml)

## Estado atual do repositório

Feito por IA.
