package com.dat3m.dartagnan.solver.caat4wmm.statistics;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.sets.Element;
import com.dat3m.dartagnan.solver.caat4wmm.EventDomain;
import com.dat3m.dartagnan.solver.caat4wmm.ExecutionGraph;
import com.dat3m.dartagnan.solver.caat4wmm.statistics.heuristics.EagerEncodingHeuristic;
import com.dat3m.dartagnan.utils.collections.UpdatableValue;
import com.dat3m.dartagnan.verification.model.EventData;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class IntermediateStatistics {

    public static boolean use = true;
    public static boolean showMemoized = true;
    public static boolean showNotMemoized = false;
    public static final int MAX_HOTNESS = 30;
    private final HotMap<List<Event>> edges;
    private final HotMap<List<Event>> edgesWOMemoized;
    private final HotMap<List<Event>> iterations;
    private final HotMap<List<Event>> iterationsWOMemoized;
    private HotMap<List<Event>> metric;
    private final HashMap<Tuple, ArrayList<ReasonElement>> computedRelations;
    private final HashMap<String, UpdatableValue<Float>> reuseCount;
    private EventDomain domain;
    private int iterationCounter;
    private final int relearn;
    private long deltaTime = 0;

    public IntermediateStatistics(int relearn) {
        edges = new HotMap<>();
        edgesWOMemoized = new HotMap<>();
        iterations = new HotMap<>();
        computedRelations = new HashMap<>();
        iterationsWOMemoized = new HotMap<>();
        metric = new HotMap<>();
        reuseCount = new HashMap<>();
        iterationCounter = 1;
        this.relearn = relearn;
    }

    public IntermediateStatistics() {
        edges = new HotMap<>();
        edgesWOMemoized = new HotMap<>();
        iterations = new HotMap<>();
        computedRelations = new HashMap<>();
        iterationsWOMemoized = new HotMap<>();
        metric = new HotMap<>();
        reuseCount = new HashMap<>();
        iterationCounter = 1;
        relearn = 0;
    }

    public TupleSetMap computeHotEdges(EagerEncodingHeuristic method) {
        return method.chooseHotEdges(edges, edgesWOMemoized, iterations, iterationsWOMemoized, metric, iterationCounter);
    }

    public void initializeFromExecutionGraph(ExecutionGraph exec) {
        domain = exec.getDomain();
    }

    public void update() {
        long startTime = System.currentTimeMillis();
        edges.update();
        //edgesWOMemoized.update();
        //iterations.update();
        //iterationsWOMemoized.update();
        //metric = edges.per(iterations, iterationCounter);
        if (relearn != 0 && iterationCounter % relearn == 0) {
            edges.clear();
            //edgesWOMemoized.clear();
            //iterations.clear();
            //iterationsWOMemoized.clear();
            iterationCounter = 0;
        }
        computedRelations.clear();
        for (var entry : reuseCount.entrySet()) {
            entry.getValue().update();
        }
        iterationCounter++;
        deltaTime += System.currentTimeMillis() - startTime;
    }

    public void insert(String name, Derivable der, Derivable cameFrom) {
        long startTime = System.currentTimeMillis();
        if (der instanceof Edge) {
            Event e1 = caatIdToEvent(((Edge) der).getFirst());
            Event e2 = caatIdToEvent(((Edge) der).getSecond());
            if (e1 == null || e2 == null) {
                return;
            }
            List<Event> edge = Arrays.asList(e1, e2);
            edges.insertAndCount(name, edge);
            //edgesWOMemoized.insertAndCount(name, edge);
            //iterations.insertAndCount(name, edge, true);
            //iterationsWOMemoized.insertAndCount(name, edge, true);

            saveForLater(name, edge, cameFrom);
        } else if (der instanceof Element) {
            Event e1 = caatIdToEvent(((Element) der).getId());
            if (e1 == null) {
                return;
            }
            List<Event> event = Collections.singletonList(e1);
            edges.insertAndCount(name, event);
            //edgesWOMemoized.insertAndCount(name, event);
            //iterations.insertAndCount(name, event, true);
            //iterationsWOMemoized.insertAndCount(name, event, true);

            saveForLater(name, event, cameFrom);
        }
        deltaTime += System.currentTimeMillis() - startTime;
    }

    public void addMemoizedReasons(Collection<Edge> caatEdges) {
        long startTime = System.currentTimeMillis();
        for (Edge caatEdge : caatEdges) {
            Tuple edge = new Tuple(caatIdToEvent(caatEdge.getFirst()), caatIdToEvent(caatEdge.getSecond()));
            if (computedRelations.containsKey(edge)) {
                for (ReasonElement el : computedRelations.get(edge)) {
                    edges.insertAndCount(el.getName(), el.getEvents());
                    //iterations.insertAndCount(el.getName(), el.getEvents(), true);
                }
            }
        }
        deltaTime += System.currentTimeMillis() - startTime;
    }

    public Event caatIdToEvent(int caatId) {
        if (domain != null) {
            EventData eventData = domain.getObjectById(caatId);
            return eventData.getEvent();
        } else {
            return null;
        }
    }

    public void reused(String name) {
        long startTime = System.currentTimeMillis();
        if (!reuseCount.containsKey(name)) {
            reuseCount.put(name, new UpdatableValue<Float>(0f));
        }
        reuseCount.get(name).setCurrent(reuseCount.get(name).current() + 1);
        deltaTime += System.currentTimeMillis() - startTime;
    }

    private void saveForLater(String name, List<Event> list, Derivable cameFrom) {
        long startTime = System.currentTimeMillis();
        if (cameFrom == null || !(cameFrom instanceof Edge)) {
            return;
        }

        Event e1 = caatIdToEvent(((Edge) cameFrom).getFirst());
        Event e2 = caatIdToEvent(((Edge) cameFrom).getSecond());
        Tuple edge = new Tuple(e1, e2);
        if (!computedRelations.containsKey(edge)) {
            computedRelations.put(edge, new ArrayList<>());
        }

        ArrayList<ReasonElement> reasons = computedRelations.get(edge);
        reasons.add(new ReasonElement(name, list));
        deltaTime += System.currentTimeMillis() - startTime;
    }

    public String toString() {
        Function<List<Event>, String> listString = list -> {
            String string = "";
            string += list.get(0).getCId();
            for (int i = 1; i < list.size(); i++) {
                string += "," + list.get(i).getCId();
            }
            return string;
        };

        BiConsumer<HashMap<List<Event>, UpdatableValue<Float>>, Map.Entry<List<Event>, UpdatableValue<Float>>> splitList =
                (map, entry) -> {
            for (Event event : entry.getKey()) {
                List<Event> singleton = Collections.singletonList(event);
                UpdatableValue<Float> currentValue = map.get(singleton);
                if (currentValue == null) {
                    currentValue = new UpdatableValue<>(0f);
                }
                float was1 = currentValue.was();
                float was2 = entry.getValue().was();
                float curr1 = currentValue.current();
                float curr2 = entry.getValue().current();
                map.put(singleton, new UpdatableValue<>(was1 + was2, curr1 + curr2));
            }
        };

        StringBuilder str = new StringBuilder();
        if (showNotMemoized) {
            str.append("\n\nHot intermediates:");
            str.append(edgesWOMemoized.toString(listString));
            str.append("\n\nHot events:");
            str.append(singletonListMapToString(edgesWOMemoized.summarizeT(splitList)));
            str.append("\n\nHot predicates:");
            str.append(stringMapToString(edgesWOMemoized.summarizeName()));
            str.append("\n\n\nHot intermediates by iteration:");
            str.append(iterationsWOMemoized.toString(listString));
            str.append("\n\nHot intermediates by #edges * #occ. iterations / #iterations:");
            str.append(edgesWOMemoized.per(iterationsWOMemoized, iterationCounter).toString(listString));
        }

        if (showMemoized) {
            str.append("\n\n\n ---------- Memoized edges taken into account ---------- \n");
            str.append("\n\nHot intermediates:").append(edges.toString(listString));
            str.append("\n\nHot events:");
            str.append(singletonListMapToString(edges.summarizeT(splitList)));
            str.append("\n\nHot predicates:");
            str.append(stringMapToString(edges.summarizeName()));
            str.append("\n\nHot intermediates by iterations:").append(iterations.toString(listString));
            str.append("\n\nHot intermediates by #edges * #occ. iterations / #iterations:");
            str.append(metric.toString(listString));
        }

        str.append("\n\n\n ---------- Reused Edges ---------- \n");
        str.append(stringMapToString(reuseCount));
        str.append("\n");
        return str.toString();
    }

    private String singletonListMapToString(HashMap<List<Event>, UpdatableValue<Float>> map) {
        StringBuilder str = new StringBuilder();
        ArrayList<Map.Entry<List<Event>, UpdatableValue<Float>>> sorted = new ArrayList<>(map.entrySet());
        sorted.sort((entry1, entry2) -> entry2.getValue().current().compareTo(entry1.getValue().current()));
        for (int i = 0; i < Math.min(MAX_HOTNESS, sorted.size()); i++) {
            Event e = sorted.get(i).getKey().get(0);
            str.append("\n").append(e.getCId()).append(" -> ").append(e).append(": ");
            float current = sorted.get(i).getValue().current();
            float was = sorted.get(i).getValue().was();
            str.append((int)current).append(" (+");
            str.append((int)(current - was)).append(")");
        }
        str.append("\n");
        return str.toString();
    }

    private String stringMapToString(HashMap<String, UpdatableValue<Float>> map) {
        StringBuilder str = new StringBuilder();
        ArrayList<Map.Entry<String, UpdatableValue<Float>>> sorted = new ArrayList<>(map.entrySet());
        sorted.sort((entry1, entry2) -> entry2.getValue().current().compareTo(entry1.getValue().current()));
        for (int i = 0; i < Math.min(MAX_HOTNESS, sorted.size()); i++) {
            float current = sorted.get(i).getValue().current();
            float difference = current - sorted.get(i).getValue().was();
            str.append("\n").append(sorted.get(i).getKey()).append(": ");
            str.append((int)current).append(" (+").append((int)difference).append(")");
        }
        str.append("\n");
        return str.toString();
    }

    private String stringIntMapToString(HashMap<String, Integer> map) {
        StringBuilder str = new StringBuilder();
        ArrayList<Map.Entry<String, Integer>> sorted = new ArrayList<>(map.entrySet());
        sorted.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
        for (int i = 0; i < Math.min(MAX_HOTNESS, sorted.size()); i++) {
            float current = sorted.get(i).getValue();
            str.append("\n").append(sorted.get(i).getKey()).append(": ");
            str.append((int)current);
        }
        str.append("\n");
        return str.toString();
    }

    public long getDeltaTime() { return deltaTime; }

}
