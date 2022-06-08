package com.dat3m.dartagnan.utils.collections;

public class UpdatableValue<T> {
    private T current;
    private T was;

    public UpdatableValue(T init) {
        current = init;
        was = init;
    }

    public UpdatableValue(T init, T current) {
        this.current = current;
        was = init;
    }

    public T current() {
        return current;
    }

    public void setCurrent(T newValue) {
        current = newValue;
    }

    public T was() {
        return was;
    }

    public void update() {
        was = current;
    }
}
