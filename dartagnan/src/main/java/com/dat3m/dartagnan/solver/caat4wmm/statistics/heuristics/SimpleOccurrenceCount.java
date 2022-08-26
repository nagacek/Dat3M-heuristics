package com.dat3m.dartagnan.solver.caat4wmm.statistics.heuristics;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat4wmm.statistics.HotMap;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;

import java.util.Arrays;
import java.util.List;

public class SimpleOccurrenceCount extends EagerEncodingHeuristic {
    private int startIteration;
    private float strength;

    public SimpleOccurrenceCount(VerificationTask task, int startIteration, float strength) {
        super(task);
        this.startIteration = startIteration;
        this.strength = strength;
    }

    @Override
    public TupleSetMap chooseHotEdges(HotMap<List<Event>> edges, HotMap<List<Event>> edgesWOMemoized,
                                      HotMap<List<Event>> iterations, HotMap<List<Event>> iterationsWOMemoized, HotMap<List<Event>> metric, int iteration) {
        if (iteration < startIteration) {
            return new TupleSetMap();
        }
        //float average = edges.getAverage(s -> !isBase(s));
        //entry.getValue().current() >= average * strength &&
        //HashMap<String, Set<List<Event>>> chosen = edges.getAll(s -> !isBase(s), entry ->  entry.getKey().size() == 2 && strength - number.getAndDecrement() > 0);
        int bound = (int)strength;
        var sorted = edges.sort();
        TupleSetMap chosenEdges = new TupleSetMap();
        for (int i = 0; i < bound && i < sorted.size(); i++) {
            var value = sorted.get(i);
            if (!isBase(value.getKey()) && value.getValue().getKey().size() == 2) {
                //&& !getRelationByName(value.getKey()).getEncodeTupleSet().contains(new Tuple(value.getValue().getKey().get(0), value.getValue().getKey().get(1)))) {
                chosenEdges.merge(new TupleSetMap(value.getKey(), new TupleSet(Arrays.asList(new Tuple(value.getValue().getKey().get(0), value.getValue().getKey().get(1))))));
            } else {
                bound++;
            }
        }
        return chosenEdges;
    }
    
}
