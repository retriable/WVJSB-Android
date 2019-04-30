package com.retriable.wvjsb

class Connection {

	fun info(): Any?{
		return info
	}

	fun event(type:String,parameter: Any?):Operation?{
		val id = String.format("%d",nextSeq++)
		val operation=Operation({o->
			operations[id]=o
		},{_->
			operations.remove(id)
		},{_->
			sendClosure?.invoke(this,id,"cancel",null)
		})
		sendClosure?.invoke(this,id,type,parameter)
		return operation
	}

	private var info: Any? = null

	private constructor()

	internal constructor(info: Any?,closure:((connection:Connection,id:String,type:String,parameter:Any?)->Unit)){
		this.info=info
		this.sendClosure=closure
	}

	internal fun ack(id:String,result:Any?,t:Throwable?){
		operations[id]?.ack(result,t)
	}

	internal var sendClosure:((connection:Connection, id:String, type:String, parameter:Any?)->Unit)? = null

	private val operations:HashMap<String,Operation> = HashMap()
	private var nextSeq:Long=0

}