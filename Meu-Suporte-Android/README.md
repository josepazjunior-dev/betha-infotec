# Meu Suporte Android

Aplicativo Android do Painel Meu Suporte (`https://meusuporte.online/painel/`).

## Objetivo
- Abrir o painel em WebView preservando a interface web existente.
- Suportar câmera, microfone e seleção de arquivos.
- Receber notificações nativas via Firebase Cloud Messaging (FCM), inclusive com o app fechado.
- Abrir a conversa indicada pelo campo `url` da mensagem FCM.

## Pacote Android
`online.meusuporte.painel`

## Firebase
Para ativar FCM:
1. Criar/abrir um projeto no Firebase.
2. Registrar um app Android com pacote `online.meusuporte.painel`.
3. Baixar `google-services.json`.
4. No GitHub, salvar o conteúdo desse arquivo no Secret `GOOGLE_SERVICES_JSON`.
5. O servidor Meu Suporte precisa implementar `/api/android_fcm_register.php` para salvar o token por atendente/dispositivo e enviar FCM quando uma nova mensagem do cliente for gravada.

Sem `google-services.json`, o projeto continua funcionando como WebView, mas o FCM não fica ativo.

## Build
O workflow `.github/workflows/build-meu-suporte-apk.yml` compila o APK debug e publica o APK como Artifact do GitHub Actions.

Build de validação configurado na branch `meu-suporte-android`.
