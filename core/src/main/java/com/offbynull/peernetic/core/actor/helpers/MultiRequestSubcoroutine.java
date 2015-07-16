package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.MultiRequestSubcoroutine.Response;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import com.offbynull.peernetic.core.shuttle.Address;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

public final class MultiRequestSubcoroutine<T> implements Subcoroutine<Response<T>> {
    private final Address id;
    private final Address timerAddressPrefix;
    private final Object request;
    private final int maxAttempts;
    private final Duration attemptInterval;
    private final Set<Class<?>> expectedResponseTypes;
    
    private final List<RequestRecipient> requests;
    private final Map<RequestRecipient, Object> responses;
    
    private MultiRequestSubcoroutine(
            Address id,
            Set<Address> destinationAddresses,
            Address timerAddressPrefix,
            Object request,
            int maxAttempts,
            Duration attemptInterval,
            Set<Class<?>> expectedResponseTypes) {
        Validate.notNull(id);
        Validate.notNull(destinationAddresses);
        Validate.notNull(request);
        Validate.notNull(timerAddressPrefix);
        Validate.notNull(attemptInterval);
        Validate.notNull(expectedResponseTypes);
        Validate.isTrue(!id.isEmpty());
        Validate.noNullElements(destinationAddresses);
        Validate.isTrue(!destinationAddresses.isEmpty());
        destinationAddresses.forEach(x -> Validate.isTrue(!x.isEmpty()));
        Validate.isTrue(!timerAddressPrefix.isEmpty());
        Validate.isTrue(maxAttempts > 0);
        Validate.isTrue(!attemptInterval.isNegative());
        this.id = id;
        this.destinationAddresses = new HashSet<>(destinationAddresses);
        this.request = request;
        this.timerAddressPrefix = timerAddressPrefix;
        this.maxAttempts = maxAttempts;
        this.attemptInterval = attemptInterval;
        this.expectedResponseTypes = new HashSet<>(expectedResponseTypes);
        this.responses = new HashMap<>(destinationAddresses.size());
    }

    @Override
    public Response<T> run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        Address routerAddress = id.appendSuffix("mrsb"); // mrsb = multirequestsubcoroutinerouter
        SubcoroutineRouter router = new SubcoroutineRouter(routerAddress, ctx);
        
        for (Address destinationAddress : destinationAddresses) {
            RequestSubcoroutine<T> requestSubcoroutine = new RequestSubcoroutine.Builder<T>()
                    .address(routerAddress.appendSuffix(state.nextRandomId()))
                    .destinationAddress(destinationAddress)
                    .request(request)
                    .timerAddressPrefix(timerAddressPrefix)
                    .expectedResponseTypes(expectedResponseTypes)
                    .build();
            router.getController().add(requestSubcoroutine, ADD_PRIME_NO_FINISH);
        }
        
        while (true) {
            cnt.suspend();
            router.forward();
        }
    }
    
    @Override
    public Address getAddress() {
        return id;
    }
    
    public static final class RequestRecipient {
        private final String uniqueSourceAddressSuffix;
        private final Address destinationAddress;

        public RequestRecipient(String uniqueSourceAddressSuffix, Address destinationAddress) {
            Validate.notNull(uniqueSourceAddressSuffix);
            Validate.notNull(destinationAddress);
            
            this.uniqueSourceAddressSuffix = uniqueSourceAddressSuffix;
            this.destinationAddress = destinationAddress;
        }

        public String getUniqueSourceAddressSuffix() {
            return uniqueSourceAddressSuffix;
        }

        public Address getDestinationAddress() {
            return destinationAddress;
        }
    }

    public static final class Response<T> {
        private final Address destinationAddress;
        private final T response;

        public Response(Address destinationAddress, T response) {
            Validate.notNull(destinationAddress);
            Validate.notNull(response);
            
            this.destinationAddress = destinationAddress;
            this.response = response;
        }

        public Address getDestinationAddress() {
            return destinationAddress;
        }

        public T getResponse() {
            return response;
        }
        
    }
    
}
