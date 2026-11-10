package ru.bramida.crm;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;

/**
 * Thin native shell around the CRM web app: full-screen WebView, files (contracts) saved to
 * Downloads or sent through the system share sheet, back button closes open dialogs first.
 */
public class MainActivity extends Activity {

    static final String START_URL = "https://milianforz-prog.github.io/bramida-crm/";
    static final String APP_HOST = "milianforz-prog.github.io";

    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);
        s.setUserAgentString(s.getUserAgentString() + " ProkatCRM-Android/1");

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme();
                if (("https".equals(scheme) || "http".equals(scheme)) && APP_HOST.equals(u.getHost())) return false;
                openExternal(u);
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) view.loadDataWithBaseURL(START_URL, OFFLINE_HTML, "text/html", "utf-8", null);
            }
        });
        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, length) -> {
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) openExternal(Uri.parse(url));
        });

        if (state != null) web.restoreState(state);
        else web.loadUrl(START_URL);
    }

    private void openExternal(Uri u) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.__androidBack && window.__androidBack()) ? 'yes' : 'no'", result -> {
            if (result != null && result.contains("yes")) return;
            if (web.canGoBack()) web.goBack();
            else finish();
        });
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }

    /** Called from the web app (window.AndroidBridge). Runs on a background thread. */
    class Bridge {
        @JavascriptInterface
        public String saveFile(String name, String mime, String base64) {
            try {
                writeToDownloads(name, mime, base64);
                return "ok";
            } catch (Exception e) {
                return String.valueOf(e.getMessage());
            }
        }

        @JavascriptInterface
        public String shareFile(String name, String mime, String base64) {
            try {
                Uri uri = writeToDownloads(name, mime, base64);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType(mime);
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent chooser = Intent.createChooser(send, name);
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                runOnUiThread(() -> startActivity(chooser));
                return "ok";
            } catch (Exception e) {
                return String.valueOf(e.getMessage());
            }
        }
    }

    private Uri writeToDownloads(String name, String mime, String base64) throws Exception {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, name);
        values.put(MediaStore.Downloads.MIME_TYPE, mime);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new Exception("нет доступа к папке «Загрузки»");
        try (OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) throw new Exception("не удалось записать файл");
            out.write(bytes);
        }
        values.clear();
        values.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
        return uri;
    }

    static final String OFFLINE_HTML = "<!doctype html><html lang='ru'><head><meta charset='utf-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;"
            + "background:#14171C;color:#EDF0F4;font:16px system-ui,sans-serif;text-align:center;padding:24px;box-sizing:border-box}"
            + "b{display:block;font-size:20px;margin-bottom:8px}p{color:#8F97A5;margin:0 0 20px}"
            + "a{display:inline-block;background:#FFC21A;color:#1D1600;text-decoration:none;font-weight:600;padding:12px 22px;border-radius:12px}</style></head>"
            + "<body><div><b>Нет связи с интернетом</b><p>Проверьте подключение и попробуйте ещё раз.</p>"
            + "<a href='" + START_URL + "'>Повторить</a></div></body></html>";
}
