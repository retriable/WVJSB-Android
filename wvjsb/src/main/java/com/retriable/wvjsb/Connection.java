package com.retriable.wvjsb;

import java.util.HashMap;

public class Connection {
    public String id;
    public Object info;


    public Operation event(String type,Object parameter){
        synchronized (this){
            String mid=String.format("%d",nextSeq++);

        }
        return null;
    }

    Connection(String id,Object info){
        super();
        this.id=id;
        this.info=info;
    }


    private long nextSeq=0;
    private HashMap<String,Operation> operations=new HashMap<>();

}
