package com.dat3m.dartagnan.solver.caat4wmm.statistics.heuristics;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat4wmm.statistics.HotMap;
import com.dat3m.dartagnan.utils.dependable.DependencyGraph;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;

import java.util.List;

public abstract class EagerEncodingHeuristic {
    protected DependencyGraph<Relation> rels;

    public EagerEncodingHeuristic(VerificationTask task) {
        this.rels = task.getMemoryModel().getRelationDependencyGraph();
    }
    public abstract TupleSetMap chooseHotEdges(HotMap<List<Event>> edges, HotMap<List<Event>> edgesWOMemoized,
                               HotMap<List<Event>> iterations, HotMap<List<Event>> iterationsWOMemoized, HotMap<List<Event>> metric, int iteration);

    protected boolean isBase(String name) {
        for (Relation relation : rels.getNodeContents()) {
            if (relation.getName().equals(name)) {
                List<Relation> deps = relation.getDependencies();
                if (deps != null && deps.size() > 0) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return true;
    }
}
