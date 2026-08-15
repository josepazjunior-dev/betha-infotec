package online.meusuporte.painel;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.firebase.messaging.FirebaseMessaging;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String PANEL_URL = "https://meusuporte.online/painel/";
    private static final int REQ_PERMISSIONS = 1001;
    private static final int REQ_FILE = 1002;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView = findViewById(R.id.webView);
        configureWebView();
        requestNativePermissions();
        registerFcmToken();
        openIntent(getIntent());
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setUserAgentString(s.getUserAgentString() + " MeuSuporteAndroid/1.0.3");

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION)) {
            WebSettingsCompat.setWebAuthenticationSupport(
                    s,
                    WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP
            );
        }

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("meusuporte.online".equalsIgnoreCase(u.getHost())) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, u));
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                CookieManager.getInstance().flush();
                if (url != null && url.startsWith("https://meusuporte.online/painel/")) {
                    registerFcmToken();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent i = params.createIntent();
                try {
                    startActivityForResult(i, REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "Não foi possível abrir os arquivos.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    private void openIntent(Intent intent) {
        String url = intent != null ? intent.getStringExtra("url") : null;
        Uri data = intent != null ? intent.getData() : null;
        if (url == null && data != null && "meusuporte.online".equalsIgnoreCase(data.getHost())) url = data.toString();
        webView.loadUrl(url != null && url.startsWith("https://meusuporte.online/") ? url : PANEL_URL);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openIntent(intent);
    }

    private void requestNativePermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQ_PERMISSIONS);
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQ_PERMISSIONS);
        }
    }

    private void registerFcmToken() {
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful() || task.getResult() == null) return;
                sendToken(task.getResult());
            });
        } catch (IllegalStateException ignored) {
        }
    }

    static void sendToken(String token) {
        new Thread(() -> {
            try {
                URL u = new URL("https://meusuporte.online/api/android_fcm_register.php");
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                c.setRequestProperty("User-Agent", "MeuSuporteAndroid/1.0.3");
                String cookies = CookieManager.getInstance().getCookie(PANEL_URL);
                if (cookies != null && !cookies.trim().isEmpty()) c.setRequestProperty("Cookie", cookies);
                String body = "token=" + URLEncoder.encode(token, StandardCharsets.UTF_8.name()) +
                        "&device=android&package=" + URLEncoder.encode("online.meusuporte.painel", StandardCharsets.UTF_8.name());
                try (OutputStream out = c.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
                c.getResponseCode();
                c.disconnect();
            } catch (Exception ignored) { }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }
}
