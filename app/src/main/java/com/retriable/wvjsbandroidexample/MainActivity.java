package com.retriable.wvjsbandroidexample;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.retriable.wvjsb.Connection;
import com.retriable.wvjsb.Server;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;

public class MainActivity extends AppCompatActivity {

    private static  final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView webView = findViewById(R.id.web_view);

        Server server = Server.getInstance(webView,null);

        server.on("immediate").onEvent(new Function3<Connection, Object, Function0<? extends Function2<Object,? super Throwable, Unit>>, Object>() {
            @Override
            public Object invoke(Connection connection, Object o, Function0<? extends Function2<Object, ? super Throwable, Unit>> function0) {

                function0.invoke();
                return null;
            }
        });

        server.on("delayed").onEvent(new Function3<Connection, Object, Function0<? extends Function2<Object,? super Throwable, Unit>>, Object>() {
            @Override
            public Object invoke(Connection connection, Object o, final Function0<? extends Function2<Object, ? super Throwable, Unit>> function0) {
                Runnable runnable=new Runnable() {
                    @Override
                    public void run() {
                        function0.invoke().invoke("delayed ack",null);
                    }
                };
                handler.postDelayed(runnable,2000);
                return runnable;
            }
        }).onCancel(new Function1<Object, Unit>() {
            @Override
            public Unit invoke(Object o) {
                Runnable runnable=(Runnable)o;
                handler.removeCallbacks(runnable);
                return null;
            }
        });

        webView.setWebChromeClient(new WebChromeClient(){

        });

        webView.setWebViewClient(new WebViewClient(){

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                return !Server.canHandle(view,request.getUrl().toString());

            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !Server.canHandle(view,url);
            }
        });

        webView.loadUrl("http://192.168.2.2:8000/index.html");
    }
}
