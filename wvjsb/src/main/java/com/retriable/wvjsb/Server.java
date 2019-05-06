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

import java.util.HashMap;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Server {
    /**
     * get instance of Server.server's life cycle is associated web view.
     * @param webView web view.
     * @param namespace namespace is used to mark different service.
     * @return server
     */
    public static Server instance(WebView webView, @Nullable String namespace){
        if (null==namespace||namespace.isEmpty()){
            namespace="wvjsb_namespace";
        }
        return get(webView, namespace, true);
    }

    /**
     * check that any server can handle url.
     * @param webView web view
     * @param urlString url string
     * @return can?
     */
    public static boolean canHandle(WebView webView,@Nullable String urlString){
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
//               server.query();
                break;
                default:
                    break;
        }
        return true;
    }

    /**
     * create a handler by event type.
     * @param type event type
     * @return handler
     */
    public final Handler on(String type){
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
        evaluate(installJs.replace("wvjsb_namespace",namespace),null);
    }

//    private void query(){
//        evaluate(String.format(queryFormat, namespace), new ValueCallback<String>() {
//            @Override
//            public void onReceiveValue(String value) {
//                try{
//                    value = StringUtils.unescape(value.substring(1,value.length()-1));
//                    List list = (List)Json.toJavaObject(value);
//                    for (Object o :list){
//                        postMessage((String)o);
//                    }
//                }catch (Throwable t){
//                    t.printStackTrace();
//                }
//            }
//        });
//    }

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

    @SuppressWarnings({"unused"})
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
                                evaluate(String.format(sendFormat, namespace, StringUtils.escape(message.string())), new ValueCallback<String>() {
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
                                evaluate(String.format(sendFormat, namespace, StringUtils.escape(message.string())), null);
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
        this.webView.addJavascriptInterface(this,namespace);
    }

    private final Map<String,Handler> handlers=new HashMap<>();
    private final String namespace;
    private final String proxy;
    private final WebView webView;
    private final Map<String,Connection> connections=new HashMap<>();
    private final Map<String, Function0Void> cancels=new HashMap<>();
    @SuppressWarnings("unused")
    private static final String queryFormat=";(function(){try{return window['%s_wvjsb_proxy'].query();}catch(e){return '[]'};})();";
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

    private static final String installJs = ";\n" +
            "(function() {\n" +
            "\tconst namespace = 'wvjsb_namespace';\n" +
            "\tconst proxyKey = namespace + '_wvjsb_proxy';\n" +
            "\n" +
            "\tfunction getProxy() {\n" +
            "\t\treturn window[proxyKey];\n" +
            "\t}\n" +
            "\n" +
            "\tfunction setProxy(proxy) {\n" +
            "\t\twindow[proxyKey] = proxy;\n" +
            "\t};\n" +
            "\n" +
            "\tlet proxy = getProxy();\n" +
            "\tif (proxy) return 'wvjsb proxy was already installed';\n" +
            "\tconst queryURL = 'https://wvjsb/' + namespace + '/query';\n" +
            "\tconst messageBuffers = [];\n" +
            "\tconst clients = {};\n" +
            "\tconst messageHandlerKey = namespace;\n" +
            "\tconst sendToClient = function(client, message) {\n" +
            "\t\tconst data = {};\n" +
            "\t\tdata[namespace] = message;\n" +
            "\t\tclient.postMessage(data, '*');\n" +
            "\t}\n" +
            "\tconst sendToServer = (function() {\n" +
            "\t\tlet v = null;\n" +
            "\t\ttry {\n" +
            "\t\t\tv = window[messageHandlerKey].postMessage; //android WebView\n" +
            "\t\t\tif (v) return function(message) {\n" +
            "\t\t\t\ttry {\n" +
            "\t\t\t\t\twindow[messageHandlerKey].postMessage(JSON.stringify(message));\n" +
            "\t\t\t\t} catch (e) {}\n" +
            "\t\t\t};\n" +
            "\t\t} catch (e) {}\n" +
            "\t\ttry {\n" +
            "\t\t\tv = webkit.messageHandlers[messageHandlerKey].postMessage; //WKWebView\n" +
            "\t\t\tif (v) return function(message) {\n" +
            "\t\t\t\ttry {\n" +
            "\t\t\t\t\twebkit.messageHandlers[messageHandlerKey].postMessage(JSON.stringify(message));\n" +
            "\t\t\t\t} catch (e) {}\n" +
            "\t\t\t};\n" +
            "\t\t} catch (e) {}\n" +
            "\t\tv = function(message) { //iOS UIWebView WebView\n" +
            "\t\t\ttry {\n" +
            "\t\t\t\tmessageBuffers.push(JSON.stringify(message));\n" +
            "\t\t\t\tconst iframe = document.createElement('iframe');\n" +
            "\t\t\t\tiframe.style.display = 'none';\n" +
            "\t\t\t\tiframe.src = queryURL;\n" +
            "\t\t\t\tdocument.documentElement.appendChild(iframe);\n" +
            "\t\t\t\tsetTimeout(function() {\n" +
            "\t\t\t\t\tdocument.documentElement.removeChild(iframe);\n" +
            "\t\t\t\t}, 1);\n" +
            "\t\t\t} catch (e) {}\n" +
            "\t\t};\n" +
            "\t\treturn v;\n" +
            "\t})();\n" +
            "\tproxy = {\n" +
            "\t\tquery: function() {\n" +
            "\t\t\tconst jsonString = JSON.stringify(messageBuffers);\n" +
            "\t\t\tmessageBuffers.splice(0, messageBuffers.length);\n" +
            "\t\t\treturn jsonString;\n" +
            "\t\t},\n" +
            "\t\tsend: function(jsonString) {\n" +
            "\t\t\tconst message = JSON.parse(jsonString);\n" +
            "\t\t\tconst {\n" +
            "\t\t\t\tto\n" +
            "\t\t\t} = message;\n" +
            "\t\t\tconst client = clients[to];\n" +
            "\t\t\tif (client) sendToClient(client,message);\n" +
            "\t\t\treturn 'true';\n" +
            "\t\t}\n" +
            "\t}\n" +
            "\tsetProxy(proxy);\n" +
            "\n" +
            "\twindow.addEventListener('message', function({\n" +
            "\t\tsource, data\n" +
            "\t}) {\n" +
            "\t\ttry {\n" +
            "\t\t\tconst message = data[namespace];\n" +
            "\t\t\tif (!message) return;\n" +
            "\t\t\tconst {\n" +
            "\t\t\t\tfrom = null, to = null\n" +
            "\t\t\t} = message;\n" +
            "\t\t\tif (!from) return;\n" +
            "\t\t\tlet client = clients[from];\n" +
            "\t\t\tif (client) {\n" +
            "\t\t\t\tif (client != source) {\n" +
            "\t\t\t\t\tthrow 'client window mismatched';\n" +
            "\t\t\t\t}\n" +
            "\t\t\t} else {\n" +
            "\t\t\t\tclients[from] = source;\n" +
            "\t\t\t}\n" +
            "\t\t\tif (to != namespace) return;\n" +
            "\t\t\tsendToServer(message);\n" +
            "\t\t} catch (e) {}\n" +
            "\t});\n" +
            "\n" +
            "\tfunction broadcast(wd, data) {\n" +
            "\t\twd.postMessage(data, '*');\n" +
            "\t\tconst frames = wd.frames;\n" +
            "\t\tfor (let i = 0; i < frames.length; i++) {\n" +
            "\t\t\tbroadcast(frames[i]);\n" +
            "\t\t}\n" +
            "\t};\n" +
            "\n" +
            "\tfunction proxyConnect() {\n" +
            "\t\tconst data = {};\n" +
            "\t\tdata[namespace] = {from:proxyKey,type:'connect'};\n" +
            "\t\tbroadcast(window, data);\n" +
            "\t}\n" +
            "\n" +
            "\tfunction proxyDisconnect() {\n" +
            "\t\tsendToServer({from:proxyKey,to:namespace,type:'disconnect'});\n" +
            "\t\tconst data = {};\n" +
            "\t\tdata[namespace] = {from:proxyKey,type:'disconnect'};\n" +
            "\t\tbroadcast(window, data);\n" +
            "\t}\n" +
            "\n" +
            "\twindow.addEventListener('unload', function() {\n" +
            "\t\tproxyDisconnect();\n" +
            "\t});\n" +
            "\n" +
            "\tproxyConnect();\n" +
            "\treturn 'wvjsb proxy was installed';\n" +
            "})();";
}
