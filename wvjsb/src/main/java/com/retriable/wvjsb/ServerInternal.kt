package com.retriable.wvjsb

import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.alibaba.fastjson.JSON
import java.io.BufferedReader
import java.lang.Exception

internal class ServerInternal(private var webView: WebView, private var namespace: String) {

    init {
        webView.addJavascriptInterface(this, namespace)
    }

    companion object {
        const val queryFormat: String = ";(function(){try{ return window['%s_wvjsb_proxy'].query();}catch(e){return []}; })();"
        const val sendFormat: String = ";(function(){try{return window['%s_wvjsb_proxy'].send('%s');}catch(e){return ''};})();"
        fun correctedJSString(str: String): String {
            var s = str
            s = s.replace("\\", "\\\\")
            s = s.replace("\"", "\\\"")
            s = s.replace("\'", "\\\'")
            s = s.replace("\n", "\\n")
            s = s.replace("\r", "\\r")
            s = s.replace("\b", "\\b")
            s = s.replace("\t", "\\t")
            s = s.replace("\u000C", "\\u000C")
            s = s.replace("\u2028", "\\u2028")
            s = s.replace("\u2029", "\\u2029")
            return s
        }
    }

    fun install() {
        val reader = BufferedReader(webView.context.assets.open("Proxy.js").reader())
        val js = reader.readText()
        webView.post{
            webView.evaluateJavascript(js.replace("wvjsb_namespace", namespace)) { value ->
                System.console()?.printf("\n%s", value)
            }
        }
        reader.close()
    }

    fun query() {
        webView.post{
            webView.evaluateJavascript(String.format(queryFormat, namespace)) { value ->
                try {
                    val o = JSON.parse(value) as ArrayList<*>
                    for (s in o) {
                        postMessage(s as String)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    @JavascriptInterface
    fun postMessage(s: String) {
        val message = Message(s)
        try {
            val from = message.from!!
            if (!message.to.equals(namespace)) {
                return
            }
            when (message.type) {
                "disconnect" -> {
                    try {
                        val connection = connections[from]!!
                        connections.remove(from)
                        handlers["disconnect"]?.eventClosure?.invoke(connection, null) {
                            { _, _ ->
                            }
                        }
                    } catch (e: Throwable) {
                    }
                }
                "connect" -> {
                    try {
                        var connection = connections[from]
                        if (connection != null) {
                            return
                        }
                        connection = Connection(message.parameter) { c_, id_, type_, parameter_ ->
                            val m = Message()
                            m.id = id_
                            m.type = type_
                            m.parameter = parameter_
                            m.from = namespace
                            m.to = from
                            sendMessage(m) { v ->
                                if (v != null) {
                                    c_.ack(id_, null, v)
                                }
                            }
                        }
                        val m = Message()
                        m.type="connect"
                        m.from=namespace
                        m.to=from
                        sendMessage(m,null)
                        connections[from] = connection
                    } catch (e: Throwable) {
                    }
                }
                "ack" -> {
                    try {
                        val id = message.id!!
                        connections[from]?.ack(id, message.parameter, message.t)
                    } catch (e: Throwable) {
                    }
                }
                "cancel" -> {
                    try {
                        val id = message.id!!
                        cancelClosures[id]?.invoke()
                    } catch (e: Throwable) {
                    }
                }
                else -> {
                    try {
                        val id = message.id!!
                        val type = message.type!!
                        val connection = connections[from]!!
                        val handler = handlers[type]!!
                        val event = handler.eventClosure!!
                        val cancelKey = String.format("%s-%s", from, id)
                        val context = event.invoke(connection, message.parameter) {
                            { result, t ->
                                val m = Message()
                                m.id = id
                                m.type = "ack"
                                m.parameter = result
                                m.t = t
                                m.from = namespace
                                m.to = from
                                sendMessage(m, null)
                            }
                        }
                        val cancelClosure = handler.cancelClosure!!
                        cancelClosures[cancelKey] = {
                            cancelClosure(context)
                        }
                    } catch (e: Throwable) {
                    }
                }
            }
        } catch (e: Throwable) {
            try {
                val event = handlers["disconnect"]!!.eventClosure!!
                connections.values.forEach { c ->
                    event(c, null) {
                        { _, _ ->
                        }
                    }
                }
            } catch (e: Throwable) {
            }
        }
    }

    private fun sendMessage(m: Message, callback: ((Throwable?) -> Unit)?) {
        webView.post{
            webView.evaluateJavascript(String.format(sendFormat, namespace, correctedJSString(m.string()))) { value ->
                try {
                    if (value.isEmpty()) {
                        throw Throwable("evaluateJavascript failed")
                    }
                    callback?.invoke(null)
                } catch (e: Throwable) {
                    callback?.invoke(e)
                }
            }
        }
    }

    fun on(type: String):Handler {
        var handler = handlers[type]
        if (handler != null) {
            return handler
        }
        handler = Handler()
        handlers[type] = handler
        return handler
    }

    private val handlers = HashMap<String, Handler>()
    private val connections = HashMap<String, Connection>()
    private val cancelClosures = HashMap<String, () -> Unit>()
}