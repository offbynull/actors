package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.common.request.ExternalMessageIdGenerator;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkFailedResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryResponse;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class OutgoingQuerySubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    private final Address timerAddress;
    private final AddressCache addressCache;

    public OutgoingQuerySubcoroutine(Address sourceId, Address timerAddress, AddressCache addressCache) {
        Validate.notNull(sourceId);
        Validate.notNull(timerAddress);
        Validate.notNull(addressCache);
        this.sourceId = sourceId;
        this.timerAddress = timerAddress;
        this.addressCache = addressCache;
    }

    @Override
    public Address getId() {
        return sourceId;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        ExternalMessageIdGenerator gen = new ExternalMessageIdGenerator(new Random());

        while (true) {
            Address address = addressCache.next();

            QueryRequest request = new QueryRequest();
            RequestSubcoroutine<QueryResponse> requestSubcoroutine = new RequestSubcoroutine.Builder<QueryResponse>()
                    .id(sourceId.appendSuffix("" + gen.generateId()))
                    .request(request)
                    .timerAddressPrefix(timerAddress)
                    .destinationAddress(address.appendSuffix("router", "handler"))
                    .throwExceptionIfNoResponse(false)
                    .addExpectedResponseType(LinkFailedResponse.class)
                    .build();
            QueryResponse response = requestSubcoroutine.run(cnt);
            
            addressCache.addAll(response.getLinks());
        }
    }

}
