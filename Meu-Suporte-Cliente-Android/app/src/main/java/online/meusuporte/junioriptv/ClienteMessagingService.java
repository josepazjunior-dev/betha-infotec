package online.meusuporte.junioriptv;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ClienteMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "meu_suporte_cliente_mensagens";

    @Override public void onMessageReceived(RemoteMessage message) {
        String title = value(message, "title", "Suporte Junior IPTV");
        String body = value(message, "body", "Você recebeu uma nova mensagem do suporte.");
        String url = value(message, "url", "https://meusuporte.online/suporte.php?empresa=betha-iptv&app=android");
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Mensagens do suporte", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            channel.setDescription("Avisos de novas mensagens do Suporte Junior IPTV");
            nm.createNotificationChannel(channel);
        }
        Intent open = new Intent(this, MainActivity.class);
        open.putExtra("url", url);
        open.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_meusuporte)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(android.app.Notification.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
        nm.notify((int)(System.currentTimeMillis() & 0x0FFFFFFF), b.build());
    }

    private String value(RemoteMessage m, String key, String fallback) {
        String v = m.getData().get(key);
        if (v != null && !v.trim().isEmpty()) return v;
        if (m.getNotification() != null) {
            if ("title".equals(key) && m.getNotification().getTitle() != null) return m.getNotification().getTitle();
            if ("body".equals(key) && m.getNotification().getBody() != null) return m.getNotification().getBody();
        }
        return fallback;
    }
}
