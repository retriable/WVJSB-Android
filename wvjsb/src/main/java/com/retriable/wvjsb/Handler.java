package com.retriable.wvjsb;

import com.retriable.wvjsb.Functions.Function3;
import com.retriable.wvjsb.Functions.Function2;
import com.retriable.wvjsb.Functions.Function1Void;

public final class Handler {
    private Function3<Connection,Object,Function2<Object,Error,Void>,Object> _onEvent;
    private Function1Void<Object> _onCancel;
    public final Handler onEvent(final Function3<Connection,Object,Function2<Object,Error,Void>,Object> onEvent){
        _onEvent = new Function3<Connection, Object, Function2<Object, Error, Void>, Object>() {
            @Override
            public Object invoke(Connection connection, Object o, Function2<Object, Error, Void> done) {
                return onEvent.invoke(connection,o,done);
            }
        };
        return this;
    }

    public final void onCancel(Function1Void<Object> onCancel){
        _onCancel= new Function1Void<Object>() {
            @Override
            public void invoke(Object o) {
                _onCancel.invoke(o);
            }
        };
    }
}
