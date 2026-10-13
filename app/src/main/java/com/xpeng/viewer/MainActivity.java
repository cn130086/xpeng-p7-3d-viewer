package com.xpeng.viewer;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class MainActivity extends Activity {
    private WebView webView;
    private HttpServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/")) path = "/www/index.html";
                else if (!path.startsWith("/www/")) path = "/www" + path;
                String assetPath = path.substring(1);
                String mime = "text/html";
                if (assetPath.endsWith(".js")) mime = "application/javascript";
                else if (assetPath.endsWith(".glb")) mime = "model/gltf-binary";
                else if (assetPath.endsWith(".css")) mime = "text/css";
                try {
                    AssetManager am = getAssets();
                    InputStream is = am.open(assetPath);
                    byte[] buf = new byte[8192];
                    exchange.getResponseHeaders().set("Content-Type", mime);
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    long len = am.openFd(assetPath).getLength();
                    exchange.sendResponseHeaders(200, len);
                    OutputStream os = exchange.getResponseBody();
                    int n;
                    while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
                    os.close();
                    is.close();
                } catch (IOException e) {
                    String notFound = "404";
                    exchange.sendResponseHeaders(404, notFound.length());
                    exchange.getResponseBody().write(notFound.getBytes());
                    exchange.getResponseBody().close();
                }
            });
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        webView = new WebView(this);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        setContentView(webView);
        webView.loadUrl("http://127.0.0.1:8080/www/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (server != null) server.stop(0);
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
