package com.retriable.wvjsb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Server {

    /*get server and create one if not exist*/
    public static @NonNull Server getInstance(@NonNull WebView webView,@Nullable String namespace) throws Exception{
        if (namespace==null||namespace.length()==0){
            namespace="wvjsb_namespace";
        }
        return get(webView,namespace,true);
    }

    /*can handle url*/
    public static boolean canHandle(@NonNull WebView webView,@NonNull String urlString) throws Exception{
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

    public void on(@NonNull String type){

    }

    public @Nullable Connection getConnection(@NonNull String s){
        return connections.get(s);
    }


    private String namespace;
    private WebView webView;
    private static final String queryFormat=";(function(){try{ return window['%s_wvjsb_proxy'].query();}catch(e){return []}; })();";
    private static final String sendFormat=";(function(){try{return window['%s_wvjsb_proxy'].send('%s');}catch(e){return ''};})();";
    private HashMap<String,Connection> connections=new HashMap<>();
    private static final Pattern pattern= Pattern.compile("^https://wvjsb/([^/]+)/([^/]+)$");
    private static WeakHashMap<WebView, HashMap<String, Server>> serversByWebView=new WeakHashMap<>();

    private void putConnection(Connection connection){
        connections.put(connection.id,connection);
    }

    private void handleMessage(Message message){

    }

    private void sendMessage(Message message){
        try {//how to do when api level < 19?
            webView.evaluateJavascript(String.format(sendFormat, namespace, CorrectedJSString(message.string())), new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    try {
                        Message message = new Message(s);
                        handleMessage(message);
                    } catch (Exception e) {}
                }
            });
        }catch (Exception e){}
    }

    private void install(){
        //怎么添加资源文件啊？
    }

    private void query(){
        webView.evaluateJavascript(String.format(queryFormat,namespace),new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                try {
                    Message message=new Message(s);
                    handleMessage(message);
                }catch (Exception e){}
            }
        });
    }

    @JavascriptInterface
    public void postMessage(String s){
        try {
            Message message=new Message(s);
            handleMessage(message);
        }catch (Exception e){}
    }

    private Server(WebView webView,String namespace){
        super();
        this.webView=webView;
        this.namespace=namespace;
        this.webView.addJavascriptInterface(this,namespace);
    }

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

    private static String CorrectedJSString(String s){
        s=s.replace("\\","\\\\");
        s=s.replace("\"","\\\"");
        s=s.replace("\'","\\\'");
        s=s.replace("\n","\\n");
        s=s.replace("\r","\\r");
        s=s.replace("\f","\\f");
        s=s.replace("\u2028","\\u2028");
        s=s.replace("\u2029","\\u2029");
        return s;
    }
}
