package com.dat3m.dartagnan.verification.solving;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat4wmm.Refiner;
import com.dat3m.dartagnan.solver.caat4wmm.ViolatingCycle;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.RelLiteral;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.wmm.axiom.Acyclic;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.google.common.collect.Sets;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// A simple class that represents a graph with edges annotated by reasons from which the edge was derived
// - The Graph is stored as a map from nodes (=events) to their incident edges
// - The reasons are stored as a map from edges to DNFs
// Right now, this class also uses a Refiner to generate encodings (i.e. translate (parts) of the Graph into a formula)
public class ReasonGraph {

    private Acyclic axiom;
    private Refiner refiner;
    private Map<Event, List<Tuple>> outEdges = new HashMap<>();
    private Map<Event, List<Tuple>> inEdges = new HashMap<>();
    private Map<Tuple, DNF<CoreLiteral>> reasonMap = new HashMap<>();

    // This is some testing code to keep track over which cycles were explicitly added,
    // which cycles are not need encoded and which newly found reasons for edges
    // are not yet encoded (if encoding of cycles/derivations should take place).
    private Set<List<Tuple>> encounteredCycles = new HashSet<>();
    private Set<List<Tuple>> notNewCycles = new HashSet<>();
    private List<List<Tuple>> newCyclesToBeEncoded = new ArrayList<>();
    private Map<Tuple, Set<Conjunction<CoreLiteral>>> derivationToBeEncoded = new HashMap<>();

    private Map<Event, Integer> hotEvents = new HashMap<>();
    private Map<Integer, Integer> cycleLength = new HashMap<>();
    private Set<List<Tuple>> allPaths = new HashSet<>();
    private Set<List<Tuple>> extrapolatedCycles = new HashSet<>();

    public ReasonGraph(Acyclic axiomToTrack, Refiner refiner) {
        this.axiom = axiomToTrack;
        this.refiner = refiner;
    }

    public Acyclic getAxiom() { return axiom;}

    // --------------- Test code ------------------
    // Add a violating cycle and return if new reasons have been found (or a new cycle)
    public boolean addCycle(ViolatingCycle vio) {
        List<Tuple> cycle = vio.getCycle();
        boolean changes = false;
        if (encounteredCycles.add(cycle)) {
            //newCyclesToBeEncoded.add(cycle);
            //changes = true; // Test code

            // Compute permutations for SAT version
            Set<List<Tuple>> permCycles = refiner.permuteTuples(cycle);
            newCyclesToBeEncoded.addAll(permCycles);
            encounteredCycles.addAll(permCycles);

           /*System.out.println("Added cycles:");
           for (List<Tuple> cyc : permCycles) {
               for (Tuple tuple : cyc) {
                   System.out.print("(" + tuple.getFirst().getCId() + "," + tuple.getSecond().getCId() + ") -> ");
               }
               if (cyc.equals(cycle)) {
                   System.out.print(" <- not permuted");
               }
               System.out.print("\n");
           }
           System.out.print("\n\n");*/
        }

        for (Tuple edge : cycle) {
            Conjunction<CoreLiteral> reason = vio.getEdgeReasons().get(edge);
            if (addReason(edge, reason)) {
                //derivationToBeEncoded.put(edge, reason);
                changes = true;

                //System.out.println("Reasons (" + edge.getFirst().getCId() + "," + edge.getSecond().getCId() + "):");

                // Compute permutations for SAT version
                List<CoreLiteral> reasonList = new ArrayList<>();
                reasonList.add(new RelLiteral(axiom.getRelation().getName(), edge, false));
                reasonList.addAll(reason.toList());
                Set<List<CoreLiteral>> permReasons = refiner.permute(reasonList);
                for (List<CoreLiteral> permutedDerivation : permReasons) {
                    assert permutedDerivation.get(0) instanceof RelLiteral;
                    Tuple permEdge = ((RelLiteral)permutedDerivation.remove(0)).getData();
                    Conjunction<CoreLiteral> permReason = new Conjunction<>(permutedDerivation);
                    Set<Conjunction<CoreLiteral>> reasonSet = new HashSet<>();
                    reasonSet.add(permReason);
                    if (derivationToBeEncoded.putIfAbsent(permEdge, reasonSet) != null) {
                        derivationToBeEncoded.get(permEdge).add(permReason);
                    }
                    //System.out.println("(" + permEdge.getFirst().getCId() + "," + permEdge.getSecond().getCId() + "): " + permReason + "\n");
                }
                //System.out.println("\n");
            }
        }
        if (!changes) {
            notNewCycles.add(cycle);
            for (Tuple edge : cycle) {
                Integer current = hotEvents.putIfAbsent(edge.getFirst(), 1);
                if (current != null) {
                    hotEvents.replace(edge.getFirst(), current + 1);
                }
            }
            Integer current = cycleLength.putIfAbsent(cycle.size(), 1);
            if (current != null) {
                cycleLength.replace(cycle.size(), current + 1);
            }
        }
        return changes;
    }

