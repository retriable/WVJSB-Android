package com.retriable.wvjsb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.retriable.wvjsb.Functions.Function1Void;
import com.retriable.wvjsb.Functions.Function2Void;
import com.retriable.wvjsb.Functions.Function4Void;

public final class Server {

    public final HashMap<String,Connection> connections=new HashMap<>();

    /*get server and create one if not exist*/
    public static final @NonNull Server getInstance(@NonNull WebView webView,@Nullable String namespace) throws Exception{
        if (namespace==null||namespace.length()==0){
            namespace="wvjsb_namespace";
        }
        return get(webView,namespace,true);
    }

    /*can handle url*/
    public static final boolean canHandle(@NonNull WebView webView,@NonNull String urlString) throws Exception{
        Matcher matcher=pattern.matcher(urlString);
        if (matcher==null) return false;
        if (!matcher.find()) return false;
        String namespace=matcher.group(1);
        String action=matcher.group(2);
        Server server = get(webView,namespace,false);
        if (server ==null) return false;
        switch (action) {
            case "install":
                server.install();
                break;
            case "query":
                server.query();
                break;
            default:
                return false;
        }
        return true;
    }

    public final void on(@NonNull String type){

    }

    private HashMap<String,Handler> handlers=new HashMap<>();
    private String namespace;
    private WebView webView;

    private void install(){
        //怎么添加资源文件啊？

    }

    private void query(){
        webView.evaluateJavascript(String.format(queryFormat,namespace),new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String string) {
                ArrayList<String> arr= (ArrayList<String>)JSON.parse(string);
                for (String s: arr) {
                    postMessage(s);
                }
            }
        });
    }

    private void sendMessage(String s, ValueCallback<String> callback){
        webView.evaluateJavascript(String.format(sendFormat, namespace, correctedJSString(s)), callback);
    }

    private void send(Message message, ValueCallback<String> callback){
        try{
            String s = message.string();
            if (s.length()==0){
                callback.onReceiveValue("");
                return;
            }
            sendMessage(s, callback);
        }catch (Exception e){

        }
    }

    @JavascriptInterface
    public void postMessage(String s){
        try {
            Message message=new Message(s);
            if (message.from==null||message.from.equals("")){
                Handler handler=handlers.get("disconnect");
                if (handler!=null){
                    for (Connection connection: connections.values()) {
                        handler._onEvent.invoke(connection, null, new Function2Void<Object, Error>() {
                            @Override
                            public void invoke(Object o, Error error) {

                            }
                        });
                    }
                }
                connections.clear();
                return;
            }
            if (!message.to.equals(namespace)){
                return;
            }
            if (message.type.equals("connect")){
                Connection connection=connections.get(message.from);
                if (connection!=null) return;
                connection=new Connection(message.parameter, new Function4Void<Connection, String, String
                        , Object>() {
                    @Override
                    public void invoke(Connection connection, String id, String type, Object parameter) {
                        Message m=new Message();
                        m.id=id;
                        m.type=type;
                        m.parameter=parameter;
//                        m.from=message.to;
//                        m.to=message.from;

                        send(m, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                boolean success=value.length()>0;
                                if (!success){
//                                    connection.ack(m.id,null,new Error(""));
                                }
                            }
                        });
                    }
                });
                connections.put(message.from,connection);
                connection.event("connect",null);

            }
        }catch (Exception e){}

    }

    private Server(WebView webView,String namespace){
        super();
        this.webView=webView;
        this.namespace=namespace;
        this.webView.addJavascriptInterface(this,namespace);
    }

    private static final String queryFormat=";(function(){try{ return window['%s_wvjsb_proxy'].query();}catch(e){return []}; })();";
    private static final String sendFormat=";(function(){try{return window['%s_wvjsb_proxy'].send('%s');}catch(e){return ''};})();";
    private static final Pattern pattern= Pattern.compile("^https://wvjsb/([^/]+)/([^/]+)$");
    private static final WeakHashMap<WebView, HashMap<String, Server>> serversByWebView=new WeakHashMap<>();

    private static Server get(WebView webView, String namespace, boolean flag) throws Exception{
        if (webView==null) {
            throw new Exception("web view can't be null");
        }
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
