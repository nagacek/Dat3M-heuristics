package com.dat3m.dartagnan;

import com.dat3m.dartagnan.utils.Result;
import com.dat3m.dartagnan.utils.rules.CSVLogger;
import com.dat3m.dartagnan.utils.rules.Provider;
import com.dat3m.dartagnan.verification.RefinementTask;
import com.dat3m.dartagnan.verification.solving.AssumeSolver;
import com.dat3m.dartagnan.verification.solving.RefinementSolver;
import com.dat3m.dartagnan.configuration.Arch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;

import static com.dat3m.dartagnan.utils.ResourceHelper.TEST_RESOURCE_PATH;
import static com.dat3m.dartagnan.utils.Result.*;
import static com.dat3m.dartagnan.configuration.Arch.*;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class CLocksARM8 extends AbstractCTest {

    public CLocksARM8(String name, Arch target, Result expected) {
        super(name, target, expected);
    }

    @Override
    protected Provider<String> getProgramPathProvider() {
        return Provider.fromSupplier(() -> TEST_RESOURCE_PATH + "locks/" + name + ".bpl");
    }

    @Override
    protected long getTimeout() {
        return 60000;
    }

	@Parameterized.Parameters(name = "{index}: {0}, target={1}")
    public static Iterable<Object[]> data() throws IOException {
		return Arrays.asList(new Object[][]{
	            {"ttas-5", ARM8, UNKNOWN},
	            {"ttas-5-acq2rx", ARM8, UNKNOWN},
	            {"ttas-5-rel2rx", ARM8, FAIL},
	            {"ticketlock-3", ARM8, PASS},
	            {"ticketlock-3-acq2rx", ARM8, PASS},
	            {"ticketlock-3-rel2rx", ARM8, FAIL},
                {"mutex-3", ARM8, UNKNOWN},
                {"mutex-3-acq2rx_futex", ARM8, UNKNOWN},
                {"mutex-3-acq2rx_lock", ARM8, UNKNOWN},
                {"mutex-3-rel2rx_futex", ARM8, UNKNOWN},
                {"mutex-3-rel2rx_unlock", ARM8, FAIL},
                {"spinlock-5", ARM8, UNKNOWN},
                {"spinlock-5-acq2rx", ARM8, UNKNOWN},
                {"spinlock-5-rel2rx", ARM8, FAIL},
                {"linuxrwlock-3", ARM8, UNKNOWN},
                {"linuxrwlock-3-acq2rx", ARM8, FAIL},
                {"linuxrwlock-3-rel2rx", ARM8, FAIL},
                {"mutex_musl-3", ARM8, UNKNOWN},
                {"mutex_musl-3-acq2rx_futex", ARM8, UNKNOWN},
                {"mutex_musl-3-acq2rx_lock", ARM8, UNKNOWN},
                {"mutex_musl-3-rel2rx_futex", ARM8, UNKNOWN},
                {"mutex_musl-3-rel2rx_unlock", ARM8, FAIL},
//                {"cna-4", ARM8, UNKNOWN},
//                {"cna-4-rel2rx_unlock1", ARM8, FAIL},
//                {"cna-4-rel2rx_unlock2", ARM8, FAIL},
//                {"cna-4-rel2rx_unlock3", ARM8, FAIL},
//                {"cna-4-rel2rx_unlock4", ARM8, FAIL},
//                {"cna-4-rel2rx_lock", ARM8, UNKNOWN},
//                {"cna-4-acq2rx_lock", ARM8, FAIL},
//                {"cna-4-acq2rx_unlock", ARM8, UNKNOWN},
//                {"cna-4-acq2rx_succ1", ARM8, UNKNOWN},
//                {"cna-4-acq2rx_succ2", ARM8, UNKNOWN},
		});
    }

	@Test
	@CSVLogger.FileName("csv/assume")
	public void testAssume() throws Exception {
		assertEquals(expected, AssumeSolver.run(contextProvider.get(), proverProvider.get(), taskProvider.get()));
	}

	@Test
	@CSVLogger.FileName("csv/refinement")
	public void testRefinement() throws Exception {
		assertEquals(expected, RefinementSolver.run(contextProvider.get(), proverProvider.get(),
				RefinementTask.fromVerificationTaskWithDefaultBaselineWMM(taskProvider.get())));
	}
}