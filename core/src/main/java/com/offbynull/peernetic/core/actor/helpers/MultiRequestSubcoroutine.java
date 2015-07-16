package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.Response;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.ForwardResult;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class MultiRequestSubcoroutine<T> implements Subcoroutine<List<Response<T>>> {
    private final Address id;
    private final Address timerAddressPrefix;
    private final Object request;
    private final int maxAttempts;
    private final Duration attemptInterval;
    private final Set<Class<?>> expectedResponseTypes;
    
    private final Map<String, Address> destinations; // key = unique source address suffix, value = destination address
    private final Map<String, Object> responses; // key = unique source address suffix, value = response
    
    private MultiRequestSubcoroutine(
            Address id,
            Map<String, Address> destinations,
            Address timerAddressPrefix,
            Object request,
            int maxAttempts,
            Duration attemptInterval,
            Set<Class<?>> expectedResponseTypes) {
        Validate.notNull(id);
        Validate.notNull(destinations);
        Validate.notNull(request);
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(attemptInterval);
        Validate.notNull(expectedResponseTypes);
        Validate.isTrue(!id.isEmpty());
        Validate.isTrue(!destinations.isEmpty());
        destinations.entrySet().forEach(x -> {
            Validate.notNull(x.getKey());
            Validate.notNull(x.getValue());
        });
        Validate.isTrue(!timerAddressPrefix.isEmpty());
        Validate.isTrue(maxAttempts > 0);
        Validate.isTrue(!attemptInterval.isNegative());
        this.id = id;
        this.destinations = new HashMap<>(destinations);
        this.request = request;
        this.timerAddressPrefix = timerAddressPrefix;
        this.maxAttempts = maxAttempts;
        this.attemptInterval = attemptInterval;
        this.expectedResponseTypes = new HashSet<>(expectedResponseTypes);
        this.responses = new HashMap<>(destinations.size());
    }

    @Override
    public List<Response<T>> run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Address routerAddress = id.appendSuffix("mrsb"); // mrsb = multirequestsubcoroutinerouter
        SubcoroutineRouter router = new SubcoroutineRouter(routerAddress, ctx);
        
        RequestSubcoroutine.Builder<T> baseRequestBuilder = new RequestSubcoroutine.Builder<T>()
                    .request(request)
                    .timerAddressPrefix(timerAddressPrefix)
                    .expectedResponseTypes(expectedResponseTypes);
        
        for (Entry<String, Address> destinationEntry : destinations.entrySet()) {
            RequestSubcoroutine<T> requestSubcoroutine = baseRequestBuilder
                    .address(routerAddress.appendSuffix(destinationEntry.getKey()))
                    .destinationAddress(destinationEntry.getValue())
                    .build();
            router.getController().add(requestSubcoroutine, ADD_PRIME_NO_FINISH);
        }
        
        List<Response<T>> ret = new ArrayList<>(destinations.size());
        while (true) {
            cnt.suspend();
            
//            ForwardResult fr = router.forward();
//            
//            if (fr.isForwarded() && fr.isCompleted()) { // calling isCompleted by itself may throw an exception, check isForwarded first
//                Address from = fr.getSubcoroutine().getAddress();
//                Object result = fr.getResult();
//                
//                Response<T> response = new Response<>(uniqueSourceAddressSuffix, from, result);
//                ret.add(response);
//            }
//            
//            if (router.getController().size() == 0) {
//                break;
//            }
        }
        
//        return ret;
    }
    
    @Override
    public Address getAddress() {
        return id;
    }

    public static final class Response<T> {
        private final String uniqueSourceAddressSuffix;
        private final Address destinationAddress;
        private final T response;

        public Response(String uniqueSourceAddressSuffix, Address destinationAddress, T response) {
            Validate.notNull(uniqueSourceAddressSuffix);
            Validate.notNull(destinationAddress);
            Validate.notNull(response);
            
            this.uniqueSourceAddressSuffix = uniqueSourceAddressSuffix;
            this.destinationAddress = destinationAddress;
            this.response = response;
        }

        public String getUniqueSourceAddressSuffix() {
            return uniqueSourceAddressSuffix;
        }

        public Address getDestinationAddress() {
            return destinationAddress;
        }

        public T getResponse() {
            return response;
        }
        
    }
    
}
