package com.retriable.wvjsb;

import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.retriable.wvjsb.Functions.Function0;
import com.retriable.wvjsb.Functions.Function0Void;
import com.retriable.wvjsb.Functions.Function2Void;
import com.retriable.wvjsb.Functions.Function3Void;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Server {

    public static Server instance(final WebView webView, @Nullable String namespace){
        if (null==namespace||namespace.isEmpty()){
            namespace="wvjsb_namespace";
        }
        return get(webView, namespace, true);
    }

    /*can handle url*/
    public static boolean canHandle(final WebView webView,@Nullable final String urlString){
        Matcher matcher=pattern.matcher(urlString);
        if (!matcher.find()) return false;
        final String namespace=matcher.group(1);
        final String action=matcher.group(2);
        final Server server = get(webView,namespace,false);
        if (server ==null) return false;
        switch (action) {
            case "install":
                server.install();
                break;
            case "query":
                //is unreachable for android platform.
                server.query();
                break;
        }
        return true;
    }

    public final Handler on(final String type){
        Handler handler;
        synchronized (handlers){
            handler = handlers.get(type);
            if (null != handler) {
                return handler;
            }
            handler = new Handler();
            handlers.put(type,handler);
            return handler;
        }
    }

    private void install(){
        try{
            final BufferedReader reader = new BufferedReader(new InputStreamReader(webView.getContext().getAssets().open("Proxy.js")));
            final StringBuilder builder =new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
            evaluate(builder.toString().replace("wvjsb_namespace", namespace), null);
        }catch (Throwable t){
            t.printStackTrace();
        }
    }

    private void query(){
        evaluate(String.format(queryFormat, namespace), new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                try{
                    // TODO: 2019-05-03 Deserialization error
                    value = StringEscapeUtils.unescapeJavaScript(value);
                    List list = (List)Json.toJavaObject(value);
                    for (Object o :list){
                        postMessage((String)o);
                    }
                }catch (Throwable t){
                    t.printStackTrace();
                }
            }
        });
    }

    private void evaluate(final String string, final ValueCallback<String> valueCallback){
        final Runnable runnable=new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript(string,valueCallback);
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()){
            runnable.run();
        }else{
            webView.post(runnable);
        }
    }

    @SuppressWarnings("WeakerAccess")
    @JavascriptInterface
    public void postMessage(String s){
        try {
          final Message message=new Message(s);
            final String from = message.from;
            final String to = message.to;
            final String type = message.type;
            if (null==from){
                return;
            }
            if (null==type){
                return;
            }
            if (!namespace.equals(to)){
                return;
            }
            final String id = message.id;
            final Object parameter = message.parameter;
            final Throwable throwable = message.throwable;
            if (proxy.equals(from)){
                if ("disconnect".equals(type)){
                    Handler handler;
                    synchronized (handlers){
                        handler=handlers.get(type);
                    }
                    synchronized (connections){
                        if (null!=handler){
                            for (final Connection c: connections.values()){
                                handler.event.invoke(c,null,emptyDone);
                            }
                        }
                        connections.clear();
                    }
                }
                return;
            }
            if ("disconnect".equals(type)){
                Connection c;
                synchronized (connections){
                    c=connections.get(from);
                    if (null==c){
                        return;
                    }
                }
                final Handler handler = handlers.get("disconnect");
                if (null==handler){
                    return;
                }
                handler.event.invoke(c,null,emptyDone);
                return;
            }
            if ("connect".equals(type)){
                Connection connection;
                synchronized (connections){
                    connection=connections.get(from);
                    if (null!=connection){
                        return;
                    }

                    connection=new Connection(parameter);
                    connections.put(from,connection);
                    final  Connection c = connection;
                    connection.send=new Function3Void<String, String, Object>() {
                        @Override
                        public void invoke(final String id, String type, Object o) {
                            final Message message=new Message();
                            message.id=id;
                            message.type=type;
                            message.parameter=o;
                            message.from=namespace;
                            message.to=from;
                            try{
                                evaluate(String.format(sendFormat, namespace, correctedJSString(message.string())), new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        if (value.isEmpty()){
                                            c.ack(id,null,new Throwable("connection lost"));
                                        }
                                    }
                                });
                            }catch (Throwable t){
                                c.ack(id,null,new Throwable("connection lost"));
                                t.printStackTrace();
                            }

                        }
                    };
                }
                connection.event("connect",null);
                Handler handler;
                synchronized (handlers){
                    handler=handlers.get("connect");
                }
                if (null==handler){
                    return;
                }
                handler.event.invoke(connection,parameter,emptyDone);
                return;
            }
            if ("ack".equals(type)){
                Connection c;
                synchronized (connections){
                    c=connections.get(from);
                    if (null==c){
                        return;
                    }
                }
                c.ack(id,parameter,throwable);
                return;
            }
            final String cancelId=from+id;
            if ("cancel".equals(type)){
                Function0Void cancel;
                synchronized (cancels){
                    cancel = cancels.get(cancelId);
                }
                if (null!=cancel){
                    cancel.invoke();
                }
                return;
            }
            final Handler handler;
            synchronized (handlers){
                handler=handlers.get(type);
                if (null==handler){
                    return;
                }
            }
            Connection connection;
            synchronized (connections){
                connection=connections.get(from);
            }
            if (null==connection){
                return;
            }
            final Object context = handler.event.invoke(connection, parameter, new Function0<Function2Void<Object,
                    Throwable>>() {
                @Override
                public Function2Void<Object, Throwable> invoke() {
                    synchronized (cancels){
                        cancels.remove(cancelId);
                    }
                    return new Function2Void<Object, Throwable>() {
                        @Override
                        public void invoke(Object o, Throwable throwable) {
                            final Message message=new Message();
                            message.id=id;
                            message.from=namespace;
                            message.to=from;
                            message.type="ack";
                            message.parameter=o;
                            message.throwable=throwable;
                            try{
                                evaluate(String.format(sendFormat, namespace, correctedJSString(message.string())), null);
                            }catch (Throwable t){
                                t.printStackTrace();
                            }
                        }
                    };
                }
            });
            if (null!=handler.cancel){
                synchronized (cancels){
                    cancels.put(cancelId, new Function0Void() {
                        @Override
                        public void invoke() {
                            handler.cancel.invoke(context);
                        }
                    });
                }
            }
        }catch (Throwable t){
            t.printStackTrace();
        }

    }

    private Server(WebView webView, String namespace){
        super();
        this.webView=webView;
        this.namespace=namespace;
        this.proxy=namespace+"_wvjsb_proxy";
//        this.webView.addJavascriptInterface(this,namespace);
    }

    private final Map<String,Handler> handlers=new HashMap<>();
    private final String namespace;
    private final String proxy;
    private final WebView webView;
    private final Map<String,Connection> connections=new HashMap<>();
    private final Map<String, Function0Void> cancels=new HashMap<>();
    private static final String queryFormat=";(function(){try{ return window['%s_wvjsb_proxy'].query();}catch(e){return '[]'}; })();";
    private static final String sendFormat=";(function(){try{return window['%s_wvjsb_proxy'].send('%s');}catch(e){return ''};})();";
    private static final Pattern pattern= Pattern.compile("^https://wvjsb/([^/]+)/([^/]+)$");
    private static final WeakHashMap<WebView, HashMap<String, Server>> serversByWebView=new WeakHashMap<>();
    private static final Function0<Function2Void<Object,Throwable>> emptyDone =  new Function0<Function2Void<Object,Throwable>>() {
        @Override
        public Function2Void<Object,Throwable> invoke() {
            return new Function2Void<Object,Throwable>() {
                @Override
                public void invoke(Object o, Throwable o2) {
                }
            };
        }
    };

    private static Server get(WebView webView, String namespace, boolean flag){
        synchronized (Server.class){
            HashMap<String, Server> serversByNamespace=serversByWebView.get(webView);
            if (serversByNamespace==null){
                serversByNamespace= new HashMap<>();
                serversByWebView.put(webView,serversByNamespace);
            }
            Server server =serversByNamespace.get(namespace);
            if (!flag) return server;
            if (server !=null) return server;
            server = new Server(webView,namespace);
            serversByNamespace.put(namespace, server);
            return server;
        }
    }

    private static String correctedJSString(String s){
        s=s.replace("\\","\\\\");
        s=s.replace("\"","\\\"");
        s=s.replace("\'","\\\'");
        s=s.replace("\n","\\n");
        s=s.replace("\r","\\r");
        s=s.replace("\f","\\f");
        s=s.replace("\b","\\b");
        s=s.replace("\t","\\t");
        s=s.replace("\u2028","\\u2028");
        s=s.replace("\u2029","\\u2029");
        return s;
    }
}
