package com.dat3m.dartagnan.wmm.axiom;

import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import com.dat3m.dartagnan.wmm.utils.TupleSet;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

/**
 *
 * @author Florian Furbach
 */
public class Irreflexive extends Axiom {

    public Irreflexive(Relation rel) {
        super(rel);
    }

    @Override
    public TupleSet getEncodeTupleSet(){
        TupleSet set = new TupleSet();
        rel.getMaxTupleSet().stream().filter(Tuple::isLoop).forEach(set::add);
        return set;
    }

    @Override
    public BooleanFormula consistent(SolverContext ctx) {
    	BooleanFormulaManager bmgr = ctx.getFormulaManager().getBooleanFormulaManager();
		BooleanFormula enc = bmgr.makeTrue();
        for(Tuple tuple : rel.getEncodeTupleSet()){
            if(tuple.isLoop()){
                enc = bmgr.and(enc, bmgr.not(rel.getSMTVar(tuple, ctx)));
            }
        }
        return enc;
    }

    @Override
    public String toString() {
        return "irreflexive " + rel.getName();
    }
}