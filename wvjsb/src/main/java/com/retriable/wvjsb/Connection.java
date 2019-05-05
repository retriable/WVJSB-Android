package com.retriable.wvjsb;

import com.retriable.wvjsb.Functions.Function1Void;
import com.retriable.wvjsb.Functions.Function3Void;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class Connection {

    public final @Nullable Object info;

    public Operation event(String type,@Nullable Object parameter){

        final String id = String.valueOf(nextSeq.incrementAndGet());
        final Operation opt=new Operation();
        opt.callRetain=new Function1Void<Operation>() {
            @Override
            public void invoke(Operation o) {
                synchronized (operations){
                    operations.put(id,o);
                }
            }
        };
        opt.callRelease=new Function1Void<Operation>() {
            @Override
            public void invoke(Operation o) {
                synchronized (operations){
                    operations.remove(id);
                }
            }
        };
        opt.callCancel=new Function1Void<Operation>() {
            @Override
            public void invoke(Operation o) {
                send.invoke(id,"cancel",null);
            }
        };
        send.invoke(id,type,parameter);
        return opt;
    }

    Function3Void<String,String,Object> send;

    Connection(@Nullable Object info){
        super();
        this.info=info;
    }

    void ack(String id,@Nullable Object parameter,@Nullable Throwable throwable){

        Operation operation;
        synchronized (operations){
            operation = operations.get(id);
        }
        if (null==operation){
            return;
        }
        operation.doAck(parameter,throwable);
    }

    private AtomicLong nextSeq=new AtomicLong(0);
    private final Map<String,Operation> operations=new HashMap<>();
}