    // Encodes all changes (newly found cycles and newly found reasons) into a formula
    public BooleanFormula encodeChanges(SolverContext ctx) {
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        Relation rel = axiom.getRelation();
        BooleanFormula enc = bmgr.makeTrue();

        // Update derivations
        for (Tuple edge : derivationToBeEncoded.keySet()) {
            for (Conjunction<CoreLiteral> reason : derivationToBeEncoded.get(edge)) {
                enc = bmgr.and(enc, bmgr.implication(clause2Formulas(reason, ctx), rel.getSMTVar(edge, ctx)));
            }
        }

        // Encode new cycles (concretely, encode formula that disallows these cycles)
        for (List<Tuple> cycle : newCyclesToBeEncoded) {
            BooleanFormula noCycleFormula = cycle.stream().map(edge -> bmgr.not(rel.getSMTVar(edge, ctx)))
                    .reduce(bmgr.makeFalse(), bmgr::or);
            enc = bmgr.and(enc, noCycleFormula);
        }
        derivationToBeEncoded.clear();
        newCyclesToBeEncoded.clear();
        return enc;
    }

    public long extrapolateCyclesLengthFromTo(int from, int to) {
        long startTime = System.currentTimeMillis();
        for (Tuple start : reasonMap.keySet()) {
            List<Tuple> startList = new ArrayList<>();
            startList.add(start);
            allPaths.add(startList);
        }

        boolean wasAdded = true;
        while(wasAdded) {
            wasAdded = false;
            for (Event event : inEdges.keySet()) {
                Set<List<Tuple>> addPaths = new HashSet<>();
                allPaths.stream().filter(list -> list.size() < to && list.get(list.size() - 1).getSecond().equals(event)).forEach(list -> {
                    for (Tuple tuple : outEdges.get(event)) {
                        if (reasonMap.containsKey(new Tuple(tuple.getSecond(), list.get(list.size() -1).getFirst())) ||
                                reasonMap.containsKey(new Tuple(list.get(list.size() -1).getFirst(), tuple.getSecond()))) {
                            List<Tuple> copy = new ArrayList<>(list);
                            copy.add(tuple);
                            if (!allPaths.contains(copy)) {
                                addPaths.add(copy);
                            }
                        }
                    }
                });
                wasAdded = allPaths.addAll(addPaths);
            }
        }
        int currentCycles = newCyclesToBeEncoded.size();
        Set<List<Tuple>> addingCandidates = new HashSet<>();
        allPaths.stream().filter(list -> list.size() >= from && list.size() <= to && list.get(0).getFirst().equals(list.get(list.size() - 1).getSecond()))
                .forEach(addingCandidates::add);
        newCyclesToBeEncoded.addAll(Sets.difference(addingCandidates, extrapolatedCycles));
        extrapolatedCycles.addAll(addingCandidates);
        //System.out.println("Number of extrapolated paths: " + allPaths.size());
        //System.out.println("Number of added cycles: " + (newCyclesToBeEncoded.size() - currentCycles));
        return System.currentTimeMillis() - startTime;
    }

    // ------------------------------------------

    // Returns the list of edges ordered by the number of reasons they have (a lot of reasons = hot?!)
    public List<Map.Entry<Tuple, DNF<CoreLiteral>>> getHotnessList() {
        List<Map.Entry<Tuple, DNF<CoreLiteral>>> edgeReasons = new ArrayList<>(reasonMap.entrySet());
        edgeReasons.sort(Comparator.comparingInt(x -> -x.getValue().getNumberOfCubes()));
        return edgeReasons;
    }

    // Add new edge with a single reason. Returns true, if the reason is new (or the edge itself is new)
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


    // Test code that tries to encode short cycles in the reason graph, but it is highly stupid
    // and ends up encoding cycles of length 2 that can never happen (e.g. cycles with 2 opposing coherences)
    public BooleanFormula encodeShortestCycles(SolverContext ctx) {
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        //BooleanFormula enc = bmgr.makeFalse();
        DNF<CoreLiteral> enc = DNF.FALSE();

        Set<Event> nodes = new HashSet<>(inEdges.keySet());

        while (!nodes.isEmpty()) {
            Event node = nodes.stream().findFirst().get();
            List<Tuple> shortestPath = findShortestPath(node, node, edge -> true);

            if (shortestPath.isEmpty()) {
                continue;
            }
            //BooleanFormula cycle = bmgr.makeTrue();
            DNF<CoreLiteral> cycle = DNF.TRUE();
            for (Tuple t : shortestPath) {
                nodes.remove(t.getFirst());
                nodes.remove(t.getSecond());

                cycle = cycle.and(reasonMap.get(t));
                //cycle = bmgr.and(cycle, DNF2Formula(reasonMap.get(t), ctx));
            }
            enc = enc.or(cycle);
        }

        return bmgr.not(refiner.refine(enc, ctx));
    }

    public String showNotNew() {
        String cycleString = "";
        for (List<Tuple> cycle : notNewCycles) {
            for (Tuple edge : cycle) {
                cycleString += "(" + edge.getFirst().getCId() + "," + edge.getSecond().getCId() + ") -> ";
            }
            cycleString += "\n";
            /*for (Tuple edge : cycle) {
                cycleString += edge.toString() + " -> ";
            }
            cycleString += "}\n";*/

        }
        return cycleString;
    }

    public String showCycleLengthHotness() {
        StringBuilder returnString = new StringBuilder();
        cycleLength.entrySet().stream().sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .forEachOrdered(entry -> {
                    returnString.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        });
        return returnString.toString();
    }

    public String showEventHotness() {
        StringBuilder returnString = new StringBuilder();
        hotEvents.entrySet().stream().sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .forEachOrdered(entry -> {
                    returnString.append(entry.getKey().getCId()).append("  [").append(entry.getKey()).append("]: ").append(entry.getValue()).append("\n");
                });
        return returnString.toString();
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


    // ========== Copy of the Shortest Path Algorithm code used in CAAT ===========
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
