package com.dat3m.dartagnan.solver.caat4wmm;


import com.dat3m.dartagnan.solver.caat.CAATSolver;
import com.dat3m.dartagnan.solver.caat.constraints.AcyclicityConstraint;
import com.dat3m.dartagnan.solver.caat.constraints.Constraint;
import com.dat3m.dartagnan.solver.caat.predicates.relationGraphs.Edge;
import com.dat3m.dartagnan.solver.caat.reasoning.CAATLiteral;
import com.dat3m.dartagnan.solver.caat.reasoning.Reasoner;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreLiteral;
import com.dat3m.dartagnan.solver.caat4wmm.coreReasoning.CoreReasoner;
import com.dat3m.dartagnan.utils.logic.Conjunction;
import com.dat3m.dartagnan.utils.logic.DNF;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.verification.model.ExecutionModel;
import com.dat3m.dartagnan.wmm.analysis.RelationAnalysis;
import com.dat3m.dartagnan.wmm.axiom.Acyclic;
import com.dat3m.dartagnan.wmm.axiom.Axiom;
import com.dat3m.dartagnan.wmm.relation.Relation;
import com.dat3m.dartagnan.wmm.utils.Tuple;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.SolverContext;

import java.util.*;

/*
    This is our domain-specific bridging component that specializes the CAATSolver to the WMM setting.
*/
public class WMMSolver {

    private final ExecutionGraph executionGraph;
    private final ExecutionModel executionModel;
    private final CAATSolver solver;
    private final CoreReasoner reasoner;

    public WMMSolver(VerificationTask task, Set<Relation> cutRelations) {
        task.getAnalysisContext().requires(RelationAnalysis.class);
        this.executionGraph = new ExecutionGraph(task, cutRelations, true);
        this.executionModel = new ExecutionModel(task);
        this.reasoner = new CoreReasoner(task, executionGraph);
        this.solver = CAATSolver.create();
    }

    public ExecutionModel getExecution() {
        return executionModel;
    }

    public ExecutionGraph getExecutionGraph() {
        return executionGraph;
    }

    public Result check(Model model, SolverContext ctx) {
        // ============ Extract ExecutionModel ==============
        long curTime = System.currentTimeMillis();
        executionModel.initialize(model, ctx);
        executionGraph.initializeFromModel(executionModel);
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
            List<Conjunction<CoreLiteral>> coreReasons = new ArrayList<>(caatResult.getBaseReasons().getNumberOfCubes());
            for (Conjunction<CAATLiteral> baseReason : caatResult.getBaseReasons().getCubes()) {
                coreReasons.add(reasoner.toCoreReason(baseReason));
            }

            computeViolatingCycles(result); // Test code to compute cycle reasons

            stats.numComputedCoreReasons = coreReasons.size();
            result.coreReasons = new DNF<>(coreReasons);
            stats.numComputedReducedCoreReasons = result.coreReasons.getNumberOfCubes();
            stats.coreReasonComputationTime = System.currentTimeMillis() - curTime;
        }

        return result;
    }

    private void computeViolatingCycles(Result result) {
        // To avoid changing the code of CAATSolver,
        // we simply recompute all cycles and their reasons again
        // This is sufficient for testing.
        EventDomain dom = executionGraph.getDomain();
        Reasoner baseReasoner = solver.getReasoner();
        for (Map.Entry<Axiom, Constraint> axConstr : executionGraph.getAxiomConstraintMap().entrySet()) {
            if (!axConstr.getKey().isAcyclicity() || !axConstr.getValue().checkForViolations()) {
                continue;
            }
            List<ViolatingCycle> violations = new ArrayList<>();

            Acyclic ax = (Acyclic) axConstr.getKey();
            AcyclicityConstraint constr = (AcyclicityConstraint) axConstr.getValue();
            List<List<Edge>> cycles = constr.getViolations();

            for (List<Edge> cycle : cycles) {
                List<Tuple> convertedCycle = new ArrayList<>();
                Map<Tuple, Conjunction<CoreLiteral>> edgeReasons = new HashMap<>();
                for (Edge e : cycle) {
                    Tuple t = new Tuple(dom.getObjectById(e.getFirst()).getEvent(), dom.getObjectById(e.getSecond()).getEvent());
                    convertedCycle.add(t);
                    edgeReasons.put(t, reasoner.toCoreReason(baseReasoner.computeReason(constr.getConstrainedPredicate(), e)));
                }
                violations.add(new ViolatingCycle(ax, convertedCycle, edgeReasons));
            }
            result.violatingCycles.put(ax, violations);
        }

        // ----- Temporary test code -----
        // This code aggregates all edge-reasons into a single map
        // This is not needed, but some older testing code relied on such a map
        for (Map.Entry<Axiom, List<ViolatingCycle>> cycles : result.violatingCycles.entrySet()) {
            Map<Tuple, Conjunction<CoreLiteral>> combinedEdgeReasons = new HashMap<>();
            for (ViolatingCycle cycle : cycles.getValue()) {
                combinedEdgeReasons.putAll(cycle.getEdgeReasons());
            }
            result.cycleEdgeReasonsMap.put(cycles.getKey(), combinedEdgeReasons);
        }
    }


    // ===================== Classes ======================

    public static class Result {
        private CAATSolver.Status status;
        private DNF<CoreLiteral> coreReasons;
        // Obsolete, since the map can be computed from the cycles.
        private Map<Axiom, Map<Tuple, Conjunction<CoreLiteral>>> cycleEdgeReasonsMap;
        private Map<Axiom, List<ViolatingCycle>> violatingCycles;
        private Statistics stats;

        public CAATSolver.Status getStatus() { return status; }
        public DNF<CoreLiteral> getCoreReasons() { return coreReasons; }
        public Map<Axiom, List<ViolatingCycle>> getViolatingCycles() { return violatingCycles; }
        public Map<Axiom, Map<Tuple, Conjunction<CoreLiteral>>> getCycleEdgeReasons() { return cycleEdgeReasonsMap; }
        public Statistics getStatistics() { return stats; }

        Result() {
            status = CAATSolver.Status.INCONCLUSIVE;
            coreReasons = DNF.FALSE();
            violatingCycles = new HashMap<>();
            cycleEdgeReasonsMap = new HashMap<>();
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
