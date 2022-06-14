package com.dat3m.dartagnan.wmm.relation.unary;

import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import com.dat3m.dartagnan.wmm.utils.TupleSetTree;
import com.google.common.collect.Sets;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 *
 * @author Florian Furbach
 */
public class RelInverse extends UnaryRelation {
    //TODO/Note: We can forward getSMTVar calls
    // to avoid encoding this completely!

    public static String makeTerm(Relation r1){
        return r1.getName() + "^-1";
    }

    public RelInverse(Relation r1){
        super(r1);
        term = makeTerm(r1);
    }

    public RelInverse(Relation r1, String name) {
        super(r1, name);
        term = makeTerm(r1);
    }

    @Override
    public TupleSet getMinTupleSet(){
        if(minTupleSet == null){
            minTupleSet = r1.getMinTupleSet().inverse();
        }
        return minTupleSet;
    }

    @Override
    public TupleSet getMaxTupleSet(){
        if(maxTupleSet == null){
            maxTupleSet = r1.getMaxTupleSet().inverse();
        }
        return maxTupleSet;
    }

    @Override
    public TupleSetTree addEncodeTupleSet(TupleSet tuples){
        TupleSet activeSet = new TupleSet(Sets.intersection(Sets.difference(tuples, encodeTupleSet), maxTupleSet));
        TupleSet oldEncodeSet = new TupleSet(encodeTupleSet);
        encodeTupleSet.addAll(activeSet);
        TupleSet difference = new TupleSet(Sets.difference(encodeTupleSet, oldEncodeSet));
        TupleSetTree tree = new TupleSetTree(difference);
        activeSet.removeAll(getMinTupleSet());

        if(!activeSet.isEmpty()){
            tree.setR1(r1.addEncodeTupleSet(activeSet.inverse()));
        }

        return tree;
    }

    @Override
    protected BooleanFormula encodeApprox(SolverContext ctx) {
    	return encodeApprox(ctx, encodeTupleSet);
    }

    @Override
    public BooleanFormula encodeApprox(SolverContext ctx, TupleSet toEncode) {
        BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula enc = bmgr.makeTrue();


        TupleSet minSet = getMinTupleSet();
        for(Tuple tuple : toEncode){
            BooleanFormula opt = minSet.contains(tuple) ? getExecPair(tuple, ctx) : r1.getSMTVar(tuple.getInverse(), ctx);
            enc = bmgr.and(enc, bmgr.equivalence(this.getSMTVar(tuple, ctx), opt));
        }
        return enc;
    }
}