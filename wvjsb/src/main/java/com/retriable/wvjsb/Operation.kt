package com.retriable.wvjsb

import android.os.Handler

class Operation {

    var retainClosure:((Operation)->Unit)? = null
    var releaseClosure:((Operation)->Unit)? = null
    var cancelClosure:((Operation)->Unit)? = null

    private constructor()

    internal constructor(retainClosure:(Operation)->Unit,releaseClosure:(Operation)->Unit,cancelClosure:(Operation)->Unit){
        this.retainClosure=retainClosure
        this.releaseClosure=releaseClosure
        this.cancelClosure=cancelClosure
    }

    fun onAck(closure: ((operation: Operation,result:Any?, t:Throwable?)->Unit )): Operation{
        this.ackClosure=closure
        retainClosure?.invoke(this)
        return this
    }

    fun timeout(timeout: Long){
        if (timeout<=0){
            return
        }
        if (ok){
            return
        }
        if (runnableClosure!=null) {
            return
        }
        runnableClosure = {
            ack(null,Throwable("timed out"))
            handler.removeCallbacks(runnableClosure)
            runnableClosure=null
        }

        handler.postDelayed(runnableClosure,timeout)
    }

    fun cancel(){
        cancelClosure?.invoke(this)
        ack(null, Throwable("cancelled"))
    }

    companion object{
        val handler:Handler = Handler()
    }

    internal fun ack(result: Any?,t: Throwable?){
        if (ok) {
            return
        }
        ok=true
        handler.removeCallbacks(runnableClosure)
        runnableClosure=null
        ackClosure?.invoke(this,result,t)
        releaseClosure?.invoke(this)
    }

    private var ok:Boolean = false
    private var runnableClosure:(()->Unit)? = null
    private var ackClosure:((Operation, Any?, Throwable?)->Unit)? = null

}