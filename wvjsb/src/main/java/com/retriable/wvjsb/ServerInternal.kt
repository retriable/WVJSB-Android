package com.retriable.wvjsb

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.alibaba.fastjson.JSON

internal class ServerInternal(private var webView: WebView,private var namespace: String){

    init{
        webView.addJavascriptInterface(this,namespace)
    }

    companion object {
        val queryFormat : String = ";(function(){try{ return window['%s_wvjsb_proxy'].query();}catch(e){return []}; })();"
        val sendFormat : String = ";(function(){try{return window['%s_wvjsb_proxy'].send('%s');}catch(e){return ''};})();"
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

    fun install(){

    }

    fun query(){
        webView.evaluateJavascript(queryFormat){value ->
            val o =JSON.parse(value)
            if (o is ArrayList<*>){
                for (s:Any in o){
                    if (s is String){
                        postMessage(s)
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun postMessage(s:String){
        val message=Message(s)

    }

    fun sendMessage(m:Message,callback:((Boolean)->Unit)?){
        var s = m.string()
        if (s.length==0) {
            return
        }
        s = String.format(sendFormat, namespace, correctedJSString(s))
        webView.evaluateJavascript(s){
            value->
            if (callback!=null) callback(value.length>0)
        }
    }

    fun on(type: String) {

    }

}