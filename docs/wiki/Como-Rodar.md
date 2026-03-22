# Como Rodar

## Requisitos

- Node.js 20 ou superior
- npm 10 ou superior
- JDK 17 ou superior
- Android Studio com Android SDK, para gerar APK

## Instalar dependencias

Na raiz do projeto:

```bash
npm install
```

O `postinstall` ja compila o pacote compartilhado `@walkie/protocol`.

## Subir o servidor

```bash
npm run dev:server
```

Endereco padrao:

```text
http://localhost:8787
```

Healthcheck:

```text
http://localhost:8787/health
```

## Subir a web app

Em outro terminal:

```bash
npm run dev:web
```

Endereco padrao:

```text
http://localhost:5173
```

## Gerar o APK Android

```powershell
cd apps/android-host
.\gradlew.bat :app:assembleDebug
```

Saida:

```text
apps/android-host/app/build/outputs/apk/debug/app-debug.apk
```

## Testar no emulador Android

No campo do servidor do app Android, use:

```text
http://10.0.2.2:8787
```

Esse alias funciona so no emulador Android.

## Testar no telefone real

1. Conecte computador e telefone na mesma rede Wi-Fi
2. Descubra o IP do computador
3. Rode servidor e web no computador
4. No telefone Android, use `http://SEU_IP:8787`
5. No navegador de outro aparelho, use `http://SEU_IP:5173`

Exemplo:

```text
http://192.168.1.29:8787
http://192.168.1.29:5173
```

## Descobrir o IP no Windows

```powershell
ipconfig
```

Procure a linha `Endereco IPv4`.

## Build geral e testes

Build:

```bash
npm run build
```

Testes:

```bash
npm test
```

## Instalar a APK com adb

```powershell
C:\Users\yurir\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r apps\android-host\app\build\outputs\apk\debug\app-debug.apk
```

## Observacoes importantes

- `localhost` no telefone aponta para o proprio telefone, nao para o computador
- o iPhone funciona melhor com origem HTTPS para uso real do microfone em navegador
- a web atual e suficiente para testes locais e para Safari em primeiro plano
