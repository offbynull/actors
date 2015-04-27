package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.CoroutineRunner;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

public final class SubcoroutineRouter {

    private final String sourceId;
    private final Context context;
    private final Map<String, CoroutineRunner> suffixMap;
    private final Controller controller;

    public SubcoroutineRouter(String sourceId, Context context) {
        Validate.notNull(sourceId);
        Validate.notNull(context);
        this.sourceId = sourceId;
        this.context = context;
        suffixMap = new HashMap<>();
        controller = new Controller();
    }

    public Controller getController() {
        return controller;
    }

    public boolean forward() throws Exception {
        String id = AddressUtils.relativize(context.getSelf(), context.getDestination());
        if (!AddressUtils.isPrefix(sourceId, id)) {
            return false;
        }
        String innerId = AddressUtils.relativize(sourceId, id);
        
        String key = AddressUtils.getFirstAddressElement(innerId);
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

    public String getSourceId() {
        return sourceId;
    }
    
    public final class Controller {
        public void add(Subcoroutine subcoroutine, AddBehaviour addBehaviour) throws Exception {
            Validate.notNull(subcoroutine);
            Validate.notNull(addBehaviour);

            String suffix = AddressUtils.relativize(sourceId, subcoroutine.getSourceId());
            Validate.isTrue(AddressUtils.getIdElementSize(suffix) == 1);

            CoroutineRunner newRunner = new CoroutineRunner(x -> subcoroutine.run(x));
            newRunner.setContext(context);
            CoroutineRunner existing = suffixMap.putIfAbsent(suffix, newRunner);
            Validate.isTrue(existing == null);

            switch (addBehaviour) {
                case ADD:
                    break;
                case ADD_PRIME:
                    forceForward(suffix, false);
                    break;
                case ADD_PRIME_NO_FINISH:
                    forceForward(suffix, true);
                    break;
            }
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
        
        private boolean forceForward(String suffix, boolean mustNotFinish) throws Exception {
            Validate.notNull(suffix);

            CoroutineRunner runner = suffixMap.get(suffix);

            boolean forwarded = false;
            if (runner != null) {
                boolean running = runner.execute();
                if (!running) {
                    Validate.validState(!mustNotFinish, "Runner pointed to by suffix was not supposed to finish");
                    suffixMap.remove(suffix);
                }
                forwarded = true;
            }

            return forwarded;
        }
    }
    
    public enum AddBehaviour {
        ADD,
        ADD_PRIME,
        ADD_PRIME_NO_FINISH;
    }
}
