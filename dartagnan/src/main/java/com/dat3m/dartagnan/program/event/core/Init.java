package com.dat3m.dartagnan.program.event.core;

import com.dat3m.dartagnan.expression.IConst;
import com.dat3m.dartagnan.expression.IExpr;
import com.dat3m.dartagnan.program.event.Tag;
import org.sosy_lab.java_smt.api.SolverContext;

public class Init extends MemEvent {

	private final IConst value;
	
	public Init(IExpr address, IConst value) {
		super(address, null);
		this.value = value;
		addFilters(Tag.ANY, Tag.VISIBLE, Tag.MEMORY, Tag.WRITE, Tag.INIT);
	}

	public IConst getValue(){
		return value;
	}

	@Override
	public void initializeEncoding(SolverContext ctx) {
		super.initializeEncoding(ctx);
		memValueExpr = value.toIntFormula(ctx);
	}

	@Override
	public String toString() {
		return "*" + address + " := " + value;
	}

	@Override
	public IConst getMemValue(){
		return value;
	}
}