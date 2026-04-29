package com.iris.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    private static final int CALL_PERMISSION = 1;
    private static final int MIC_PERMISSION = 2;
    private String pendingCallNumber = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);

        // Request permissions
        requestPermissions();

        // WebView settings
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");

        // JavaScript Bridge
        webView.addJavascriptInterface(new IRISBridge(), "AndroidBridge");

        // WebChromeClient — mic permission + audio
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        // WebViewClient — intercept links
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();

                // YouTube
                if (url.contains("youtube.com") || url.contains("youtu.be") || url.startsWith("vnd.youtube")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.setPackage("com.google.android.youtube");
                        startActivity(intent);
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    }
                    return true;
                }

                // WhatsApp
                if (url.contains("wa.me") || url.contains("whatsapp")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.setPackage("com.whatsapp");
                        startActivity(intent);
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    }
                    return true;
                }

                // Call
                if (url.startsWith("tel:")) {
                    String number = url.replace("tel:", "");
                    makeAutoCall(number);
                    return true;
                }

                // Maps
                if (url.contains("maps.google.com")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }

                return false;
            }
        });

        // Load IRIS
        webView.loadUrl("https://ashh2905-iris-ai.hf.space");
    }

    // JavaScript Bridge
    class IRISBridge {
        @JavascriptInterface
        public void makeCall(String number) {
            makeAutoCall(number);
        }

        @JavascriptInterface
        public void openYouTube(String query) {
            runOnUiThread(() -> {
                try {
                    // Direct YouTube search with autoplay intent
                    String ytUrl = "https://www.youtube.com/results?search_query=" + Uri.encode(query);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl));
                    intent.setPackage("com.google.android.youtube");
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))));
                }
            });
        }

        @JavascriptInterface
        public void openWhatsApp(String number, String message) {
            runOnUiThread(() -> {
                try {
                    String url = "https://wa.me/" + number + "?text=" + Uri.encode(message);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setPackage("com.whatsapp");
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "WhatsApp nahi mila!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void openMaps(String location) {
            runOnUiThread(() -> {
                String url = "https://maps.google.com?q=" + Uri.encode(location);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            });
        }

        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() ->
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show()
            );
        }
    }

    // Auto Call with 3 second countdown
    private void makeAutoCall(String number) {
        pendingCallNumber = number;
        runOnUiThread(() -> {
            Toast.makeText(this, "📞 Calling in 3 seconds...", Toast.LENGTH_LONG).show();
            // Countdown
            new Handler().postDelayed(() -> {
                Toast.makeText(this, "📞 2...", Toast.LENGTH_SHORT).show();
            }, 1000);
            new Handler().postDelayed(() -> {
                Toast.makeText(this, "📞 1...", Toast.LENGTH_SHORT).show();
            }, 2000);
            new Handler().postDelayed(() -> {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + number));
                    startActivity(callIntent);
                } else {
                    // Permission nahi hai to maango
                    pendingCallNumber = number;
                    ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION);
                }
            }, 3000);
        });
    }

    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        };
        ActivityCompat.requestPermissions(this, permissions, MIC_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PERMISSION && pendingCallNumber != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                makeAutoCall(pendingCallNumber);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
