package com.retriable.wvjsb;

import java.util.HashMap;
import com.retriable.wvjsb.Functions.Function4Void;

public class Connection {
    public Object info;

    public Operation event(String type,Object parameter){
        synchronized (this){
            String mid=String.format("%d",nextSeq++);

        }
        return null;
    }

    Connection(Object info,Function4Void<Connection,String,String,Object> send){
        super();
        this.info=info;
        this.send=send;
    }

    void ack(String id,Object result,Error error){

    }
    private Function4Void<Connection,String,String,Object> send;
    private long nextSeq=0;
    private HashMap<String,Operation> operations=new HashMap<>();

}
