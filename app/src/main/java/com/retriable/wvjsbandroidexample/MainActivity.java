package com.retriable.wvjsbandroidexample;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.retriable.wvjsb.Server;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView webView = findViewById(R.id.web_view);

        Server server = Server.getInstance(webView,null);
        server.on("immediate");
        webView.setWebChromeClient(new WebChromeClient(){

        });
        webView.setWebViewClient(new WebViewClient(){

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                if(Build.VERSION.SDK_INT > 21){
                    return !Server.canHandle(view,request.getUrl().toString());
                }
                return false;

            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !Server.canHandle(view,url);
            }
        });
    }
}
