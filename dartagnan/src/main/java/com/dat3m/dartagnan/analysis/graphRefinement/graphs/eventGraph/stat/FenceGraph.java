package com.dat3m.dartagnan.analysis.graphRefinement.graphs.eventGraph.stat;

import com.dat3m.dartagnan.analysis.graphRefinement.util.EdgeDirection;
import com.dat3m.dartagnan.program.Thread;
import com.dat3m.dartagnan.verification.model.Edge;
import com.dat3m.dartagnan.verification.model.EventData;
import com.dat3m.dartagnan.verification.model.ExecutionModel;

import java.util.*;
import java.util.stream.Stream;

public class FenceGraph extends StaticEventGraph {

    private final String fenceName;
    private Map<Thread, List<EventData>> threadFencesMap;

    public FenceGraph(String fenceName) {
        this.fenceName = fenceName;
    }

    @Override
    public boolean contains(EventData a, EventData b) {
        if (a.getThread() != b.getThread() || a.getLocalId() >= b.getLocalId())
            return false;

        //TODO: We might want to employ binary search instead of linear search
        return threadFencesMap.get(a.getThread()).stream()
                .anyMatch(fence -> a.getLocalId() < fence.getLocalId() && fence.getLocalId() < b.getLocalId());
    }

    @Override
    public void constructFromModel(ExecutionModel model) {
        super.constructFromModel(model);

        threadFencesMap = new HashMap<>();
        for (Thread t : model.getThreads()) {
            threadFencesMap.put(t, new ArrayList<>());
        }
        Set<EventData> fenceEvents = model.getFenceMap().get(fenceName);
        if (fenceEvents == null) {
            return;
        }

        for (EventData fence : fenceEvents) {
            threadFencesMap.get(fence.getThread()).add(fence);
        }
        for (List<EventData> fenceList : threadFencesMap.values()) {
            fenceList.sort(Comparator.comparingInt(EventData::getLocalId));
        }
        super.autoComputeSize();
    }

    @Override
    public Stream<Edge> edgeStream() {
        return model.getThreadEventsMap().entrySet().stream()
                .map(x ->  {
                    List<EventData> fences = threadFencesMap.get(x.getKey());
                    if (fences.isEmpty()) {
                        return Collections.<EventData>emptyList();
                    }
                    int lastId = fences.get(fences.size() - 1).getLocalId();
                    return x.getValue().subList(0, lastId);
                })
                .flatMap(Collection::stream)
                .flatMap(x -> edgeStream(x, EdgeDirection.OUTGOING));
    }

    @Override
    public Stream<Edge> edgeStream(EventData e, EdgeDirection dir) {
        List<EventData> threadEvents = model.getThreadEventsMap().get(e.getThread());
        if (dir == EdgeDirection.OUTGOING) {
            EventData fence = getNextFence(e);
            return fence == null ? Stream.empty() :
                    threadEvents.subList(fence.getLocalId() + 1, threadEvents.size())
                            .stream().map(x -> new Edge(e, x));
        } else {
            EventData fence = getPreviousFence(e);
            return fence == null ? Stream.empty() :
                    threadEvents.subList(0, fence.getLocalId()).stream().map(x -> new Edge(x, e));
        }
    }
    @Override
    public int getMinSize(EventData e, EdgeDirection dir) {
        int size = 0;
        if (dir == EdgeDirection.OUTGOING) {
            EventData closestFence = getNextFence(e);
            size = closestFence == null ? 0 :
                    model.getThreadEventsMap().get(e.getThread()).size() - (closestFence.getLocalId() + 1);
        } else if (dir == EdgeDirection.INGOING) {
            EventData closestFence = getPreviousFence(e);
            size = closestFence == null ? 0 : closestFence.getLocalId();
        }
        return size;
    }

    public EventData getNextFence(EventData e) {
        List<EventData> fences = threadFencesMap.get(e.getThread());
        if (fences.isEmpty()) {
            return null;
        }

        EventData closestFence = null;
        for (int i = fences.size() - 1; i >= 0; i--) {
            EventData fence = fences.get(i);
            if (fence.getLocalId() > e.getLocalId()) {
                closestFence = fence;
            } else {
                break;
            }
        }
        return closestFence;
    }

    public EventData getPreviousFence(EventData e) {
        List<EventData> fences = threadFencesMap.get(e.getThread());
        if (fences.isEmpty()) {
            return null;
        }


        EventData closestFence = null;
        for (EventData fence : fences) {
            if (fence.getLocalId() < e.getLocalId()) {
                closestFence = fence;
            } else {
                break;
            }
        }
        return closestFence;
    }

}