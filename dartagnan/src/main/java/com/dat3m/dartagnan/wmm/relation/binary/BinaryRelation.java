package com.dat3m.dartagnan.wmm.relation.binary;

import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import com.google.common.collect.Sets;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Florian Furbach
 */
public abstract class BinaryRelation extends Relation {

    protected Relation r1;
    protected Relation r2;

    BinaryRelation(Relation r1, Relation r2) {
        this.r1 = r1;
        this.r2 = r2;
    }

    BinaryRelation(Relation r1, Relation r2, String name) {
        super(name);
        this.r1 = r1;
        this.r2 = r2;
    }

    public Relation getFirst() {
        return r1;
    }

    public Relation getSecond() {
        return r2;
    }

    @Override
    public List<Relation> getDependencies() {
        return Arrays.asList(r1 ,r2);
    }

    @Override
    public int updateRecursiveGroupId(int parentId){
        if(recursiveGroupId == 0 || forceUpdateRecursiveGroupId){
            forceUpdateRecursiveGroupId = false;
            int r1Id = r1.updateRecursiveGroupId(parentId | recursiveGroupId);
            int r2Id = r2.updateRecursiveGroupId(parentId | recursiveGroupId);
            recursiveGroupId |= (r1Id | r2Id) & parentId;
        }
        return recursiveGroupId;
    }

    @Override
    public TupleSetMap addEncodeTupleSet(TupleSet tuples){ // Not valid for composition
        TupleSet activeSet = new TupleSet(Sets.intersection(Sets.difference(tuples, encodeTupleSet), maxTupleSet));
        TupleSet oldEncodeSet = new TupleSet(encodeTupleSet);
        encodeTupleSet.addAll(activeSet);
        activeSet.removeAll(getMinTupleSet());

        TupleSet difference = new TupleSet(Sets.difference(encodeTupleSet, oldEncodeSet));
        TupleSetMap map = new TupleSetMap(getName(), difference);
        if(!activeSet.isEmpty()){
            map.merge(r1.addEncodeTupleSet(activeSet));
            map.merge(r2.addEncodeTupleSet(activeSet));
        }

        return map;
    }

    @Override
    public void incrementWeight(int amount) {
        weight += amount;
        r1.incrementWeight(amount);
        r2.incrementWeight(amount);
    }

    @Override
    public BooleanFormula encode(SolverContext ctx) {
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        if(isEncoded){
			return bmgr.makeTrue();
        }
        isEncoded = true;
        return bmgr.and(r1.encode(ctx), r2.encode(ctx), doEncode(ctx));
    }
}
