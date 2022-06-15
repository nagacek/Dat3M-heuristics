package com.dat3m.dartagnan.wmm.utils;

import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    public Set<Map.Entry<String, TupleSet>> getEntries() { return map.entrySet(); }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (var entry : map.entrySet()) {
            str.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        return str.toString();
    }

    protected HashMap<String, TupleSet> getMap() {
        return map;
    }

}
