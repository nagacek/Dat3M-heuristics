package com.dat3m.dartagnan.solver.caat4wmm;

import com.dat3m.dartagnan.solver.caat.misc.EdgeSetMap;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;

public class EdgeManager {
    private final TupleSetMap edges;
    private EdgeSetMap caatEdges;
    private ExecutionModel model;

    public EdgeManager(ExecutionModel model) {
        edges = new TupleSetMap();
        this.model = model;
    }

    public EdgeManager() {
        edges = new TupleSetMap();
    }

    public void addEagerlyEncodedEdges(TupleSetMap newEdges) {
        edges.merge(newEdges);
    }

    public void initCAATView() {
        caatEdges = EdgeSetMap.fromTupleSetMap(edges, model);
    }

    public boolean isEagerlyEncoded(String name, Edge edge) {
        return caatEdges.contains(name, edge);
    }

    public boolean isEagerlyEncoded(String name) {
        return edges.contains(name);
    }
}
