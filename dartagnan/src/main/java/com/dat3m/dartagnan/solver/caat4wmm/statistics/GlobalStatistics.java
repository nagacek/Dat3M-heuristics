package com.dat3m.dartagnan.solver.caat4wmm.statistics;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.predicates.sets.Element;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.EdgeLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.ElementLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.EventDomain;
import com.dat3m.dartagnan.solver.caat4wmm.ExecutionGraph;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.RelLiteral;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.model.EventData;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.utils.Tuple;

import java.util.*;
import java.util.function.Function;

public class GlobalStatistics {

    private static ExecutionGraph executionGraph;
    private static EventDomain domain;
    private static ExecutionModel executionModel;
    private static HotMap<Tuple> hotCoreEdges;
    private static HotMap<Tuple> hotBaseEdges;
    private static HotMap<Event> hotBaseElements;
    private static Set<Conjunction<CAATLiteral>> currentBaseReasons;
    private static String baseReasons;

    private GlobalStatistics() {}

    public static void initialize(ExecutionGraph exGraph) {
        executionGraph = exGraph;
        domain = executionGraph.getDomain();
        executionModel = domain.getExecution();
        hotCoreEdges = new HotMap<>();
        hotBaseEdges = new HotMap<>();
        hotBaseElements = new HotMap<>();
        currentBaseReasons = new HashSet<>();
        baseReasons = "";
    }

    public static void newIteration() {
        hotCoreEdges.update();
        hotBaseEdges.update();
        hotBaseElements.update();
        currentBaseReasons = new HashSet<>();
        baseReasons = "";
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
                    hotBaseElements.insertAndCount(lit.getName(), event);
                    globalIdSet.add(new ElementLiteral(lit.getName(), new Element(event.getCId()), lit.isNegative()));
                } else {
                    Edge edge = ((EdgeLiteral)lit).getEdge();
                    Event e1 = caatIdToEvent(edge.getFirst());
                    Event e2 = caatIdToEvent(edge.getSecond());
                    Tuple tuple = new Tuple(e1, e2);
                    hotBaseEdges.insertAndCount(lit.getName(), tuple);
                    globalIdSet.add(new EdgeLiteral(lit.getName(), new Edge(e1.getCId(), e2.getCId()), lit.isNegative()));
                }
            }
            currentBaseReasons.add(new Conjunction<>(globalIdSet));
        }
    }

    private static Event caatIdToEvent(int caatId) {
        EventData eventData = domain.getObjectById(caatId);
        return eventData.getEvent();
    }

    public static String printBaseReasons() {
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
        str.append("\n\nHot base edges:");
        str.append(hotBaseEdges.toString(tupleToString));
        str.append("\n\nHot unary base predicates:");
        str.append(hotBaseElements.toString(eventToString));
        str.append("\n");
        return str.toString();
    }
}
