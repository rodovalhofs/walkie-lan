# Walkie LAN

Walkie-talkie open source para rede local, com Android como host oficial da sala e web como camada publica e console auxiliar.

## O que o projeto e hoje

O Walkie LAN entrou na fase V2 com foco `local-first`.

O caminho principal agora e este:

1. Um Android cria a sala.
2. O proprio Android sobe um endpoint local na LAN.
3. A sala e anunciada por descoberta local no Android.
4. Outro Android entra por descoberta LAN.
5. iPhone ou desktop abrem o console auxiliar por QR code ou link local.

O fluxo antigo com URL manual e servidor externo continua existindo, mas ficou isolado em `Modo avancado` para compatibilidade e debug.

## Componentes do monorepo

```text
apps/
  android-host/       APK Android em Kotlin + Jetpack Compose
  signaling-server/   Servidor Node para compatibilidade e modo avancado
  web-client/         Site publico + console/laboratorio web em React + Vite
packages/
  protocol/           Contratos compartilhados entre Android, web e servidor
docs/
  wiki/               Fonte versionada da wiki do GitHub
```

## O que ja esta implementado

- host local no Android
- descoberta LAN no Android por NSD/mDNS
- entrada por descoberta de sala no APK
- QR code para abrir console auxiliar no navegador
- home nova no APK com `Modo simples` e `Modo avancado`
- tela operacional de PTT no APK
- seletor de saida de audio no Android
- landing publica nova no site
- laboratorio web separado do fluxo principal
- papeis de cliente no protocolo:
  - `full_voice`
  - `console_only`
  - `experimental_web_voice`
- modo de transporte no protocolo:
  - `local_lan`
  - `remote_signaling`

## O que e o caminho principal

### APK Android

O APK e o protagonista do produto:

- cria sala
- anuncia a sala na LAN
- entra em salas encontradas na rede
- mostra codigo e QR
- faz PTT
- controla host e sessao
- permite trocar saida de audio

### Site / PWA

O site foi reposicionado:

- landing publica do projeto
- explicacao de uso
- entrada em laboratorio avancado
- console auxiliar de sala
- participacao web experimental, fora do fluxo principal

## Requisitos

- Node.js 20 ou superior
- npm 10 ou superior
- JDK 17 ou superior
- Android Studio + Android SDK para gerar APK

## Como rodar

### Fluxo principal local-first

1. Gere e instale o APK Android:

```powershell
cd apps/android-host
.\gradlew.bat :app:assembleDebug
```

APK gerada:

```text
apps/android-host/app/build/outputs/apk/debug/app-debug.apk
```

2. Abra o APK no telefone Android.
3. Use `Criar sala` no `Modo simples`.
4. Mostre o QR ou o codigo da sala.
5. Em outro Android, entre pela lista de salas LAN.
6. Em iPhone ou desktop, abra o console local pelo QR ou URL local mostrada no app.

### Fluxo avancado / compatibilidade

Se voce quiser manter o fluxo antigo com servidor manual:

```bash
npm install
npm run dev:server
npm run dev:web
```

Servidor:

```text
http://localhost:8787
```

Web:

```text
http://localhost:5173
```

No APK, abra `Modo avancado` e informe a URL do servidor manual.

## Testes e validacao

Build geral:

```bash
npm run build
```

Testes:

```bash
npm test
```

Build do APK:

```powershell
cd apps/android-host
.\gradlew.bat :app:assembleDebug
```

## Instalar APK com adb

```powershell
C:\Users\yurir\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r apps\android-host\app\build\outputs\apk\debug\app-debug.apk
```

## Como a arquitetura ficou na V2

### Fluxo simples

- Android hospeda localmente
- Android anuncia a sala na LAN
- Android cliente entra por descoberta local
- navegador entra como console auxiliar

### Fluxo avancado

- servidor Node cria a sala
- web ou Android entram por URL manual
- fluxo legado continua disponivel para debug e compatibilidade

## Documentacao

- Wiki do GitHub: `https://github.com/rodovalhofs/walkie-lan/wiki`
- Fonte da wiki no repo: [docs/wiki](docs/wiki)

Paginas recomendadas:

- [Visao geral](docs/wiki/Visao-Geral.md)
- [Arquitetura](docs/wiki/Arquitetura.md)
- [Android host](docs/wiki/Android-Host.md)
- [Frontend web](docs/wiki/Frontend-Web.md)
- [Como rodar](docs/wiki/Como-Rodar.md)

## Open source

- Licenca: [LICENSE](LICENSE)
- Contribuicao: [CONTRIBUTING.md](CONTRIBUTING.md)
- Conduta: [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)
- CI: [.github/workflows/ci.yml](.github/workflows/ci.yml)

## Estado atual do repositorio

Projeto open source em evolucao continua, com desenvolvimento apoiado por IA.
