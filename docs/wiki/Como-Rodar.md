# Como Rodar

## Requisitos

- Node.js 20 ou superior
- npm 10 ou superior
- JDK 17 ou superior
- Android Studio com Android SDK

## Instalar dependencias

Na raiz do projeto:

```bash
npm install
```

O `postinstall` ja compila o pacote compartilhado `@walkie/protocol`.

## Fluxo principal: local-first

Este e o fluxo recomendado na V2.

### 1. Gerar o APK

```powershell
cd apps/android-host
.\gradlew.bat :app:assembleDebug
```

Saida:

```text
apps/android-host/app/build/outputs/apk/debug/app-debug.apk
```

### 2. Instalar no Android

Via `adb`:

```powershell
C:\Users\yurir\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r apps\android-host\app\build\outputs\apk\debug\app-debug.apk
```

### 3. Criar sala no APK

No app:

- abra `Modo simples`
- ajuste apelido, nome da sala e canais se quiser
- toque em `Criar sala`

### 4. Entrar pelo segundo Android

No outro APK:

- abra o app na mesma rede Wi-Fi
- toque em atualizar se necessario
- escolha a sala descoberta na LAN

### 5. Abrir o console no navegador

No host Android:

- use o QR code exibido
- ou abra a URL local do console mostrada na tela

Esse console funciona bem para:

- iPhone no Safari
- desktop na mesma rede
- acompanhamento da sala, participantes e eventos

## Fluxo avancado / legado

Se voce quiser testar compatibilidade com servidor manual:

### Subir o servidor

```bash
npm run dev:server
```

Endereco padrao:

```text
http://localhost:8787
```

### Subir a web publica/laboratorio

```bash
npm run dev:web
```

Endereco padrao:

```text
http://localhost:5173
```

### Usar no APK

- abra `Modo avancado`
- informe a URL manual do servidor
- crie ou entre por codigo

## Build e testes

Build geral:

```bash
npm run build
```

Testes:

```bash
npm test
```

## Observacoes importantes

- `localhost` no telefone significa o proprio telefone
- a web nao e mais o fluxo principal de voz
- o servidor Node nao e mais obrigatorio para o caminho principal
- o iPhone entra melhor como console auxiliar no fluxo local
