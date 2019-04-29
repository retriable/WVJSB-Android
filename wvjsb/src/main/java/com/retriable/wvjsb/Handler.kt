package com.retriable.wvjsb

class Handler {


    fun onEvent(onEvent:(connection: Connection,parameter: Any?,done:()->((result:Any?,error:Error?)->Any?))->Unit){

    }

    fun onCancel(onCancel:(context:Any?)->Unit){

    }

    internal var _onEvent:((connection: Connection,parameter: Any?,done:()->((result:Any?,error:Error?)->Any?))->Unit)?=null
    internal var _onCancel:((context:Any?)->Unit)?=null

}