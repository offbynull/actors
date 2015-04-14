package com.offbynull.peernetic.examples.common.coroutines;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.apache.commons.lang3.Validate;

public final class ParentCoroutine implements Coroutine {

    private final String prefix;
    private final String timerAddressPrefix;
    private final Context context;
    private final Map<String, Coroutine> suffixMap;

    public ParentCoroutine(String prefix, String timerAddressPrefix, Context context) {
        Validate.notNull(prefix);
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(context);
        this.prefix = prefix;
        this.timerAddressPrefix = timerAddressPrefix;
        this.context = context;
        suffixMap = new HashMap<>();
    }

    public void add(String suffix, Coroutine coroutine) {
        Validate.notNull(suffix);
        Validate.notNull(coroutine);
        Validate.isTrue(AddressUtils.getIdElementSize(suffix) == 1);

        Coroutine existing = suffixMap.putIfAbsent(suffix, coroutine);
        Validate.isTrue(existing == null);
    }

    public RequestCoroutine addSendRequest(String destinationAddress, ExternalMessage request, Duration timeoutDuration,
            Class<? extends ExternalMessage> expectedResponseType) {
        String suffix = "" + request.getId();
        RequestCoroutine coroutine = new RequestCoroutine(prefix, destinationAddress, request, timerAddressPrefix, timeoutDuration,
                expectedResponseType);
        add(suffix, coroutine);
        return coroutine;
    }

    public SleepCoroutine addSleep(Duration timeoutDuration) {
        SleepCoroutine coroutine = new SleepCoroutine(timerAddressPrefix, timeoutDuration);
        add("", coroutine);
        return coroutine;
    }

    public void remove(String suffix) {
        Validate.notNull(suffix);
        Validate.isTrue(AddressUtils.getIdElementSize(suffix) == 1);
        Coroutine old = suffixMap.remove(suffix);
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

    @Override
    public void run(Continuation cnt) throws Exception {
        Validate.notNull(cnt);
        while (true) {
            String id = AddressUtils.relativize(prefix, context.getSource());

            String key = AddressUtils.getFirstAddressElement(id);
            Coroutine coroutine = suffixMap.get(key);

            if (coroutine != null) {
                coroutine.run(cnt);
            }
            
            if (!isEmpty()) {
                cnt.suspend();
            }
        }
    }
}
