package com.retriable.wvjsb;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

class Message {

    @Nullable
    String    id;
    @Nullable
    String    type;
    @Nullable
    String    from;
    @Nullable
    String    to;
    @Nullable
    Object    parameter;
    @Nullable
    Throwable throwable;

    Message(){
        super();
    }

    @SuppressWarnings("unchecked")
    Message(@Nullable String s) throws Throwable{
        this();
        if (null==s||s.isEmpty()){
            return;
        }
        Map<String,Object> map = (Map<String,Object>) Json.toJavaObject(s);
        Object o = map.get("id");
        if (null!=o){
            id=(String)o;
        }
        o = map.get("type");
        if (null!=o){
            type=(String)o;
        }
        o = map.get("from");
        if (null!=o){
            from=(String)o;
        }
        o = map.get("to");
        if (null!=o){
            to=(String)o;
        }
        parameter = map.get("parameter");
        Object error = map.get("error");
        if (error instanceof String){
            throwable=new Throwable((String)error);
        }else if (error instanceof Map){
            throwable=new Throwable((String)((Map)error).get("description"));
        }
    }

    @Nullable String string() throws Throwable{
        Map<String,Object> map=new HashMap<>();
        if (null!=id){
            map.put("id",id);
        }
        if (null!=type){
            map.put("type",type);
        }
        if (null!=from){
            map.put("from",from);
        }
        if (null!=to){
            map.put("to",to);
        }
        if (null!=parameter){
            map.put("parameter",parameter);
        }
        if (null!=throwable){
            Map<String,String> error = new HashMap<>();
            error.put("description",throwable.getMessage());
            if (null!=throwable){
                map.put("error",error);
            }
        }
        return Json.toJsonString(map);
    }
}
