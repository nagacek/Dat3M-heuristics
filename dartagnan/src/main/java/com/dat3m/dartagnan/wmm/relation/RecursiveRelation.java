package com.dat3m.dartagnan.wmm.relation;

import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Florian Furbach
 */
public class RecursiveRelation extends Relation {

    private Relation r1;
    private boolean doRecurse = false;

    private boolean weightRecursion = true;
    private int initialAmount;

    public Relation getInner() {
        return r1;
    }

    @Override
    public List<Relation> getDependencies() {
        return Collections.singletonList(r1);
    }

    public RecursiveRelation(String name) {
        super(name);
        term = name;
    }

    public static String makeTerm(String name){
        return name;
    }

    @Override
    public void initializeEncoding(SolverContext ctx){
        if(doRecurse){
            doRecurse = false;
            super.initializeEncoding(ctx);
            r1.initializeEncoding(ctx);
        }
    }

    @Override
    public void initializeRelationAnalysis(VerificationTask task, Context context) {
        if(doRecurse){
            doRecurse = false;
            super.initializeRelationAnalysis(task, context);
            r1.initializeRelationAnalysis(task, context);
        }
    }

    public void setConcreteRelation(Relation r1){
        r1.isRecursive = true;
        r1.setName(name);
        this.r1 = r1;
        this.isRecursive = true;
        this.term = r1.getTerm();
    }

    public void setDoRecurse(){
        doRecurse = true;
    }

    @Override
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            minTupleSet = new TupleSet();
        }
        return minTupleSet;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = new TupleSet();
        }
        return maxTupleSet;
    }

    @Override
    public TupleSet getMinTupleSetRecursive(){
        if(doRecurse){
            doRecurse = false;
            minTupleSet = r1.getMinTupleSetRecursive();
            return minTupleSet;
        }
        return getMinTupleSet();
    }

    @Override
    public TupleSet getMaxTupleSetRecursive(){
        if(doRecurse){
            doRecurse = false;
            maxTupleSet = r1.getMaxTupleSetRecursive();
            return maxTupleSet;
        }
        return getMaxTupleSet();
    }

    @Override
    public TupleSetMap addEncodeTupleSet(TupleSet tuples){
        TupleSet oldEncodeSet = new TupleSet(encodeTupleSet);
        TupleSet difference = new TupleSet();
        TupleSetMap map = new TupleSetMap(getName(), difference);
        if(encodeTupleSet != tuples){
            encodeTupleSet.addAll(tuples);
            difference.addAll(encodeTupleSet);
            //TODO: This encodeTupleSet is never used except to stop this recursion
            // Can it get larger than r1's encodeTupleSet???
        }
        if(doRecurse){
            doRecurse = false;
            map.merge(r1.addEncodeTupleSet(encodeTupleSet));
        }
        difference.removeAll(oldEncodeSet);

        return map;
    }

    @Override
    public void incrementWeight(int amount) {
        if (weightRecursion) {
            weightRecursion = false;
            initialAmount = amount;
            weight += amount;
            r1.incrementWeight(amount);
        } else {
            weight += amount - initialAmount;
        }
    }

    public void setWeightRecursion() { weightRecursion = true; }

    @Override
    public int updateRecursiveGroupId(int parentId){
        if(forceUpdateRecursiveGroupId){
            forceUpdateRecursiveGroupId = false;
            int r1Id = r1.updateRecursiveGroupId(parentId | recursiveGroupId);
            recursiveGroupId |= r1Id & parentId;
        }
        return recursiveGroupId;
    }

    @Override
    public BooleanFormula encode(SolverContext ctx) {
        if(isEncoded){
            return ctx.getFormulaManager().getBooleanFormulaManager().makeTrue();
        }
        isEncoded = true;
        return r1.encode(ctx);
    }

    @Override
    protected BooleanFormula encodeApprox(SolverContext ctx) {
        return r1.encodeApprox(ctx);
    }

    @Override
    public BooleanFormula encodeApprox(SolverContext ctx, TupleSet toEncode) {
        return encodeApprox(ctx);
    }
}
