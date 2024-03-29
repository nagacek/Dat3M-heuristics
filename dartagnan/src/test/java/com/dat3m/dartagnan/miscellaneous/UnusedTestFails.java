package com.dat3m.dartagnan.miscellaneous;

import com.dat3m.dartagnan.GlobalSettings;
import com.dat3m.dartagnan.parsers.cat.ParserCat;
import com.dat3m.dartagnan.utils.ResourceHelper;
import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.wmm.Wmm;
import com.dat3m.dartagnan.configuration.Arch;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
NOTE: We use these tests to collect benchmarks that are failing for some reason
(buggy, too slow, unsupported features etc.)
 */
@RunWith(Parameterized.class)
public class UnusedTestFails {

    static final int TIMEOUT = 600000;

    private final String path;
    private final Wmm wmm;
    private final Arch target;
    private final Result expected;

    public UnusedTestFails(String path, Wmm wmm, Arch target, Result expected) {
        this.path = path;
        this.wmm = wmm;
        this.target = target;
        this.expected = expected;
    }

	@Parameterized.Parameters(name = "{index}: {0} bound={2}")
    public static Iterable<Object[]> data() throws IOException {
        String scCat = GlobalSettings.ATOMIC_AS_LOCK ? "cat/svcomp-locks.cat" : "cat/svcomp.cat";
        Wmm sc = new ParserCat().parse(new File(ResourceHelper.CAT_RESOURCE_PATH + scCat));
        Wmm tso = new ParserCat().parse(new File(ResourceHelper.CAT_RESOURCE_PATH + "cat/tso.cat"));
        Wmm power = new ParserCat().parse(new File(ResourceHelper.CAT_RESOURCE_PATH + "cat/power.cat"));
        Wmm arm = new ParserCat().parse(new File(ResourceHelper.CAT_RESOURCE_PATH + "cat/aarch64.cat"));

        List<Object[]> data = new ArrayList<>();
        //data.add(new Object[]{"../tests/mutex-2.c", tso, POWER, s1, UNKNOWN});
        //data.add(new Object[]{"../lfds/ms_datCAS-O0.bpl", wmm, s2});
        //data.add(new Object[]{"../lfds/ms-O0.bpl", wmm, s2});
        //data.add(new Object[]{"../output/ms-test-O0.bpl", wmm, s2});
        //data.add(new Object[]{"../output/ttas-5-O0.bpl", wmm, s2});
        //data.add(new Object[]{"../output/mutex-4-O0.bpl", wmm, s2});

        return data;
    }

    //@Test(timeout = TIMEOUT)
//    public void test() {
//        try (SolverContext ctx = TestHelper.createContext();
//             ProverEnvironment prover = ctx.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS))
//        {
//            Program program = new ProgramParser().parse(new File(path));
//            VerificationTask task = new VerificationTask(program, wmm, target, settings);
//            assertEquals(expected, AssumeSolver.run(ctx, prover, task));
//        } catch (Exception e){
//            fail(e.getMessage());
//        }
//    }
//
//    //@Test(timeout = TIMEOUT)
//    public void testRefinement() {
//        try (SolverContext ctx = TestHelper.createContext();
//             ProverEnvironment prover = ctx.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS))
//        {
//            Program program = new ProgramParser().parse(new File(path));
//            VerificationTask task = new VerificationTask(program, wmm, target, settings);
//            assertEquals(expected, RefinementSolver.run(ctx, prover,
//                    RefinementTask.fromVerificationTaskWithDefaultBaselineWMM(task)));
//        } catch (Exception e){
//            fail(e.getMessage());
//        }
//    }
}