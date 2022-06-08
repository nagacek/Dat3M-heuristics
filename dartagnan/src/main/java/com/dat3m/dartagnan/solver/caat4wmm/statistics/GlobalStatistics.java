package com.dat3m.dartagnan.solver.caat4wmm.statistics;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat.predicates.Derivable;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.sets.Element;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.EdgeLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.ElementLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.EventDomain;
import com.dat3m.dartagnan.solver.caat4wmm.ExecutionGraph;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.RelLiteral;
import com.dat3m.dartagnan.utils.collections.UpdatableValue;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.model.EventData;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import scala.Int;

import java.util.*;
import java.util.function.Function;

public class GlobalStatistics {

    public static final boolean globalStats = true;
    public static final boolean showTree = false;
    public static final boolean showNotMemoized = true;
    public static final boolean showMemoized = true;
    public static final int MAX_HOTNESS = 30;
    private static ExecutionGraph executionGraph;
    private static EventDomain domain;
    private static ExecutionModel executionModel;
    private static HotMap<Tuple> hotCoreEdges;
    //private static HotMap<Tuple> hotBaseEdges;
    //private static HotMap<Event> hotBaseElements;
    //private static HotMap<Tuple> hotIntermediateEdges;
    //private static HotMap<Event> hotIntermediateElements;
    private static HashSet<HotTree> intermediateTrees;
    private static HotTree currentTree;
    //private static HashMap<String, UpdatableValue<Integer>> predicateSummary;
    private static Set<Conjunction<CAATLiteral>> currentBaseReasons;

    private GlobalStatistics() {}

    public static void initialize(ExecutionGraph exGraph) {
        executionGraph = exGraph;
        domain = executionGraph.getDomain();
        executionModel = domain.getExecution();
        hotCoreEdges = new HotMap<>();
        //hotBaseEdges = new HotMap<>();
        //hotBaseElements = new HotMap<>();
        //hotIntermediateEdges = new HotMap<>();
        //hotIntermediateElements = new HotMap<>();
        intermediateTrees = new HashSet<>();
        currentTree = null;
        //predicateSummary = new HashMap<>();
        currentBaseReasons = new HashSet<>();
    }

    public static void newIteration() {
        hotCoreEdges.update();
        //hotBaseEdges.update();
        //hotBaseElements.update();
        //hotIntermediateEdges.update();
        //hotIntermediateElements.update();
        for (HotTree tree : intermediateTrees) {
            tree.update();
        }
        currentTree = null;
        //for (UpdatableValue<Integer> value : predicateSummary.values()) {
        //    value.update();
        //}
        currentBaseReasons = new HashSet<>();
    }

    public static void newPredicate() {
        currentTree = null;
    }

    public static void insertCoreEdges(List<Conjunction<CoreLiteral>> edges) {
        for (Conjunction<CoreLiteral> edge : edges) {
            for (CoreLiteral lit : edge.getLiterals()) {
                if (lit instanceof RelLiteral) {
                    String litName = lit.getName();
                    Tuple insertTuple = new Tuple(((RelLiteral) lit).getData().getFirst(), ((RelLiteral) lit).getData().getSecond());
                    hotCoreEdges.insertAndCount(litName, insertTuple);
                }
            }
        }
    }

    public static void insertBaseEdges(DNF<CAATLiteral> baseEdges) {
        Set<Conjunction<CAATLiteral>> reasons = baseEdges.getCubes();
        for (Conjunction<CAATLiteral> reason : reasons) {
            HashSet<CAATLiteral> globalIdSet = new HashSet<>();
            for (CAATLiteral lit : reason.getLiterals()) {
                if (lit instanceof ElementLiteral) {
                    Event event = caatIdToEvent(((ElementLiteral) lit).getElement().getId());
                    //hotBaseElements.insertAndCount(lit.getName(), event);
                    globalIdSet.add(new ElementLiteral(lit.getName(), new Element(event.getCId()), lit.isNegative()));
                } else {
                    Edge edge = ((EdgeLiteral)lit).getEdge();
                    Event e1 = caatIdToEvent(edge.getFirst());
                    Event e2 = caatIdToEvent(edge.getSecond());
                    //Tuple tuple = new Tuple(e1, e2);
                    //hotBaseEdges.insertAndCount(lit.getName(), tuple);
                    globalIdSet.add(new EdgeLiteral(lit.getName(), new Edge(e1.getCId(), e2.getCId()), lit.isNegative()));
                }
            }
            currentBaseReasons.add(new Conjunction<>(globalIdSet));
        }
    }

