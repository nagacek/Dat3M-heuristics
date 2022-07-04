package com.dat3m.dartagnan.solver.caat4wmm;

import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.wmm.axiom.Acyclic;
import com.dat3m.dartagnan.wmm.utils.Tuple;

import java.util.List;
import java.util.Map;

// Representation of a cycle that violates an axiom, including the reasons from
// which each edge can be derived.
public class ViolatingCycle {

    private final Acyclic axiom;
    private final List<Tuple> cycle;
    private final Map<Tuple, Conjunction<CoreLiteral>> edgeReasons;

    public ViolatingCycle(Acyclic axiom, List<Tuple> cycle, Map<Tuple, Conjunction<CoreLiteral>> edgeReasons) {
        this.axiom = axiom;
        this.cycle = cycle;
        this.edgeReasons = edgeReasons;
    }

    public Acyclic getAxiom() {
        return axiom;
    }

    public List<Tuple> getCycle() {
        return cycle;
    }

    public Map<Tuple, Conjunction<CoreLiteral>> getEdgeReasons() {
        return edgeReasons;
    }

}
