package com.dat3m.dartagnan.wmm.analysis.relationAnalysis.newWmm;

import com.dat3m.dartagnan.program.analysis.ExecutionAnalysis;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.verification.Context;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import org.sosy_lab.java_smt.api.*;

import static org.sosy_lab.java_smt.api.FormulaType.BooleanType;
import static org.sosy_lab.java_smt.api.FormulaType.IntegerType;

public class EncodingContext {

    private final SolverContext solverContext;
    private final Context analysisContext;

    private final ExecutionAnalysis execAnalysis;

    public EncodingContext(SolverContext solverContext, Context analysisContext) {
        this.solverContext = solverContext;
        this.analysisContext = analysisContext;

        execAnalysis = analysisContext.requires(ExecutionAnalysis.class);
    }

    public SolverContext getSolverContext() { return solverContext; }
    public BooleanFormulaManager getBmgr() { return solverContext.getFormulaManager().getBooleanFormulaManager(); }
    public IntegerFormulaManager getImgr() { return solverContext.getFormulaManager().getIntegerFormulaManager(); }
    public BitvectorFormulaManager getBvmgr() { return solverContext.getFormulaManager().getBitvectorFormulaManager(); }


    public BooleanFormula execPair(Event e1, Event e2) {
        if (e1.exec() == e2.exec()) {
            return e1.exec();
        }
        if (e1.getCId() > e2.getCId()) {
            Event temp = e1;
            e1 = e2;
            e2 = temp;
        }
        if (execAnalysis.isImplied(e1, e2)) {
            return e1.exec();
        } else if (execAnalysis.isImplied(e2 ,e1)) {
            return e2.exec();
        }
        return getBmgr().and(e1.exec(), e2.exec());
    }

    public final BooleanFormula execPair(Tuple t) {
        return execPair(t.getFirst(), t.getSecond());
    }

    public BooleanFormula edgeVar(String name, Tuple edge) {
        return edgeVar(name, edge.getFirst(), edge.getSecond());
    }

    public BooleanFormula edgeVar(String name, Event e1, Event e2) {
        FormulaManager fmgr = solverContext.getFormulaManager();
        return fmgr.makeVariable(BooleanType, fmgr.escape(name) + "(" + e1.repr() + "," + e2.repr() + ")");
    }

    public NumeralFormula.IntegerFormula clockVar(String name, Event e) {
        FormulaManager fmgr = solverContext.getFormulaManager();
        return fmgr.makeVariable(IntegerType, fmgr.escape(name) + "(" + e.repr() + ")");
    }

}
