package com.offbynull.peernetic.examples.common.coroutines;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

public final class ParentCoroutine {

    private final String prefix;
    private final Context context;
    private final Map<String, CoroutineRunner> suffixMap;

    public ParentCoroutine(String prefix, Context context) {
        Validate.notNull(prefix);
        Validate.notNull(context);
        this.prefix = prefix;
        this.context = context;
        suffixMap = new HashMap<>();
    }

    public void add(String suffix, Coroutine coroutine) {
        Validate.notNull(suffix);
        Validate.notNull(coroutine);
        Validate.isTrue(AddressUtils.getIdElementSize(suffix) == 1);

        CoroutineRunner newRunner = new CoroutineRunner(coroutine);
        newRunner.setContext(context);
        CoroutineRunner existing = suffixMap.putIfAbsent(suffix, newRunner);
        Validate.isTrue(existing == null);
    }

    public void remove(String suffix) {
        Validate.notNull(suffix);
        Validate.isTrue(AddressUtils.getIdElementSize(suffix) == 1);
        CoroutineRunner old = suffixMap.remove(suffix);
        Validate.isTrue(old == null);
    }

    public int size() {
        return suffixMap.size();
    }

    public boolean isEmpty() {
        return suffixMap.isEmpty();
    }

    public boolean contains(String suffix) {
        Validate.notNull(suffix);
        Validate.isTrue(AddressUtils.getIdElementSize(suffix) == 1);
        return suffixMap.containsKey(suffix);
    }

    public void runUntilFinished(Continuation cnt) throws Exception {
        Validate.notNull(cnt);
        while (true) {
            String id = AddressUtils.relativize(prefix, context.getSource());

            String key = AddressUtils.getFirstAddressElement(id);
            CoroutineRunner runner = suffixMap.get(key);

            if (runner != null) {
                boolean running = runner.execute();
                if (!running) {
                    suffixMap.remove(key);
                }
            }
            
            if (suffixMap.isEmpty()) {
                return;
            }
            
            cnt.suspend();
        }
    }

    public boolean forward() throws Exception {
        String id = AddressUtils.relativize(prefix, context.getSource());

        String key = AddressUtils.getFirstAddressElement(id);
        CoroutineRunner runner = suffixMap.get(key);

        boolean forwarded = false;
        if (runner != null) {
            boolean running = runner.execute();
            if (!running) {
                suffixMap.remove(key);
            }
            forwarded = true;
        }
        
        return forwarded;
    }

    public boolean forceForward(String suffix, boolean mustNotFinish) throws Exception {
        Validate.notNull(suffix);
        
        CoroutineRunner runner = suffixMap.get(suffix);

        boolean forwarded = false;
        if (runner != null) {
            boolean running = runner.execute();
            if (!running) {
                Validate.validState(!mustNotFinish, "Runner pointed to by suffix was not suppose to finish");
                suffixMap.remove(suffix);
            }
            forwarded = true;
        }
        
        return forwarded;
    }
}
