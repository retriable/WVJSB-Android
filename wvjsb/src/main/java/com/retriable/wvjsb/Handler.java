package com.retriable.wvjsb;

import com.retriable.wvjsb.Functions.Function0;
import com.retriable.wvjsb.Functions.Function1Void;
import com.retriable.wvjsb.Functions.Function2Void;
import com.retriable.wvjsb.Functions.Function3;

import org.jetbrains.annotations.NotNull;

public final class Handler {
    /**
     *  handle event
     * @param  event event handler
     * @return current handler
     */
    public Handler onEvent(@NotNull Function3<Connection,Object,Function0<Function2Void<Object,Throwable>>,Object> event){
        this.event=event;
        return this;
    }

    /**
     * handle cancel
     * @param cancel cancel handler
     */
    public void onCancel(@NotNull Function1Void<Object> cancel){
        this.cancel=cancel;
    }

    Handler(){super();}

    Function3<Connection,Object, Function0<Function2Void<Object,Throwable>>,Object> event;
    Function1Void<Object> cancel;
}
