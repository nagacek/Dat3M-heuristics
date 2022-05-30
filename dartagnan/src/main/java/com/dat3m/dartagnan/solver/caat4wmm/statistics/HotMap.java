package com.dat3m.dartagnan.solver.caat4wmm.statistics;

import com.dat3m.dartagnan.utils.collections.UpdatableValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/*
    Double map counting occurrences of arguments corresponding to a given name.
    It can track an old value for every entry that is updated on demand.
 */
public class HotMap <T>{
    private HashMap<String, HashMap<T, UpdatableValue<Integer>>> map;

    public HotMap() {
        map = new HashMap<>();
    }

    public void insertAndCount(String name, T argument) {
        initMap(name);
        HashMap<T, UpdatableValue<Integer>> relationMap = map.get(name);
        if (!relationMap.containsKey(argument)) {
            UpdatableValue<Integer> counter = new UpdatableValue<>(0);
            counter.setCurrent(1);
            relationMap.put(argument, counter);
        } else {
            UpdatableValue<Integer> current = relationMap.get(argument);
            current.setCurrent(current.current() + 1);
        }
    }

    public void update() {
        for (var relations : map.values()) {
            for (UpdatableValue<Integer> counter : relations.values()) {
                counter.update();
            }
        }
    }

    private void initMap(String initKey) {
        if (!map.containsKey(initKey)) {
            map.put(initKey, new HashMap<>());
        }
    }

    public String toString(Function<T,String> func) {
        ArrayList<Map.Entry<String, Map.Entry<T, UpdatableValue<Integer>>>> sorted = new ArrayList<>();
        StringBuilder str = new StringBuilder();
        for (var relation : map.keySet()) {
            for (var edge : map.get(relation).entrySet()){
                sorted.add(Map.entry(relation, edge));
            }
        }
        sorted.sort((obj1, obj2) -> obj2.getValue().getValue().current().compareTo(obj1.getValue().getValue().current()));
        for (var entry : sorted) {
            String name = entry.getKey();
            T argument = entry.getValue().getKey();
            UpdatableValue<Integer> occurrences = entry.getValue().getValue();
            str.append("\n").append(name).append("(").append(func.apply(argument)).append(")").append(": ")
                    .append(occurrences.current()).append(" (+").append(occurrences.current() - occurrences.was()).append(")");
        }
        return str.toString();
    }

}

