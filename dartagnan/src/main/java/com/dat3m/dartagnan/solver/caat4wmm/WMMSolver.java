package com.dat3m.dartagnan.solver.caat4wmm;


import com.dat3m.dartagnan.solver.caat.CAATSolver;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreReasoner;
//import com.dat3m.dartagnan.solver.caat4wmm.statistics.GlobalStatistics;
import com.dat3m.dartagnan.solver.caat4wmm.statistics.IntermediateStatistics;
import com.dat3m.dartagnan.solver.caat4wmm.statistics.heuristics.*;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.TupleSetMap;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/*
    This is our domain-specific bridging component that specializes the CAATSolver to the WMM setting.
*/
public class WMMSolver {

    private final ExecutionGraph executionGraph;
    private final ExecutionModel executionModel;
    private final CAATSolver solver;
    private final CoreReasoner reasoner;
    private final IntermediateStatistics intermediateStats;
    private final EagerEncodingHeuristic heuristic;
    private boolean initGlobalStats;

    public WMMSolver(VerificationTask task, Set<Relation> cutRelations) {
        task.getAnalysisContext().requires(RelationAnalysis.class);
        this.executionGraph = new ExecutionGraph(task, cutRelations, true);
        this.executionModel = new ExecutionModel(task);
        this.reasoner = new CoreReasoner(task, executionGraph);
        this.solver = CAATSolver.create();
        this.intermediateStats = null;
        this.heuristic = null;
    }

    public WMMSolver(VerificationTask task, Set<Relation> cutRelations, IntermediateStatistics stats) {
        task.getAnalysisContext().requires(RelationAnalysis.class);
        this.executionGraph = new ExecutionGraph(task, cutRelations, true);
        this.executionModel = new ExecutionModel(task);
        EdgeManager manager = new EdgeManager(executionModel);
        this.reasoner = new CoreReasoner(task, executionGraph, manager);
        this.solver = CAATSolver.create(stats, manager);
        this.intermediateStats = stats;
        this.heuristic = new WeightedRelations(task, 5, 3);
    }

    public ExecutionModel getExecution() {
        return executionModel;
    }

    public ExecutionGraph getExecutionGraph() {
        return executionGraph;
    }

    public void addEagerlyEncodedEdges(TupleSetMap edges) {
        solver.addEagerlyEncodedEdges(edges);
    }

    public Result check(Model model, SolverContext ctx) {
        // ============ Extract ExecutionModel ==============
        long curTime = System.currentTimeMillis();
        executionModel.initialize(model, ctx);
        executionGraph.initializeFromModel(executionModel);
        intermediateStats.initializeFromExecutionGraph(executionGraph);
        if (IntermediateStatistics.use && initGlobalStats) {
            //GlobalStatistics.initialize(executionGraph);
            initGlobalStats = false;
        }
        long extractTime = System.currentTimeMillis() - curTime;

        // ============== Run the CAATSolver ==============
        CAATSolver.Result caatResult = solver.check(executionGraph.getCAATModel());
        Result result = Result.fromCAATResult(caatResult);
        Statistics stats = result.stats;
        stats.modelExtractionTime = extractTime;
        stats.modelSize = executionGraph.getDomain().size();

        if (result.getStatus() == CAATSolver.Status.INCONSISTENT) {
            // ============== Compute Core reasons ==============
            curTime = System.currentTimeMillis();
            DNF<CAATLiteral> baseReasons = caatResult.getBaseReasons();
            List<Conjunction<CoreLiteral>> coreReasons = new ArrayList<>(baseReasons.getNumberOfCubes());
            for (Conjunction<CAATLiteral> baseReason : caatResult.getBaseReasons().getCubes()) {
                coreReasons.add(reasoner.toCoreReason(baseReason));
            }
            stats.numComputedCoreReasons = coreReasons.size();
            result.coreReasons = new DNF<>(coreReasons);
            result.hotEdges = intermediateStats.computeHotEdges(heuristic);
            /*if (GlobalStatistics.globalStats) {
                GlobalStatistics.insertCoreEdges(coreReasons);
                GlobalStatistics.insertBaseEdges(baseReasons);
            }*/
            stats.numComputedReducedCoreReasons = result.coreReasons.getNumberOfCubes();
            stats.coreReasonComputationTime = System.currentTimeMillis() - curTime;
        }

        return result;
    }

    public void initializeGlobalStats() {
        initGlobalStats = true;
    }


    // ===================== Classes ======================

    public static class Result {
        private CAATSolver.Status status;
        private DNF<CoreLiteral> coreReasons;
        private Statistics stats;
        private TupleSetMap hotEdges;

        public CAATSolver.Status getStatus() { return status; }
        public DNF<CoreLiteral> getCoreReasons() { return coreReasons; }
        public Statistics getStatistics() { return stats; }
        public TupleSetMap getHotEdges() { return hotEdges; }

        Result() {
            status = CAATSolver.Status.INCONCLUSIVE;
            coreReasons = DNF.FALSE();
            hotEdges = new TupleSetMap();
        }

        static Result fromCAATResult(CAATSolver.Result caatResult) {
            Result result = new Result();
            result.status = caatResult.getStatus();
            result.stats = new Statistics();
            result.stats.caatStats = caatResult.getStatistics();

            return result;
        }


        @Override
        public String toString() {
            return status + "\n" +
                    coreReasons + "\n" +
                    hotEdges + "\n" +
                    stats;
        }
    }

    public static class Statistics {
        CAATSolver.Statistics caatStats;
        long modelExtractionTime;
        long coreReasonComputationTime;
        int modelSize;
        int numComputedCoreReasons;
        int numComputedReducedCoreReasons;

        public long getModelExtractionTime() { return modelExtractionTime; }
        public long getPopulationTime() { return caatStats.getPopulationTime(); }
        public long getBaseReasonComputationTime() { return caatStats.getReasonComputationTime(); }
        public long getCoreReasonComputationTime() { return coreReasonComputationTime; }
        public long getConsistencyCheckTime() { return caatStats.getConsistencyCheckTime(); }
        public int getModelSize() { return modelSize; }
        public int getNumComputedBaseReasons() { return caatStats.getNumComputedReasons(); }
        public int getNumComputedReducedBaseReasons() { return caatStats.getNumComputedReducedReasons(); }
        public int getNumComputedCoreReasons() { return numComputedCoreReasons; }
        public int getNumComputedReducedCoreReasons() { return numComputedReducedCoreReasons; }


        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append("Model extraction time(ms): ").append(getModelExtractionTime()).append("\n");
            str.append("Population time(ms): ").append(getPopulationTime()).append("\n");
            str.append("Consistency check time(ms): ").append(getConsistencyCheckTime()).append("\n");
            str.append("Base Reason computation time(ms): ").append(getBaseReasonComputationTime()).append("\n");
            str.append("Core Reason computation time(ms): ").append(getCoreReasonComputationTime()).append("\n");
            str.append("Model size (#events): ").append(getModelSize()).append("\n");
            str.append("#Computed reasons (base/core): ").append(getNumComputedBaseReasons())
                    .append("/").append(getNumComputedCoreReasons()).append("\n");
            str.append("#Computed reduced reasons (base/core): ").append(getNumComputedReducedBaseReasons())
                    .append("/").append(getNumComputedReducedCoreReasons()).append("\n");
            return str.toString();
        }

    }

}
