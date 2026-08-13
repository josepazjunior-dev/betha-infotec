package online.meusuporte.junioriptv;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://meusuporte.online/suporte.php?empresa=betha-iptv&app=android";
    private static final int REQ_PERMISSIONS = 2001;
    private static final int REQ_FILE = 2002;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private volatile String fcmToken = "";
    private volatile String customerToken = "";
    private volatile long conversationId = 0L;
    private volatile String lastRegistrationKey = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);
        configureWebView();
        requestNativePermissions();
        refreshFcmToken();
        openIntent(getIntent());
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " MeuSuporteClienteAndroid/2.0.0");
        webView.addJavascriptInterface(new ClientBridge(), "MeuSuporteAndroid");
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("meusuporte.online".equalsIgnoreCase(u.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, u));
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectCustomerStateBridge();
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
            @Override public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try { startActivityForResult(params.createIntent(), REQ_FILE); }
                catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "Não foi possível abrir os arquivos.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    private void injectCustomerStateBridge() {
        String js = "(function(){if(window.__msAndroidBridge)return;window.__msAndroidBridge=1;" +
                "function sync(){try{var s=localStorage.getItem('meusuporte_chat_last_betha-iptv');" +
                "if(s&&window.MeuSuporteAndroid)window.MeuSuporteAndroid.customerState(s);}catch(e){}}" +
                "sync();setInterval(sync,2000);})();";
        webView.evaluateJavascript(js, null);
    }

    public final class ClientBridge {
        @JavascriptInterface public void customerState(String raw) {
            try {
                JSONObject j = new JSONObject(raw);
                String t = j.optString("token", "").trim();
                long c = j.optLong("conversation_id", 0L);
                if (!t.isEmpty() && c > 0) {
                    customerToken = t;
                    conversationId = c;
                    registerDeviceIfReady();
                }
            } catch (Exception ignored) { }
        }
    }

    private void refreshFcmToken() {
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) return;
                fcmToken = task.getResult();
                registerDeviceIfReady();
            });
        } catch (IllegalStateException ignored) { }
    }

    private void registerDeviceIfReady() {
        final String f = fcmToken;
        final String t = customerToken;
        final long c = conversationId;
        if (f == null || f.isEmpty() || t == null || t.isEmpty() || c <= 0) return;
        String k = c + ":" + t + ":" + f;
        if (k.equals(lastRegistrationKey)) return;
        lastRegistrationKey = k;
        new Thread(() -> {
            try {
                URL u = new URL("https://meusuporte.online/api/client_android_fcm_register.php");
                HttpURLConnection conn = (HttpURLConnection)u.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                conn.setRequestProperty("User-Agent", "MeuSuporteClienteAndroid/2.0.0");
                String body = "conversation_id=" + c +
                        "&customer_token=" + URLEncoder.encode(t, StandardCharsets.UTF_8.name()) +
                        "&fcm_token=" + URLEncoder.encode(f, StandardCharsets.UTF_8.name()) +
                        "&package=" + URLEncoder.encode("online.meusuporte.junioriptv", StandardCharsets.UTF_8.name());
                try (OutputStream out = conn.getOutputStream()) { out.write(body.getBytes(StandardCharsets.UTF_8)); }
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) lastRegistrationKey = "";
                conn.disconnect();
            } catch (Exception e) { lastRegistrationKey = ""; }
        }).start();
    }

    private void requestNativePermissions() {
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQ_PERMISSIONS);
        else if (Build.VERSION.SDK_INT >= 23) requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQ_PERMISSIONS);
    }

    private void openIntent(Intent intent) {
        String url = intent != null ? intent.getStringExtra("url") : null;
        Uri data = intent != null ? intent.getData() : null;
        if (url == null && data != null && "meusuporte.online".equalsIgnoreCase(data.getHost())) url = data.toString();
        webView.loadUrl(url != null && url.startsWith("https://meusuporte.online/") ? url : HOME_URL);
    }

    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); openIntent(intent); }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }
    @Override public void onBackPressed() { if (webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
