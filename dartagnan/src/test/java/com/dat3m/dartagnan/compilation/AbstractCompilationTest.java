package com.dat3m.dartagnan.compilation;

import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.filter.FilterAbstract;
import com.dat3m.dartagnan.program.filter.FilterBasic;
import com.dat3m.dartagnan.program.filter.FilterUnion;
import com.dat3m.dartagnan.utils.ResourceHelper;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.utils.rules.Providers;
import com.dat3m.dartagnan.utils.rules.RequestShutdownOnError;
import com.dat3m.dartagnan.verification.RefinementTask;
import com.dat3m.dartagnan.verification.VerificationTask;
import com.dat3m.dartagnan.verification.solving.RefinementSolver;
import com.dat3m.dartagnan.wmm.Wmm;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.configuration.Property;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static com.dat3m.dartagnan.configuration.OptionNames.INITIALIZE_REGISTERS;

public abstract class AbstractCompilationTest {

    private static final boolean DO_INITIALIZE_REGISTERS = true;

    private String path;

    AbstractCompilationTest(String path) {
        this.path = path;
    }

    static Iterable<Object[]> buildLitmusTests(String litmusPath) throws IOException {
        int n = ResourceHelper.LITMUS_RESOURCE_PATH.length();
        Map<String, Result> expectationMap = ResourceHelper.getExpectedResults();

        try (Stream<Path> fileStream = Files.walk(Paths.get(ResourceHelper.LITMUS_RESOURCE_PATH + litmusPath))) {
            return fileStream
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .filter(f -> f.endsWith("litmus"))
                    .filter(f -> expectationMap.containsKey(f.substring(n)))
                    .collect(ArrayList::new,
                            (l, f) -> l.add(new Object[]{f}), ArrayList::addAll);
        }
    }


    // =================== Modifiable behavior ====================

    protected abstract Provider<Arch> getSourceProvider();
    protected abstract Provider<Arch> getTargetProvider();
    protected long getTimeout() { return 10000; }

    // ============================================================

    protected final Provider<ShutdownManager> shutdownManagerProvider = Provider.fromSupplier(ShutdownManager::create);
    protected final Provider<Arch> sourceProvider = getSourceProvider();
    protected final Provider<Arch> targetProvider = getTargetProvider();
    protected final Provider<String> filePathProvider = () -> path;
    protected final Provider<Program> program1Provider = Providers.createProgramFromPath(filePathProvider);
    protected final Provider<Program> program2Provider = Providers.createProgramFromPath(filePathProvider);
    protected final Provider<Wmm> wmm1Provider = Providers.createWmmFromArch(getSourceProvider());
    protected final Provider<Wmm> wmm2Provider = Providers.createWmmFromArch(getTargetProvider());
    protected final Provider<EnumSet<Property>> propertyProvider = Provider.fromSupplier(() -> EnumSet.of(Property.getDefault()));
    protected final Provider<Configuration> configProvider = Provider.fromSupplier(() -> Configuration.builder().setOption(INITIALIZE_REGISTERS, String.valueOf(DO_INITIALIZE_REGISTERS)).build());
    protected final Provider<VerificationTask> task1Provider = Providers.createTask(program1Provider, wmm1Provider, propertyProvider, sourceProvider, () -> 1, configProvider);
    protected final Provider<VerificationTask> task2Provider = Providers.createTask(program2Provider, wmm2Provider, propertyProvider, targetProvider,  () -> 1, configProvider);
    protected final Provider<SolverContext> context1Provider = Providers.createSolverContextFromManager(shutdownManagerProvider);
    protected final Provider<SolverContext> context2Provider = Providers.createSolverContextFromManager(shutdownManagerProvider);
    protected final Provider<ProverEnvironment> prover1Provider = Providers.createProverWithFixedOptions(context1Provider, ProverOptions.GENERATE_MODELS);
    protected final Provider<ProverEnvironment> prover2Provider = Providers.createProverWithFixedOptions(context2Provider, ProverOptions.GENERATE_MODELS);
    
    private final Timeout timeout = Timeout.millis(getTimeout());
    private final RequestShutdownOnError shutdownOnError = RequestShutdownOnError.create(shutdownManagerProvider);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(shutdownManagerProvider)
            .around(shutdownOnError)
            .around(filePathProvider)
            .around(program1Provider)
            .around(program2Provider)
            .around(wmm1Provider)
            .around(wmm2Provider)
            .around(propertyProvider)
            .around(configProvider)
            .around(task1Provider)
            .around(task2Provider)
            .around(timeout)
            // Context/Prover need to be created inside test-thread spawned by <timeout>
            .around(context1Provider)
            .around(context2Provider)
            .around(prover1Provider)
    		.around(prover2Provider);

    @Test
    public void testRefinement() throws Exception {
    	// The following have features (locks and RCU) that hardware models do not support
    	FilterAbstract rcu = FilterUnion.get(FilterBasic.get(Tag.Linux.RCU_LOCK), 
    			FilterUnion.get(FilterBasic.get(Tag.Linux.RCU_UNLOCK), FilterBasic.get(Tag.Linux.RCU_SYNC)));
    	FilterAbstract lock = FilterUnion.get(FilterBasic.get(Tag.Linux.LOCK_READ), 
    			FilterUnion.get(FilterBasic.get(Tag.Linux.LOCK_WRITE), FilterBasic.get(Tag.Linux.UNLOCK)));
    	if(task1Provider.get().getProgram().getCache().getEvents(FilterUnion.get(rcu, lock)).isEmpty()) {
        	if(RefinementSolver.run(context1Provider.get(), prover1Provider.get(),
                    RefinementTask.fromVerificationTaskWithDefaultBaselineWMM(task1Provider.get())).equals(Result.PASS)) {
        		assertEquals(Result.PASS, RefinementSolver.run(context2Provider.get(), prover2Provider.get(),
                        RefinementTask.fromVerificationTaskWithDefaultBaselineWMM(task2Provider.get())));
        	}
    	}
    }
}
