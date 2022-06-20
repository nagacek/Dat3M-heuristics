package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat4wmm.Refiner;
import com.dat3m.dartagnan.solver.caat4wmm.ViolatingCycle;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.wmm.axiom.Acyclic;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class ReasonGraph {

    private Acyclic axiom;
    private Refiner refiner;

    private Map<Event, List<Tuple>> outEdges = new HashMap<>();
    private Map<Event, List<Tuple>> inEdges = new HashMap<>();
    private Map<Tuple, DNF<CoreLiteral>> reasonMap = new HashMap<>();

    private Set<List<Tuple>> encounteredCycles = new HashSet<>();
    private List<List<Tuple>> newCyclesToBeEncoded = new ArrayList<>();
    private Map<Tuple, Conjunction<CoreLiteral>> derivationToBeEncoded = new HashMap<>();

    public ReasonGraph(Acyclic axiomToTrack, Refiner refiner) {
        this.axiom = axiomToTrack;
        this.refiner = refiner;
    }

    public Acyclic getAxiom() { return axiom;}

    // --------------- Test code ------------------
    public boolean addCycle(ViolatingCycle vio) {
        List<Tuple> cycle = vio.getCycle();
        boolean changes = false;
        if (encounteredCycles.add(cycle)) {
            newCyclesToBeEncoded.add(cycle);
            //changes = true;
        }

        for (Tuple edge : cycle) {
            Conjunction<CoreLiteral> reason = vio.getEdgeReasons().get(edge);
            if (addReason(edge, reason)) {
                derivationToBeEncoded.put(edge, reason);
                changes = true;
            }
        }
        return changes;
    }

    public BooleanFormula encodeChanges(SolverContext ctx) {
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        Relation rel = axiom.getRelation();
        BooleanFormula enc = bmgr.makeTrue();

        for (Tuple edge : derivationToBeEncoded.keySet()) {
            Conjunction<CoreLiteral> reason = derivationToBeEncoded.get(edge);
            enc = bmgr.and(enc, bmgr.implication(clause2Formulas(reason, ctx), rel.getSMTVar(edge, ctx)));
        }

        for (List<Tuple> cycle : newCyclesToBeEncoded) {
            BooleanFormula noCycleFormula = cycle.stream().map(edge -> bmgr.not(rel.getSMTVar(edge, ctx)))
                    .reduce(bmgr.makeFalse(), bmgr::or);
            enc = bmgr.and(enc, noCycleFormula);
        }
        derivationToBeEncoded.clear();
        newCyclesToBeEncoded.clear();
        return enc;
    }

    // ------------------------------------------

    public List<Map.Entry<Tuple, DNF<CoreLiteral>>> getHotnessList() {
        List<Map.Entry<Tuple, DNF<CoreLiteral>>> edgeReasons = new ArrayList<>(reasonMap.entrySet());
        edgeReasons.sort(Comparator.comparingInt(x -> -x.getValue().getNumberOfCubes()));
        return edgeReasons;
    }


    public boolean addReason(Tuple edge, Conjunction<CoreLiteral> reason) {
        DNF<CoreLiteral> existingReasons = reasonMap.get(edge);
        if (existingReasons == null) {
            reasonMap.put(edge, new DNF<>(reason));
            outEdges.computeIfAbsent(edge.getFirst(), key -> new ArrayList<>()).add(edge);
            inEdges.computeIfAbsent(edge.getSecond(), key -> new ArrayList<>()).add(edge);
            return true;
        }

        DNF<CoreLiteral> newReasons = existingReasons.or(new DNF<CoreLiteral>(reason));
        if (newReasons.equals(existingReasons)) {
            return false;
        } else {
            reasonMap.put(edge, newReasons);
            return true;
        }
    }


    public BooleanFormula encodeShortestCycles(SolverContext ctx) {
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula enc = bmgr.makeFalse();

        Set<Event> nodes = new HashSet<>(inEdges.keySet());

        while (!nodes.isEmpty()) {
            Event node = nodes.stream().findFirst().get();
            List<Tuple> shortestPath = findShortestPath(node, node, edge -> true);

            if (shortestPath.isEmpty()) {
                continue;
            }
            BooleanFormula cycle = bmgr.makeTrue();
            for (Tuple t : shortestPath) {
                nodes.remove(t.getFirst());
                nodes.remove(t.getSecond());

                cycle = bmgr.and(cycle, DNF2Formula(reasonMap.get(t), ctx));
            }
            enc = bmgr.or(enc, cycle);
        }

        return bmgr.not(enc);
    }

    private BooleanFormula DNF2Formula(DNF<CoreLiteral> dnf, SolverContext ctx) {
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula enc = bmgr.makeFalse();
        for (Conjunction<CoreLiteral> clause : dnf.getCubes()) {
            enc = bmgr.or(enc, clause2Formulas(clause, ctx));
        }
        return enc;
    }

    private BooleanFormula clause2Formulas(Conjunction<CoreLiteral> clause, SolverContext ctx) {
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula formula = bmgr.makeTrue();
        for (CoreLiteral lit : clause.getLiterals()) {
            formula = bmgr.and(formula, refiner.permuteAndConvert(lit, Function.identity(), ctx));
        }
        return formula;
    }

    private final Queue<Event> queue1 = new ArrayDeque<>();
    private final Queue<Event> queue2 = new ArrayDeque<>();
    private final Map<Event, Tuple> parentMap1 = new HashMap<>();
    private final Map<Event, Tuple> parentMap2 = new HashMap<>();

    private List<Tuple> findShortestPath(Event start, Event end,
                                         Predicate<Tuple> filter) {
        queue1.clear();
        queue2.clear();
        parentMap1.clear();
        parentMap2.clear();

        queue1.add(start);
        queue2.add(end);
        boolean found = false;
        boolean doForwardBFS = true;
        Event cur = null;

        while (!found && (!queue1.isEmpty() || !queue2.isEmpty())) {
            if (doForwardBFS) {
                // Forward BFS
                int curSize = queue1.size();
                while (curSize-- > 0 && !found) {
                    for (Tuple next : outEdges.get(queue1.poll())) {
                        if (!filter.test(next)) {
                            continue;
                        }

                        cur = next.getSecond();

                        if (cur == end || parentMap2.containsKey(cur)) {
                            parentMap1.put(cur, next);
                            found = true;
                            break;
                        } else if (!parentMap1.containsKey(cur)) {
                            parentMap1.put(cur, next);
                            queue1.add(cur);
                        }
                    }
                }
                doForwardBFS = false;
            } else {
                // Backward BFS
                int curSize = queue2.size();
                while (curSize-- > 0 && !found) {
                    for (Tuple next : inEdges.get(queue2.poll())) {
                        if (!filter.test(next)) {
                            continue;
                        }
                        cur = next.getFirst();

                        if (parentMap1.containsKey(cur)) {
                            parentMap2.put(cur, next);
                            found = true;
                            break;
                        } else if (!parentMap2.containsKey(cur)) {
                            parentMap2.put(cur, next);
                            queue2.add(cur);
                        }
                    }
                }
                doForwardBFS = true;
            }
        }

        if (!found) {
            return Collections.emptyList();
        }

        LinkedList<Tuple> path = new LinkedList<>();
        Event e = cur;
        do {
            Tuple backEdge = parentMap1.get(e);
            path.addFirst(backEdge);
            e = backEdge.getFirst();
        } while (e != start);

        e = cur;
        while (e != end) {
            Tuple forwardEdge = parentMap2.get(e);
            path.addLast(forwardEdge);
            e = forwardEdge.getSecond();
        }

        return path;
    }
}
