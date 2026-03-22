# Como contribuir

Obrigado por querer melhorar o Walkie LAN.

Este projeto ainda está em fase inicial, então contribuições pequenas e objetivas ajudam bastante.

## Antes de começar

1. faça um fork do repositório
2. crie uma branch para sua alteração
3. mantenha o escopo focado
4. descreva claramente o que foi alterado

## Preparando o ambiente

1. instale Node.js e npm
2. rode `npm install`
3. inicie o servidor com `npm run dev:server`
4. inicie a web com `npm run dev:web`
5. para Android, abra `apps/android-host` no Android Studio

## Boas práticas para pull requests

- prefira PRs pequenas
- explique o problema e a solução
- diga como você testou
- atualize a documentação se o comportamento mudou

## Checklist antes de abrir PR

- `npm run build`
- `npm test`
- se mexeu no Android, `.\gradlew.bat :app:assembleDebug`
- verifique se não deixou arquivos de build ou dados locais no commit

## Ao reportar bugs

Inclua, se possível:

- dispositivo e sistema operacional
- navegador, se for problema web
- passos para reproduzir
- mensagens de erro
- prints ou logs

## Tipos de contribuição bem-vindos

- correção de bugs
- melhoria de UX
- melhoria de documentação
- testes
- deploy e automação
- melhorias de áudio/PTT
