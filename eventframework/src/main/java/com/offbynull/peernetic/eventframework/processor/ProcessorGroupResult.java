package com.offbynull.peernetic.eventframework.processor;

import com.offbynull.peernetic.eventframework.event.OutgoingEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class ProcessorGroupResult<K> {
    private Map<K, ProcessResult> idMap;
    private Map<Class<? extends ProcessResult>, Map<K, ? extends ProcessResult>>
            typeIdMap;
    private Set<K> idSet;

    public ProcessorGroupResult(Map<K, ProcessResult> resultMap) {
        if (resultMap == null) {
            throw new NullPointerException();
        }
        
        idSet = new HashSet<>();
        idMap = new HashMap<>();
        typeIdMap = new HashMap<>();
        
        for (Entry<K, ProcessResult> entry : resultMap.entrySet()) {
            K key = entry.getKey();
            ProcessResult res = entry.getValue();
            
            if (key == null || res == null) {
                throw new NullPointerException();
            }

            if (!(res instanceof FinishedProcessResult)
                    && !(res instanceof OngoingProcessResult)) {
                throw new IllegalArgumentException();
            }

            typeIdMap.put(FinishedProcessResult.class,
                    new HashMap<K, ProcessResult>());
            typeIdMap.put(OngoingProcessResult.class,
                    new HashMap<K, ProcessResult>());
            
            idMap.put(entry.getKey(), entry.getValue());
            
            Class<? extends ProcessResult> resultCls =
                    entry.getValue().getClass();
            
            Map<K, ProcessResult> idMapForType =
                    (Map<K, ProcessResult>) typeIdMap.get(resultCls);
            idMapForType.put(entry.getKey(), entry.getValue());
        }
        
        idSet.addAll(typeIdMap.get(FinishedProcessResult.class).keySet());
    }

    public Map<K, FinishedProcessResult> viewFinishedProcessorResults() {
        return Collections.unmodifiableMap((Map<K, FinishedProcessResult>)
                typeIdMap.get(FinishedProcessResult.class));
    }

    public Map<K, OngoingProcessResult> viewOngoingProcessorResults() {
        return Collections.unmodifiableMap((Map<K, OngoingProcessResult>)
                typeIdMap.get(OngoingProcessResult.class));
    }

    public Map<K, ProcessResult> viewProcessorResults() {
        return Collections.unmodifiableMap(idMap);
    }
    
    public Set<K> viewRemovedProcessors() {
        return Collections.unmodifiableSet(idSet);
    }

    public List<OutgoingEvent> gatherOutgoingEvents() {
        List<OutgoingEvent> ret = new ArrayList<>();
        for (Map<K, ? extends ProcessResult> results : typeIdMap.values()) {
            for (ProcessResult result : (Collection<ProcessResult>) results) {
                ret.addAll(result.viewOutgoingEvents());
            }
        }
        return ret;
    }

    public boolean isFinished() {
        return typeIdMap.get(OngoingProcessResult.class).isEmpty();
    }
}
