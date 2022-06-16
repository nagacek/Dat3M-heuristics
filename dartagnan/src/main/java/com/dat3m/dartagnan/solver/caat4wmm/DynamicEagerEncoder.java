package com.dat3m.dartagnan.solver.caat4wmm;

import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.RelationRepository;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverContext;

public class DynamicEagerEncoder {

    public static TupleSetMap determineEncodedTuples(TupleSetMap hotEdges, RelationRepository rels) {
        TupleSetMap toEncode = new TupleSetMap();
        for (var entry : hotEdges.getEntries()) {
            toEncode.merge(rels.getRelation(entry.getKey()).addEncodeTupleSet(entry.getValue()));
        }
        return toEncode;
    }

    public static BooleanFormula encodeEagerly(TupleSetMap toEncode, RelationRepository rels, SolverContext ctx) {
        BooleanFormulaManager manager = ctx.getFormulaManager().getBooleanFormulaManager();
        BooleanFormula eagerEncoding = manager.makeTrue();
        for (var entry : toEncode.getEntries()) {
            Relation rel = rels.getRelation(entry.getKey());
            eagerEncoding = manager.and(eagerEncoding, rel.encodeApprox(ctx, entry.getValue()));
        }
        return eagerEncoding;
    }
}
