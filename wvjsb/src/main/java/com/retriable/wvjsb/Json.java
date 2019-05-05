package com.retriable.wvjsb;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

final class Json {
    private Json(){super();}

    /**
     * convert map or list to json string
     * @param o map or list
     * @return string
     * @throws Throwable throwable
     */
    static String toJsonString(@Nullable Object o) throws Throwable{
        if ((o instanceof Map) || (o instanceof List)){
            Object oo = toJSONObject(o);
            if (null!=oo){
                return oo.toString();
            }
        }
       throw new Throwable("only support Map and List");
    }

    /**
     * covert json string to map or list
     * @param s json string
     * @return map or list
     * @throws Throwable throwable
     */
    static Object toJavaObject(@Nullable String s) throws Throwable{
        if (null==s){
            throw new NullPointerException("json string can't be null");
        }
        try{
            return toJavaObject(new JSONObject(s),true);
        }catch (Throwable t1){
            return toJavaObject(new JSONArray(s),true);
        }
    }

    /**
     * covert map or list to json object or json array
     * @param o map or list
     * @return json object
     * @throws Throwable throwable
     */
    @SuppressWarnings("unchecked")
     private static @Nullable Object toJSONObject(Object o) throws Throwable {
        if (o instanceof Map){
            final JSONObject object = new JSONObject();
            Map<String,Object> map = (Map<String,Object>)o;
            for (Map.Entry<String,Object> entry: map.entrySet()){
                object.put(entry.getKey(),toJSONObject(entry.getValue()));
            }
            return object;
        }
        if (o instanceof List){
            final JSONArray array = new JSONArray();
            List<Object> list = (List<Object>)o;
            for (Object obj: list){
                array.put(toJSONObject(obj));
            }
            return o;
        }
        return o;
    }

    /**
     * convert json object or json array to object
     * @param o json array or json object ,object
     * @param strict strict json object or json array
     * @return  Object
     * @throws Throwable throwable
     */
    private static @Nullable Object toJavaObject(@Nullable Object o,Boolean strict) throws Throwable{
        if (null==o){
            return null;
        }
        if (o instanceof JSONObject){
            Map<String,Object> map = new HashMap<>();
            Iterator<String> iterator=((JSONObject)o).keys();
            while (iterator.hasNext()){
                String k = iterator.next();
                Object oo = toJavaObject(((JSONObject)o).get(k),false);
                if (null!=oo){
                    map.put(k,oo);
                }
            }
            return map;
        }
        if (o instanceof JSONArray){
            List<Object> list = new ArrayList<>();
            for (int i=0,count=((JSONArray)o).length();i<count;i++){
                Object oo = toJavaObject(((JSONArray)o).get(i),false);
                if (null!=oo){
                    list.add(oo);
                }
            }
            return list;
        }
        if (strict){
            throw new Throwable("only support JSONObject and JSONArray");
        }
        return o;
    }


}
