package com.dat3m.dartagnan.expression;

import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.memory.Location;
import com.google.common.collect.ImmutableSet;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;

public class IfExpr implements ExprInterface {

	private final BExpr guard;
	private final ExprInterface tbranch;
	private final ExprInterface fbranch;
	
	public IfExpr(BExpr guard, ExprInterface tbranch, ExprInterface fbranch) {
		this.guard =  guard;
		this.tbranch = tbranch;
		this.fbranch = fbranch;
	}

	@Override
	public Expr toZ3Int(Event e, Context ctx) {
		return ctx.mkITE(guard.toZ3Bool(e, ctx), tbranch.toZ3Int(e, ctx), fbranch.toZ3Int(e, ctx));
	}

	@Override
	public BoolExpr toZ3Bool(Event e, Context ctx) {
		return (BoolExpr)ctx.mkITE(guard.toZ3Bool(e, ctx), tbranch.toZ3Bool(e, ctx), fbranch.toZ3Bool(e, ctx));
	}

	@Override
	public Expr getLastValueExpr(Context ctx) {
		// In principle this method is only called by assertions 
		// and thus it should never be called for this class
        throw new RuntimeException("Problem with getLastValueExpr in " + this.toString());
	}

	@Override
	public long getIntValue(Event e, Model model, Context ctx) {
		return guard.getBoolValue(e, model, ctx) ? tbranch.getIntValue(e, model, ctx) : fbranch.getIntValue(e, model, ctx);
	}

	@Override
	public boolean getBoolValue(Event e, Model model, Context ctx) {
		return guard.getBoolValue(e, model, ctx)? tbranch.getBoolValue(e, model, ctx) : fbranch.getBoolValue(e, model, ctx);
	}

	@Override
	public ImmutableSet<Register> getRegs() {
        return new ImmutableSet.Builder<Register>().addAll(guard.getRegs()).addAll(tbranch.getRegs()).addAll(fbranch.getRegs()).build();
	}

	@Override
	public ImmutableSet<Location> getLocs() {
		return new ImmutableSet.Builder<Location>().addAll(guard.getLocs()).addAll(tbranch.getLocs()).addAll(fbranch.getLocs()).build();
	}
	
    @Override
    public String toString() {
        return "(if " + guard + " then " + tbranch + " else " + fbranch + ")";
    }

	@Override
	public IConst reduce() {
		return guard.reduce().getValue() > 0 ? tbranch.reduce() : fbranch.reduce();
	}
	
	public BExpr getGuard() {
		return guard;
	}

	@Override
	public int getPrecision() {
		if(fbranch.getPrecision() != tbranch.getPrecision()) {
            throw new RuntimeException("The type of " + tbranch + " and " + fbranch + " does not match");
		}
		return tbranch.getPrecision();
	}
	
	@Override
	public IExpr getBase() {
		throw new UnsupportedOperationException("getBase not supported for " + this);
	}
}
