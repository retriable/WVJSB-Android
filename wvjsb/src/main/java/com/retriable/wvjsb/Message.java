package com.retriable.wvjsb;

import com.alibaba.fastjson.JSON;

import java.util.HashMap;
import java.util.Map;

class Message {
    String id;
    String type;
    String from;
    String to;
    Object parameter;
    Error  error;

    Message(){
        super();
    }

    Message(String s) throws Exception{
        super();
        Object o = JSON.parse(s);
        Map<String,Object> map=(Map<String,Object>)o;
        id=(String)map.get("id");
        type=(String)map.get("type");
        from=(String)map.get("from");
        to=(String)map.get("to");
        parameter=map.get("parameter");
        Object e =map.get("error");
        if (e instanceof String){
            error=new Error((String)e);
        }else if (e instanceof Map){
            error=new Error((String)((Map)e).get("description"));
        }
    }
    String string() throws Exception{
        HashMap<String,Object> map=new HashMap<>();
        map.put("id",id);
        map.put("type",type);
        map.put("from",from);
        map.put("to",to);
        map.put("parameter",parameter);
        map.put("error",error.toString());
        return JSON.toJSONString(map);
    }
}
