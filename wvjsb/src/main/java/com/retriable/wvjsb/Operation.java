package com.retriable.wvjsb;
import com.retriable.wvjsb.Functions.Function3Void;
public class Operation {

    public Operation onAck(Function3Void<Operation,Object,Error> onAck){
        return this;
    }
    public void timeout(Long timeout){

    }

    public void cancel(Object context){

    }


}
