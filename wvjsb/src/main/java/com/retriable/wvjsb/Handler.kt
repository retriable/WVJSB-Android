package com.retriable.wvjsb

class Handler {

    fun onEvent(closure:((connection: Connection,parameter: Any?,done:()->((result:Any?,t:Throwable?)->Unit))->Any?)):Handler{
        this.eventClosure=closure
        return this
    }

    fun onCancel(closure:(context:Any?)->Unit){
        this.cancelClosure=closure
    }

    internal var eventClosure:((connection: Connection, parameter: Any?, done:()->((result:Any?, t:Throwable?)->Unit))->Any?)?=null
    internal var cancelClosure:((context:Any?)->Unit)?=null
}