    /*public static void insertIntermediate(String name, Derivable derivable) {
        countSummary(name);
        if (derivable instanceof Edge) {
            Event e1 = caatIdToEvent(((Edge)derivable).getFirst());
            Event e2 = caatIdToEvent(((Edge) derivable).getSecond());
            hotIntermediateEdges.insertAndCount(name, new Tuple(e1, e2));
        } else if (derivable instanceof Element) {
            Event e = caatIdToEvent(((Element) derivable).getId());
            hotIntermediateElements.insertAndCount(name, e);
        }
    }*/

    public static void go(String name) {
        if (currentTree == null) {
            HotTree newCurrent = null;
            for (HotTree tree : intermediateTrees) {
                if (tree.getName().equals(name)) {
                    newCurrent = tree;
                    break;
                }
            }
            if (newCurrent != null) {
                currentTree = newCurrent;
            } else {
                currentTree = new HotTree(name);
                intermediateTrees.add(currentTree);
            }
        } else {
            if (currentTree.getLeft() == null) {
                currentTree.setLeft(name);
                currentTree = currentTree.getLeft();
            } else if (currentTree.getLeft().getName().equals(name)) {
                currentTree = currentTree.getLeft();
            } else if (currentTree.getRight() == null) {
                currentTree.setRight(name);
                currentTree = currentTree.getRight();
            } else if (currentTree.getRight().getName().equals(name)) {
                currentTree = currentTree.getRight();
            } else {
                throw new IllegalStateException("Faulty tree");
            }
        }
        currentTree.increment();
    }

    public static void goUp() {
        if (currentTree != null) {
            currentTree = currentTree.getParent();
        } else {
            throw new IllegalStateException("Faulty tree");
        }
    }

    public static void operator(String operator) {
        if (currentTree.getOperator().equals("")) {
            currentTree.setOperator(operator);
        }
    }

    public static Set<HotTree> getTrees() {
        return intermediateTrees;
    }

    /*private static void countSummary(String name) {
        if (!predicateSummary.containsKey(name)) {
            predicateSummary.put(name, new UpdatableValue<>(0));
        } else {
            UpdatableValue<Integer> current = predicateSummary.get(name);
            current.setCurrent(current.current() + 1);
        }
    }*/

    /*private static List<Map.Entry<String, UpdatableValue<Integer>>> sortSummary() {
        ArrayList<Map.Entry<String, UpdatableValue<Integer>>> sorted = new ArrayList<>();
        for (var entry : predicateSummary.entrySet()) {
            sorted.add(entry);
        }
        sorted.sort((obj1, obj2) -> obj2.getValue().current().compareTo(obj1.getValue().current()));
        return sorted;
    }*/

    private static Event caatIdToEvent(int caatId) {
        EventData eventData = domain.getObjectById(caatId);
        return eventData.getEvent();
    }

    private static String printBaseReasons() {
        StringBuilder str = new StringBuilder();
        for (Conjunction<CAATLiteral> reason : currentBaseReasons) {
            str.append("\n").append(reason);
        }
        return str.toString();
    }

    public static String print() {
        Function<Tuple, String> tupleToString = t -> t.getFirst().getCId() + "," + t.getSecond().getCId();
        Function<Event, String> eventToString = e -> Integer.toString(e.getCId());
        StringBuilder str = new StringBuilder();
        str.append("\n\nBase reasons:");
        str.append(printBaseReasons());
        str.append("\n\nHot core edges:");
        str.append(hotCoreEdges.toString(tupleToString));
        if (showTree || showNotMemoized) {
            str.append("\n\n\n ---------- Newly computed ---------- \n");
        }
        if (showTree) {
            str.append("\n\nHot derivation tree:");
            for (HotTree tree : intermediateTrees) {
                str.append("\n").append(tree);
            }
        }
        //str.append("\n\nHot intermediate edges:");
        //str.append(hotIntermediateEdges.toString(tupleToString));
        //str.append("\n\nHot intermediate unary predicates:");
        //str.append(hotIntermediateElements.toString(eventToString));
        //str.append("\n\nSummary hot predicates:");
        //for (var entry : sortSummary()) {
        //    UpdatableValue<Integer> value = entry.getValue();
        //    str.append("\n").append(entry.getKey()).append(": ").append(value.current())
        //            .append(" (+").append(value.current() - value.was()).append(")");
        //}
        //str.append("\n\nHot base edges:");
        //str.append(hotBaseEdges.toString(tupleToString));
        //str.append("\n\nHot unary base predicates:");
        //str.append(hotBaseElements.toString(eventToString));
        //str.append("\n");
        return str.toString();
    }
}
