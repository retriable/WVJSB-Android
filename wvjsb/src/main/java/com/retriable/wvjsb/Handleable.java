package com.retriable.wvjsb;

public interface Handleable {
    public Handleable onEvent(Connection connection,Object parameter,Doneable doneable);
    public void onCancel(Cancelable cancelable);
}
