package com.retriable.wvjsb;

public class Functions {

    public interface Function1Void<T>{
        void invoke(T t);
    }
    public interface Function0<R> {
        R invoke();
    }
    public interface Function1<T,R> {
        R invoke(T t);
    }

    public interface Function2Void<T1,T2> {
        void invoke(T1 t1,T2 t2);
    }

    public interface Function2<T1,T2,R> {
        R invoke(T1 t1,T2 t2);
    }

    public interface Function3Void<T1,T2,T3>{
        void invoke(T1 t1,T2 t2,T3 t3);
    }

    public interface Function3<T1,T2,T3,R> {
        R invoke(T1 t1,T2 t2,T3 t3);
    }

    public interface Function4Void<T1,T2,T3,T4>{
        void invoke(T1 t1,T2 t2,T3 t3,T4 t4);
    }

}
