package com.retriable.wvjsb;

import android.os.Handler;
import android.util.Log;

import com.retriable.wvjsb.Functions.Function1Void;
import com.retriable.wvjsb.Functions.Function2Void;
import com.retriable.wvjsb.Functions.Function3Void;

import org.jetbrains.annotations.Nullable;

public final class Operation {
    /**
     * ack from client
     * @param ack ack handler
     * @return current operation
     */
    public Operation onAck(final Function3Void<Operation,Object,Throwable> ack){
        synchronized (this){
            if (ok){
                return this;
            }
            final Operation o = this;
            callRetain.invoke(o);
            this.ack=new Function2Void<Object, Throwable>() {
                @Override
                public void invoke(Object parameter, Throwable throwable) {
                    callRelease.invoke(o);
                    ack.invoke(o,parameter,throwable);
                }
            };
            return this;
        }
    }

    /**
     * set timeout for operation
     * @param timeout timeout
     * @return current operation
     */
    public Operation timeout(long timeout){
        if (timeout<=0){
            return this;
        }
        synchronized (this){
            if (ok || null!=runnable){
                return this;
            }
            final Operation o=this;
            runnable=new Runnable() {
                @Override
                public void run() {
                    synchronized (o){
                        Log.d("wvjsb", "thread id: "+ Thread.currentThread().getId());
                        if (ok){
                            return;
                        }
                        ok=true;
                        if (null!=runnable){
                            handler.removeCallbacks(runnable);
                            runnable=null;
                        }
                        if (null==ack){
                            return;
                        }
                    }
                    ack.invoke(null,new Throwable("timed out"));
                }
            };
        }
        handler.postDelayed(runnable,timeout);
        return this;
    }

    public void cancel(){
        synchronized (this){
            if (ok){
                return;
            }
            ok=true;
            if (null!=runnable){
                handler.removeCallbacks(runnable);
                runnable=null;
            }
            callCancel.invoke(this);
            if (null==ack){
                return;
            }
        }
        ack.invoke(null,new Throwable("cancelled"));
    }

    Function1Void<Operation> callRetain;
    Function1Void<Operation> callRelease;
    Function1Void<Operation> callCancel;

    Operation (){
        super();
    }

    void doAck(@Nullable Object parameter,@Nullable Throwable throwable){
        synchronized (this){
            if (ok){
                return;
            }
            ok=true;
            if (null!=runnable){
                handler.removeCallbacks(runnable);
                runnable=null;
            }
            if (null==ack){
                return;
            }
        }
        ack.invoke(parameter,throwable);
    }

    private boolean ok = false;
    private Runnable runnable;
    private Function2Void<Object,Throwable> ack;
    private static final Handler handler = new Handler();

}
