package com.dat3m.dartagnan.solver.caat4wmm.statistics;

import com.dat3m.dartagnan.program.event.core.Event;

import java.util.List;

public class ReasonElement {
    private String name;
    private List<Event> events;

    public ReasonElement(String name, List<Event> events) {
        this.name = name;
        this.events = events;
    }

    public String getName() {
        return name;
    }

    public List<Event> getEvents() {
        return events;
    }
}
