package com.offbynull.peernetic.eventframework.processor;

import com.offbynull.peernetic.eventframework.handler.IncomingEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ProcessorGroup<K> {

    private LinkedHashMap<K, Processor> map;
    private boolean ignoreExceptions;

    public ProcessorGroup() {
        this(false);
    }

    public ProcessorGroup(boolean ignoreExceptions) {
        map = new LinkedHashMap<>();
        this.ignoreExceptions = ignoreExceptions;
    }

    public void setIgnoreExceptions(boolean ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
    }

    public void add(K id, Processor processor) {
        if (id == null || processor == null) {
            throw new NullPointerException();
        }

        if (map.containsKey(id)) {
            throw new IllegalArgumentException();
        }

        map.put(id, processor);
    }

    public void remove(K id) {
        if (id == null) {
            throw new NullPointerException();
        }
        
        map.remove(id);
    }

    public ProcessorGroupResult<K> process(long timestamp, IncomingEvent event)
            throws Exception {
        Map<K, ProcessResult> processResultMap = new HashMap<>();

        Iterator<Entry<K, Processor>> it = map.entrySet().iterator();
        
        while (it.hasNext()) {
            Entry<K, Processor> entry = it.next();
            
            K key = entry.getKey();
            Processor processor = entry.getValue();

            ProcessResult result;
            try {
                result = processor.process(timestamp, event);
            } catch (Exception e) {
                if (!ignoreExceptions) {
                    throw e;
                }
                continue;
            }

            if (result instanceof OngoingProcessResult) {
                // do nothing
            } else if (result instanceof FinishedProcessResult) {
                it.remove();
            } else {
                throw new IllegalArgumentException();
            }

            processResultMap.put(key, result);
        }

        return new ProcessorGroupResult<>(processResultMap);
    }
}
