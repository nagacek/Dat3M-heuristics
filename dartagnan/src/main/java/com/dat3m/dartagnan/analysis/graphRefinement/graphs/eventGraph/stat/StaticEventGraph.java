package com.dat3m.dartagnan.analysis.graphRefinement.graphs.eventGraph.stat;

import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.analysis.graphRefinement.coreReason.CoreLiteral;
import com.dat3m.dartagnan.analysis.graphRefinement.coreReason.EventLiteral;
import com.dat3m.dartagnan.analysis.graphRefinement.graphs.eventGraph.EventGraph;
import com.dat3m.dartagnan.analysis.graphRefinement.logic.Conjunction;
import com.dat3m.dartagnan.verification.model.Edge;
import com.dat3m.dartagnan.verification.model.EventData;
import com.dat3m.dartagnan.analysis.graphRefinement.util.EdgeDirection;
import com.dat3m.dartagnan.analysis.graphRefinement.graphs.eventGraph.AbstractEventGraph;
import com.dat3m.dartagnan.utils.timeable.Timestamp;

import java.util.Collections;
import java.util.List;

public abstract class StaticEventGraph extends AbstractEventGraph {
    protected int size;

    @Override
    public List<EventGraph> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public Timestamp getTime(Edge edge) {
        return contains(edge) ? Timestamp.ZERO : Timestamp.INVALID;
    }

    @Override
    public Timestamp getTime(EventData a, EventData b) {
        return contains(a, b) ? Timestamp.ZERO : Timestamp.INVALID;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int getMinSize() { return size; }

    @Override
    public int getMaxSize() { return size; }

    @Override
    public int getEstimatedSize() { return size; }

    @Override
    public abstract int getMinSize(EventData e, EdgeDirection dir);

    @Override
    public int getMaxSize(EventData e, EdgeDirection dir) {
        return getMinSize(e, dir);
    }

    @Override
    public int getEstimatedSize(EventData e, EdgeDirection dir) {
        return getMinSize(e, dir);
    }

    @Override
    public void backtrack() {
    }

    @Override
    public void initialize(ExecutionModel context) {
        super.initialize(context);
        size = 0;
    }

    @Override
    public Conjunction<CoreLiteral> computeReason(Edge edge) {
        if (!contains(edge))
            return Conjunction.FALSE;
        return new Conjunction<>(new EventLiteral(edge.getFirst()), new EventLiteral(edge.getSecond()));
    }


    protected final void autoComputeSize() {
        size = 0;
        for (EventData e : context.getEventList())
            size += getMinSize(e, EdgeDirection.Outgoing);
    }

}