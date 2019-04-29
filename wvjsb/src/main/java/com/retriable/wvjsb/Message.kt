package com.retriable.wvjsb

import com.alibaba.fastjson.JSON

internal class Message {

    var id:String?=null
    var type:String?=null
    var from:String?=null
    var to:String?=null
    var parameter:Any?=null
    var error:Error?=null

    constructor()

    @Throws(Exception::class)
    constructor(s:String){
        val o = JSON.parse(s)
        if (o is Map<*,*>){
           o.forEach { entry->
               var value = entry.value
               when(entry.key){
                   "id" -> id = value as? String
                   "type"-> type = value as? String
                   "from"-> from = value as? String
                   "to"-> to = value as? String
                   "parameter"-> parameter = value
                   "error"-> {
                       when(value){
                           is String->error=Error(value)
                           is Map<*,*>->{
                               value= value["description"]
                               if (value is String){
                                   error=Error(value)
                               }
                           }
                       }
                   }
               }
           }
        }
    }

    fun string() :String{
        val map = HashMap<String,Any?>()
        map["id"]=id
        map["type"]=type
        map["from"]=from
        map["to"]=to
        map["parameter"]=parameter
        map["error"]=error?.message
        return JSON.toJSONString(map)
    }
}