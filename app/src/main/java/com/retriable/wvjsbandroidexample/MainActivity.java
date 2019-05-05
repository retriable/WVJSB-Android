package com.retriable.wvjsbandroidexample;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.retriable.wvjsb.Connection;
import com.retriable.wvjsb.Functions.Function0;
import com.retriable.wvjsb.Functions.Function1Void;
import com.retriable.wvjsb.Functions.Function2Void;
import com.retriable.wvjsb.Functions.Function3;
import com.retriable.wvjsb.Functions.Function3Void;
import com.retriable.wvjsb.Operation;
import com.retriable.wvjsb.Server;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        final WebView webView=findViewById(R.id.webView);
        WebView.setWebContentsDebuggingEnabled(true);
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (Server.canHandle(webView, url)) {
                    return null;
                }
                return super.shouldInterceptRequest(view, url);
            }
        });
        final Server server = Server.instance(webView,null);

        server.on("connect").onEvent(new Function3<Connection, Object,
                Function0<Function2Void<Object, Throwable>>, Object>() {
            @Override
            public Object invoke(Connection connection, Object o, Function0<Function2Void<Object,
                    Throwable>> function2VoidFunction0) {
                synchronized (connections){
                    connections.add(connection);
                }
                Log.d("app", "connect: "+o.toString());
                function2VoidFunction0.invoke();
                return null;
            }
        });

        server.on("disconnect").onEvent(new Function3<Connection, Object,
                Function0<Function2Void<Object, Throwable>>, Object>() {
            @Override
            public Object invoke(Connection connection, Object o, Function0<Function2Void<Object,
                    Throwable>> function2VoidFunction0) {
                synchronized (connections){
                    connections.remove(connection);
                }
                Log.d("app", "disconnect: "+ (connection.info != null ?
                            connection.info.toString() : null));
                function2VoidFunction0.invoke();
                return null;
            }
        });

        server.on("immediate").onEvent(new Function3<Connection, Object,
                Function0<Function2Void<Object, Throwable>>, Object>() {
            @Override
            public Object invoke(Connection connection, Object o, Function0<Function2Void<Object,
                    Throwable>> function2VoidFunction0) {
                function2VoidFunction0.invoke().invoke("[\\] ['] [\"] [\b] [\f] [\n] [\r] [\t] [\u2028] [\u2029]",null);
                return null;
            }
        });
        server.on("delayed").onEvent(new Function3<Connection, Object,
                Function0<Function2Void<Object, Throwable>>, Object>() {
            @Override
            public Object invoke(Connection connection, Object o,final Function0<Function2Void<Object,
                    Throwable>> function2VoidFunction0) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if(Math.random()<0.50){
                            function2VoidFunction0.invoke().invoke(null,new Throwable("can not find host"));
                        }else{
                            function2VoidFunction0.invoke().invoke("[\\] ['] [\"] [\b] [\f] [\n] [\r] [\t] [\u2028] [\u2029]",null);
                        }
                    }
                };
                handler.postDelayed(runnable,2000);
                return runnable;
            }
        }).onCancel(new Function1Void<Object>() {
            @Override
            public void invoke(Object o) {
                handler.removeCallbacks((Runnable) o);
            }
        });
        reload(null);
    }

    public void reload(View v){
        final WebView webView=findViewById(R.id.webView);
        webView.clearCache(true);
        webView.reload();
        // TODO make sure the URL is consistent to your web server
        webView.loadUrl("http://192.168.2.2:8000/index.html");
    }

    public void immediate(View v){
        synchronized (connections){
            for (Connection c :connections){
                Operation operation =  c.event("immediate",null).timeout(10000).onAck(new Function3Void<Operation, Object,
                        Throwable>() {
                    @Override
                    public void invoke(Operation operation, Object o, Throwable throwable) {
                        if (null!=throwable){
                            Log.d("app", "did receive immediate error: "+throwable.getMessage());
                        }else{
                            Log.d("app", "did receive immediate ack: "+o.toString());
                        }
                    }
                });
                synchronized (operations){
                    operations.add(operation);
                }
            }
        }
    }

    public void delayed(View v){
        synchronized (connections){
            for (Connection c :connections){
                Operation operation =  c.event("delayed",null).timeout(10000).onAck(new Function3Void<Operation, Object,
                        Throwable>() {
                    @Override
                    public void invoke(Operation operation, Object o, Throwable throwable) {
                        if (null!=throwable){
                            Log.d("app", "did receive delayed error: "+throwable.getMessage());
                        }else{
                            Log.d("app", "did receive delayed ack: "+o.toString());
                        }
                    }
                });
                synchronized (operations){
                    operations.add(operation);
                }
            }
        }
    }

    public void cancel(View v){
        synchronized (operations){
            for (Operation o:operations){
                o.cancel();
            }
            operations.clear();
        }
    }

    private final ArrayList<Connection> connections = new ArrayList<>();
    private final ArrayList<Operation> operations= new ArrayList<>();
    private static final Handler handler=new Handler();
}
