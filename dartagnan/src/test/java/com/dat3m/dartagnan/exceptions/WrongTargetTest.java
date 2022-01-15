package com.dat3m.dartagnan.exceptions;

import com.dat3m.dartagnan.parsers.program.ProgramParser;
import com.dat3m.dartagnan.program.Program;
import com.dat3m.dartagnan.program.processing.LoopUnrolling;
import com.dat3m.dartagnan.program.processing.compilation.Compilation;
import com.dat3m.dartagnan.utils.ResourceHelper;
import com.dat3m.dartagnan.configuration.Arch;

import java.io.File;

import org.junit.Test;

public class WrongTargetTest {

    @Test(expected = IllegalArgumentException.class)
    public void X86CompiledToNone() throws Exception {
    	Program p = new ProgramParser().parse(new File(ResourceHelper.LITMUS_RESOURCE_PATH + "litmus/X86/2+2W+mfence-rmws.litmus"));
    	LoopUnrolling.newInstance().run(p);
    	Compilation comp = Compilation.newInstance();
		comp.setTarget(Arch.NONE);
		comp.run(p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void X86CompiledToPower() throws Exception {
    	Program p = new ProgramParser().parse(new File(ResourceHelper.LITMUS_RESOURCE_PATH + "litmus/X86/2+2W+mfence-rmws.litmus"));
    	LoopUnrolling.newInstance().run(p);
    	Compilation comp = Compilation.newInstance();
		comp.setTarget(Arch.POWER);
		comp.run(p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void X86CompiledToARM8() throws Exception {
    	Program p = new ProgramParser().parse(new File(ResourceHelper.LITMUS_RESOURCE_PATH + "litmus/X86/2+2W+mfence-rmws.litmus"));
    	LoopUnrolling.newInstance().run(p);
    	Compilation comp = Compilation.newInstance();
		comp.setTarget(Arch.ARM8);
		comp.run(p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ARMCompiledToNone() throws Exception {
    	Program p = new ProgramParser().parse(new File(ResourceHelper.LITMUS_RESOURCE_PATH + "litmus/AARCH64/ATOM/2+2W+poxxs.litmus"));
    	LoopUnrolling.newInstance().run(p);
    	Compilation comp = Compilation.newInstance();
		comp.setTarget(Arch.NONE);
		comp.run(p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ARMCompiledToTSO() throws Exception {
    	Program p = new ProgramParser().parse(new File(ResourceHelper.LITMUS_RESOURCE_PATH + "litmus/AARCH64/ATOM/2+2W+poxxs.litmus"));
    	LoopUnrolling.newInstance().run(p);
    	Compilation comp = Compilation.newInstance();
		comp.setTarget(Arch.TSO);
		comp.run(p);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ARMCompiledToPower() throws Exception {
    	Program p = new ProgramParser().parse(new File(ResourceHelper.LITMUS_RESOURCE_PATH + "litmus/AARCH64/ATOM/2+2W+poxxs.litmus"));
    	LoopUnrolling.newInstance().run(p);
    	Compilation comp = Compilation.newInstance();
		comp.setTarget(Arch.POWER);
		comp.run(p);
    }
}
