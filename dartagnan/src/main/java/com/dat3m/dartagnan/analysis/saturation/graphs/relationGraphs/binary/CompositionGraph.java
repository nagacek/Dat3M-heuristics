package com.dat3m.dartagnan.analysis.saturation.graphs.relationGraphs.binary;

import com.dat3m.dartagnan.analysis.saturation.graphs.relationGraphs.Edge;
import com.dat3m.dartagnan.analysis.saturation.graphs.relationGraphs.RelationGraph;
import com.dat3m.dartagnan.analysis.saturation.graphs.relationGraphs.utils.MaterializedGraph;
import com.dat3m.dartagnan.analysis.saturation.util.GraphVisitor;
import com.dat3m.dartagnan.utils.collections.SetUtil;
import com.dat3m.dartagnan.utils.timeable.Timestamp;
import com.dat3m.dartagnan.verification.model.ExecutionModel;

import java.util.*;

public class CompositionGraph extends MaterializedGraph {

    private final RelationGraph first;
    private final RelationGraph second;

    @Override
    public List<? extends RelationGraph> getDependencies() {
        return Arrays.asList(first, second);
    }

    public RelationGraph getFirst() { return first; }
    public RelationGraph getSecond() { return second; }

    public CompositionGraph(RelationGraph first, RelationGraph second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public void constructFromModel(ExecutionModel model) {
        super.constructFromModel(model);
        initialPopulation();
    }

    @Override
    public <TRet, TData, TContext> TRet accept(GraphVisitor<TRet, TData, TContext> visitor, TData data, TContext context) {
        return visitor.visitComposition(this, data, context);
    }

    private Edge combine(Edge a, Edge b, Timestamp time) {
        return new Edge(a.getFirst(), b.getSecond(), time,
                Math.max(a.getDerivationLength(), b.getDerivationLength()) + 1);
    }

    @Override
    public Collection<Edge> forwardPropagate(RelationGraph changedGraph, Collection<Edge> addedEdges) {
        ArrayList<Edge> newEdges = new ArrayList<>();
        if (changedGraph == first) {
            // (A+R);B = A;B + R;B
            addedEdges.forEach(e -> updateFirst(e, newEdges));
        }
        if (changedGraph == second) {
            // A;(B+R) = A;B + A;R
            addedEdges.forEach(e -> updateSecond(e, newEdges));
        }
        // For A;A, we have the following:
        // (A+R);(A+R) = A;A + A;R + R;A + R;R = A;A + (A+R);R + R;(A+R)
        // So we add (A+R);R and R;(A+R), which is done by doing both of the above update procedures
        return newEdges;
    }

    private void updateFirst(Edge a, Collection<Edge> addedEdges) {
        second.outEdgeStream(a.getSecond())
                .map(b -> combine(a, b, a.getTime()))
                .filter(simpleGraph::add).forEach(addedEdges::add);
    }

    private void updateSecond(Edge b, Collection<Edge> addedEdges) {
        first.inEdgeStream(b.getFirst())
                .map(a -> combine(a, b, b.getTime()))
                .filter(simpleGraph::add).forEach(addedEdges::add);
    }


    private void initialPopulation() {
        Set<Edge> fakeSet = SetUtil.fakeSet();
        if (first.getEstimatedSize() <= second.getEstimatedSize()) {
            first.edgeStream().forEach(a -> updateFirst(a, fakeSet));
        } else {
            second.edgeStream().forEach(b -> updateSecond(b, fakeSet));
        }
    }


}