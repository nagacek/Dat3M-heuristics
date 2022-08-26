package com.dat3m.dartagnan.solver.caat4wmm.statistics.heuristics;

import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.solver.caat4wmm.statistics.HotMap;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.relation.unary.RelTransRef;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;

import java.util.Arrays;
import java.util.List;

public class WeightedRelations extends EagerEncodingHeuristic {
    private final List<Relation> allRelations;
    private int strength;
    private int startIteration;

    public WeightedRelations(VerificationTask task, int startIteration, int strength) {
        super(task);
        allRelations = task.getMemoryModel().getRelationDependencyGraph().getNodeContents();
        this.startIteration = startIteration;
        this.strength = strength;
    }

    @Override
    public TupleSetMap chooseHotEdges(HotMap<List<Event>> edges, HotMap<List<Event>> edgesWOMemoized, HotMap<List<Event>> iterations, HotMap<List<Event>> iterationsWOMemoized, HotMap<List<Event>> metric, int iteration) {
        if (iteration < startIteration) {
            return new TupleSetMap();
        }

        var sorted = edges.sort((obj1, obj2) -> {
            float obj2Current = obj2.getValue().getValue().current();
            Relation rel2 = getRelationByName(obj2.getKey());
            float obj2Weight = rel2 == null ? 0 : rel2.getWeight();
            float obj2Compare = obj2Current * obj2Weight;
            float obj1Current = obj1.getValue().getValue().current();
            Relation rel1 = getRelationByName(obj1.getKey());
            float obj1Weight = rel1 == null ? 0 : rel1.getWeight();
            float obj1Compare = obj1Current * obj1Weight;

            return Float.compare(obj2Compare, obj1Compare);
        });
        TupleSetMap chosenEdges = new TupleSetMap();
        int bound = strength;
        for (int i = 0; i < bound && i < sorted.size(); i++) {
            var value = sorted.get(i);
            if (!isBase(value.getKey()) && value.getValue().getKey().size() == 2 && !(getRelationByName(value.getKey()) instanceof RelTransRef)//){
                    && !getRelationByName(value.getKey()).getEncodeTupleSet().contains(new Tuple(value.getValue().getKey().get(0), value.getValue().getKey().get(1)))) {
                chosenEdges.merge(new TupleSetMap(value.getKey(), new TupleSet(Arrays.asList(new Tuple(value.getValue().getKey().get(0), value.getValue().getKey().get(1))))));
            } else {
                bound++;
            }
        }
        return chosenEdges;
    }

    private Relation getRelationByName(String name) {
        for (Relation relation : allRelations) {
            if (relation.getName().equals(name)) {
                return relation;
            }
        }
        return null;
    }
}
