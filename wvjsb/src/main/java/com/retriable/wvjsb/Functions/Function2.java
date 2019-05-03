package com.retriable.wvjsb.Functions;

public interface Function2<T1,T2,R> {
    R invoke(final T1 t1,final T2 t2);
}
