package com.dat3m.dartagnan.solver.caat4wmm.statistics.heuristics;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat4wmm.statistics.HotMap;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.utils.RelationRepository;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;

import java.util.List;

public abstract class EagerEncodingHeuristic {
    protected RelationRepository rels;

    public EagerEncodingHeuristic(VerificationTask task) {
        this.rels = task.getMemoryModel().getRelationRepository();
    }
    public abstract TupleSetMap chooseHotEdges(HotMap<List<Event>> edges, HotMap<List<Event>> edgesWOMemoized,
                               HotMap<List<Event>> iterations, HotMap<List<Event>> iterationsWOMemoized, int iteration);

    protected boolean isBase(String name) {
        return rels.getBasicRelation(name) != null;
    }
}
