package com.dat3m.dartagnan.wmm.utils;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.witness.Edge;
import com.google.common.collect.Sets;

import java.util.*;

public class TupleSetMap {
    private HashMap<String, TupleSet> map;

    public TupleSetMap(TupleSetMap other) {
        map = new HashMap<>(other.getMap());
    }

    public TupleSetMap(String name, TupleSet set) {
        map = new HashMap<>();
        map.put(name, set);
    }

    public TupleSetMap(HashMap<String, Set<List<Event>>> other) {
        map = new HashMap<>();
        for (var entry : other.entrySet()) {
            TupleSet newSet = new TupleSet();
            entry.getValue().forEach(e -> {
                if (e.size() == 2)
                    newSet.add(new Tuple(e.get(0), e.get(1)));
            });
            if (newSet.size() > 0)
                map.put(entry.getKey(), newSet);
        }
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

    public boolean contains(String name, Tuple tuple) {
        TupleSet contained = map.get(name);
        return contained != null && contained.contains(tuple);
    }

    public boolean contains(String name) {
        return map.get(name) != null;
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
