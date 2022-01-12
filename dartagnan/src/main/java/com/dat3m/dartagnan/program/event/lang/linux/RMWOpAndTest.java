package com.dat3m.dartagnan.program.event.lang.linux;

import com.dat3m.dartagnan.configuration.Arch;
import com.dat3m.dartagnan.expression.*;
import com.dat3m.dartagnan.expression.op.COpBin;
import com.dat3m.dartagnan.expression.op.IOpBin;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Tag;
import com.dat3m.dartagnan.program.event.core.Event;
import com.dat3m.dartagnan.program.event.core.Load;
import com.dat3m.dartagnan.program.event.core.Local;
import com.dat3m.dartagnan.program.event.core.rmw.RMWStore;
import com.dat3m.dartagnan.program.event.core.utils.RegReaderData;
import com.dat3m.dartagnan.program.event.core.utils.RegWriter;
import com.google.common.base.Preconditions;

import java.math.BigInteger;
import java.util.List;

import static com.dat3m.dartagnan.program.event.EventFactory.*;

public class RMWOpAndTest extends RMWAbstract implements RegWriter, RegReaderData {

    private final IOpBin op;

    public RMWOpAndTest(IExpr address, Register register, IExpr value, IOpBin op) {
        super(address, register, value, Tag.Linux.MO_MB);
        this.op = op;
    }

    private RMWOpAndTest(RMWOpAndTest other){
        super(other);
        this.op = other.op;
    }

    @Override
    public String toString() {
        return resultRegister + " := atomic_" + op.toLinuxName() + "_and_test(" + value + ", " + address + ")";
    }

    public IOpBin getOp() {
    	return op;
    }
    
    @Override
    public ExprInterface getMemValue(){
        return value;
    }

    // Unrolling
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    public RMWOpAndTest getCopy(){
        return new RMWOpAndTest(this);
    }


    // Compilation
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    public List<Event> compile(Arch target) {
        Preconditions.checkArgument(target == Arch.NONE, "Compilation to " + target + " is not supported for " + getClass().getName());

        Register dummy = new Register(null, resultRegister.getThreadId(), resultRegister.getPrecision());
        Load load = newRMWLoad(dummy, address, Tag.Linux.MO_RELAXED);
        Local localOp = newLocal(dummy, new IExprBin(dummy, op, value));
        RMWStore store = newRMWStore(load, address, dummy, Tag.Linux.MO_RELAXED);
        Local test = newLocal(resultRegister, new Atom(dummy, COpBin.EQ, new IConst(BigInteger.ZERO, resultRegister.getPrecision())));

        //TODO: Are the memory barriers really unconditional?
        return eventSequence(
                com.dat3m.dartagnan.program.event.EventFactory.Linux.newMemoryBarrier(),
                load,
                localOp,
                store,
                test,
                com.dat3m.dartagnan.program.event.EventFactory.Linux.newMemoryBarrier()
        );
    }
}