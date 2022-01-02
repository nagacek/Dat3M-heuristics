package com.dat3m.dartagnan.program.arch.aarch64.event;

import com.dat3m.dartagnan.expression.ExprInterface;
import com.dat3m.dartagnan.expression.IExpr;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.ExecutionStatus;
import com.dat3m.dartagnan.program.event.Store;
import com.dat3m.dartagnan.program.event.rmw.RMWStoreExclusive;
import com.dat3m.dartagnan.program.event.utils.RegReaderData;
import com.dat3m.dartagnan.program.event.utils.RegWriter;
import com.dat3m.dartagnan.program.utils.EType;
import com.dat3m.dartagnan.wmm.utils.Arch;
import com.google.common.base.Preconditions;

import java.util.List;

import static com.dat3m.dartagnan.program.EventFactory.*;

public class StoreExclusive extends Store implements RegWriter, RegReaderData {

    private final Register register;

    public StoreExclusive(Register register, IExpr address, ExprInterface value, String mo){
        super(address, value, mo);
        this.register = register;
        addFilters(EType.EXCL, EType.REG_WRITER);
    }

    private StoreExclusive(StoreExclusive other){
        super(other);
        this.register = other.register;
    }

    @Override
    public Register getResultRegister(){
        return register;
    }

    @Override
    public String toString(){
        return register + " <- store(*" + address + ", " + value + (mo != null ? ", " + mo : "") + ")";
    }

    // Unrolling
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    public StoreExclusive getCopy(){
        return new StoreExclusive(this);
    }


    // Compilation
    // -----------------------------------------------------------------------------------------------------------------

    @Override
    public List<Event> compile(Arch target) {
        Preconditions.checkArgument(target == Arch.ARM8, "Compilation of StoreExclusive is not implemented for " + target);

        RMWStoreExclusive store = newRMWStoreExclusive(address, value, mo);
        ExecutionStatus status = newExecutionStatus(register, store);
        return eventSequence(
                store,
                status
        );
    }
}
