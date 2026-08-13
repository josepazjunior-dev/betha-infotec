# Meu Suporte Cliente Android

Aplicativo Android do cliente do Meu Suporte / Junior IPTV.

- Pacote: `online.meusuporte.junioriptv`
- Abre o suporte em modo Android.
- Reaproveita login por ID e estado salvo pelo chat web.
- Registra o token FCM no servidor quando identifica a conversa autenticada.
- Recebe notificações nativas com o app aberto, em segundo plano ou fechado (exceto force-stop do Android).

Para habilitar FCM, registre este pacote como um segundo app Android no mesmo projeto Firebase e adicione `app/google-services.json`.
