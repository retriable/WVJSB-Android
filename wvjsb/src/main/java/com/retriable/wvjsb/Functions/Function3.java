package com.retriable.wvjsb.Functions;

public interface Function3<T1,T2,T3,R> {
    R invoke(final T1 t1,final T2 t2,final T3 t3);
}
