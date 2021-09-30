package com.dat3m.dartagnan.program.utils;

import com.dat3m.dartagnan.expression.IExpr;
import com.dat3m.dartagnan.program.Register;
import com.dat3m.dartagnan.program.event.Event;
import com.dat3m.dartagnan.program.event.MemEvent;
import com.dat3m.dartagnan.program.event.utils.RegReaderData;
import com.dat3m.dartagnan.program.event.utils.RegWriter;
import com.dat3m.dartagnan.wmm.filter.FilterAbstract;
import com.dat3m.dartagnan.wmm.filter.FilterBasic;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.*;

public class ThreadCache {

    private final Map<FilterAbstract, ImmutableList<Event>> events = new HashMap<>();
    private ImmutableSet<Register> registers;
    private ImmutableMap<Register, ImmutableList<Event>> regWriterMap;

    public ThreadCache(List<Event> events){
        this.events.put(FilterBasic.get(EType.ANY), ImmutableList.copyOf(events));
    }

    public ImmutableList<Event> getEvents(FilterAbstract filter){
        if(!events.containsKey(filter)){
            ImmutableList.Builder<Event> builder = new ImmutableList.Builder<>();
            for(Event e : getEvents(FilterBasic.get(EType.ANY))){
                if(filter.filter(e)){
                    builder.add(e);
                }
            }
            events.put(filter, builder.build());
        }
        return events.get(filter);
    }

    public ImmutableSet<Register> getRegisters(){
        if(registers == null){
            ImmutableSet.Builder<Register> builder = new ImmutableSet.Builder<>();
            for(Event e : getEvents(FilterBasic.get(EType.ANY))){
                if(e instanceof RegWriter){
                    builder.add(((RegWriter) e).getResultRegister());
                }
                if(e instanceof MemEvent){
                    IExpr address = ((MemEvent) e).getAddress();
                    builder.addAll(address.getRegs());
                }
                if(e instanceof RegReaderData){
                    builder.addAll(((RegReaderData) e).getDataRegs());
                }
            }
            registers = builder.build();
        }
        return registers;
    }

    public ImmutableMap<Register, ImmutableList<Event>> getRegWriterMap(){
        if(regWriterMap == null){
            Map<Register, List<Event>> regEventMap = new HashMap<>();
            List<Event> regWriters = getEvents(FilterBasic.get(EType.REG_WRITER));
            for (Event e : regWriters) {
                Register register = ((RegWriter) e).getResultRegister();
                regEventMap.computeIfAbsent(register, key -> new ArrayList<>(regWriters.size())).add(e);
            }

            ImmutableMap.Builder<Register, ImmutableList<Event>> builder = new ImmutableMap.Builder<>();
            for (Register register : regEventMap.keySet()) {
                List<Event> list = regEventMap.get(register);
                Collections.sort(list);
                builder.put(register, ImmutableList.copyOf(list));
            }

            regWriterMap = builder.build();
        }
        return regWriterMap;
    }
}