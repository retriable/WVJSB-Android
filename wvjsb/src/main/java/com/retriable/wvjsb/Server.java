package com.retriable.wvjsb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.webkit.WebView;


import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public final class Server {

    /*get server and create one if not exist*/
    public static @NonNull Server getInstance(@NonNull WebView webView,@Nullable String namespace){
        if (namespace==null||namespace.length()==0) {
            namespace = "wvjsb_namespace";
        }
        return get(webView,namespace,true);
    }

    /*can handle url*/
    public static boolean canHandle(@NonNull WebView webView,@NonNull String urlString){
        Matcher matcher=pattern.matcher(urlString);
        if (matcher==null) return false;
        if (!matcher.find()) return false;
        String namespace=matcher.group(1);
        String action=matcher.group(2);
        Server server = get(webView,namespace,false);
        if (server ==null) return false;
        switch (action) {
            case "install":
                server.internal.install();
                break;
            case "query":
                server.internal.query();
                break;
            default:
                return false;
        }
        return true;
    }

    public void on(@NonNull String type){
        internal.on(type);
    }

    private static WeakHashMap<WebView, HashMap<String, Server>> serversByWebView=new WeakHashMap<>();
    private ServerInternal internal;
    private static Pattern pattern= Pattern.compile("^https://wvjsb/([^/]+)/([^/]+)$");

    private Server(WebView webView,String namespace){
        super();
        this.internal=new ServerInternal(webView,namespace);
    }

    private static Server get(WebView webView, String namespace, boolean flag){
        if (webView==null) return null;
        if (namespace.length()==0) return null;
        synchronized (Server.class){
            HashMap<String, Server> serversByNamespace=serversByWebView.get(webView);
            if (serversByNamespace==null){
                serversByNamespace= new HashMap<>();
                serversByWebView.put(webView,serversByNamespace);
            }
            Server server =serversByNamespace.get(namespace);
            if (server !=null) return server;
            if (!flag) return null;
            server = new Server(webView,namespace);
            serversByNamespace.put(namespace, server);
            return server;
        }
    }

}
