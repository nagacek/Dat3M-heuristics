package com.dat3m.dartagnan.wmm.utils;

import com.google.common.collect.Sets;

import java.util.HashMap;

public class TupleSetMap {
    private HashMap<String, TupleSet> map;

    public TupleSetMap(TupleSetMap other) {
        map = new HashMap<>(other.getMap());
    }

    public TupleSetMap(String name, TupleSet set) {
        map = new HashMap<>();
        map.put(name, set);
    }

    public TupleSetMap() {
        map = new HashMap<>();
    }

    public void merge(TupleSetMap other) {
        other.getMap().forEach((name, set) -> map.merge(name, set, (set1, set2) -> new TupleSet(Sets.union(set1, set2))));
    }

    public TupleSet getDifference(String name) {
        return map.get(name);
    }

    protected HashMap<String, TupleSet> getMap() {
        return map;
    }

}
