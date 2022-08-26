package com.dat3m.dartagnan.solver.caat4wmm.statistics;

import com.dat3m.dartagnan.utils.collections.UpdatableValue;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/*
    Double map counting occurrences of arguments corresponding to a given name.
    It can track an old value for every entry that is updated on demand.
 */
public class HotMap <T>{
    private HashMap<String, HashMap<T, UpdatableValue<Float>>> map;

    private static final int MAX_HOTNESS = IntermediateStatistics.MAX_HOTNESS;

    public HotMap() {
        map = new HashMap<>();
    }

    public void insertAndCount(String name, T argument) {
        insertAndCount(name, argument, false);
    }

    public void insertAndCount(String name, T argument, boolean registerOnly) {
        initMap(name);
        HashMap<T, UpdatableValue<Float>> relationMap = map.get(name);
        if (!relationMap.containsKey(argument)) {
            UpdatableValue<Float> counter = new UpdatableValue<>(0f);
            counter.setCurrent(1f);
            relationMap.put(argument, counter);
        } else {
            UpdatableValue<Float> current = relationMap.get(argument);
            if (registerOnly && current.current() > current.was()) {
                return;
            }
            current.setCurrent(current.current() + 1);
        }
    }

    public void insertAndSet(String name, T argument, float oldValue, float newValue) {
        initMap(name);
        HashMap<T, UpdatableValue<Float>> relationMap = map.get(name);
        if (!relationMap.containsKey(argument)) {
            UpdatableValue<Float> counter = new UpdatableValue<>(oldValue);
            counter.setCurrent(newValue);
            relationMap.put(argument, counter);
        } else {
            UpdatableValue<Float> current = relationMap.get(argument);
            current.setCurrent(newValue);
        }
    }

    public void update() {
        for (var relations : map.values()) {
            for (UpdatableValue<Float> counter : relations.values()) {
                counter.update();
            }
        }
    }

    public float getAverage(Function<String, Boolean> considerRelation) {
        int i = 0;
        float value = 0f;
        for (var relation : map.entrySet()) {
            if (considerRelation.apply(relation.getKey())) {
                for (var tuple : relation.getValue().entrySet()) {
                    i++;
                    value += tuple.getValue().current();
                }
            }
        }
        return value / i;
    }

    public HashMap<String, Set<T>> getAll(Function<String, Boolean> considerRelation, Predicate<Map.Entry<T, UpdatableValue<Float>>> threshold) {
        HashMap<String, Set<T>> candidates = new HashMap<>();
        for (var relation : map.entrySet()) {
            if (!considerRelation.apply(relation.getKey())) {
                continue;
            }
            Set<T> tuples = relation.getValue().entrySet().stream()
                    .filter(threshold)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            candidates.put(relation.getKey(), tuples);
        }
        return candidates;
    }

    protected UpdatableValue<Float> get(String name, T argument) {
        return map.get(name).get(argument);
    }

    public HotMap<T> per(HotMap<T> other, int normalizer) {
        HotMap<T> returnValue = new HotMap<>();
        for (var name : map.keySet()) {
            for (var value : map.get(name).entrySet()) {
                UpdatableValue<Float> otherValue = other.get(name, value.getKey());
                float oldValue;
                if (normalizer > 1) {
                    oldValue = value.getValue().was() * otherValue.was() / (normalizer - 1);
                } else {
                    oldValue = 0f;
                }
                float newValue = value.getValue().current() * otherValue.current() / normalizer;
                returnValue.insertAndSet(name, value.getKey(), oldValue, newValue);
            }
        }
        return returnValue;
    }

    public HashMap<T, UpdatableValue<Float>> summarizeT(BiConsumer<HashMap<T, UpdatableValue<Float>>, Map.Entry<T, UpdatableValue<Float>>> func) {
        HashMap<T, UpdatableValue<Float>> newMap = new HashMap<>();
        for (var entry : map.entrySet()) {
            for (var value : entry.getValue().entrySet()) {
                func.accept(newMap, value);
            }
        }
        return newMap;
    }

    public HashMap<String, UpdatableValue<Float>> summarizeName() {
        HashMap<String, UpdatableValue<Float>> newMap = new HashMap<>();
        for (var key : map.keySet()) {
            if (!newMap.containsKey(key)) {
                newMap.put(key, new UpdatableValue<>(0f));
            }
            for (var value : map.get(key).values()) {
                UpdatableValue<Float> oldValue = newMap.get(key);
                float was = oldValue.was() + value.was();
                float current = oldValue.current() + value.current();
                UpdatableValue<Float> newValue = new UpdatableValue<>(was, current);
                newMap.put(key, newValue);
            }
        }
        return newMap;
    }

    private void initMap(String initKey) {
        if (!map.containsKey(initKey)) {
            map.put(initKey, new HashMap<>());
        }
    }

    public ArrayList<Map.Entry<String, Map.Entry<T, UpdatableValue<Float>>>> sort(Comparator<Map.Entry<String, Map.Entry<T, UpdatableValue<Float>>>> comp) {
        ArrayList<Map.Entry<String, Map.Entry<T, UpdatableValue<Float>>>> sorted = new ArrayList<>();
        for (var relation : map.keySet()) {
            for (var edge : map.get(relation).entrySet()){
                sorted.add(Map.entry(relation, edge));
            }
        }
        sorted.sort(comp);
        return sorted;
    }

    public ArrayList<Map.Entry<String, Map.Entry<T, UpdatableValue<Float>>>> sort() {
        return sort((obj1, obj2) -> obj2.getValue().getValue().current().compareTo(obj1.getValue().getValue().current()));
    }

    public String toString(Function<T, String> func) {
        ArrayList<Map.Entry<String, Map.Entry<T, UpdatableValue<Float>>>> sorted = sort();
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < Math.min(MAX_HOTNESS, sorted.size() - 1); i++) {
            String name = sorted.get(i).getKey();
            T argument = sorted.get(i).getValue().getKey();
            UpdatableValue<Float> occurrences = sorted.get(i).getValue().getValue();
            str.append("\n").append(name).append("(").append(func.apply(argument)).append(")").append(": ");

            float current = occurrences.current();
            if (occurrences.current() == (int)current) {
                str.append((int)current);
            } else {
                str.append(String.format("%.2f", current));
            }

            float difference = occurrences.current() - occurrences.was();
            if (difference >= 0) {
                str.append(" (+");
            } else {
                str.append(" (");
            }
            if (difference == (int) difference) {
                str.append((int)difference);
            } else {
                str.append(String.format("%.2f", difference));
            }

            str.append(")");
        }
        return str.toString();
    }

}

