package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.unstructured.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryResponse;
import java.time.Duration;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class OutgoingQuerySubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final Address timerAddress;
    private final Address logAddress;
    private final State state;

    public OutgoingQuerySubcoroutine(Address subAddress, State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        this.subAddress = subAddress;
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.state = state;
    }

    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        while (true) {
            new SleepSubcoroutine.Builder()
                    .address(subAddress.appendSuffix(state.nextRandomId()))
                    .timerAddress(timerAddress)
                    .duration(Duration.ofSeconds(1L))
                    .build()
                    .run(cnt);

            if (!state.hasMoreCachedAddresses()) {
                ctx.addOutgoingMessage(subAddress, logAddress, warn("No further cached addresses are available."));
                continue;
            }

            String linkId = state.getNextCachedLinkId();
            Address destination = state.getAddressTransformer().linkIdToRemoteAddress(linkId);
            ctx.addOutgoingMessage(subAddress, logAddress, info("Querying {}", linkId));

            QueryRequest request = new QueryRequest();
            RequestSubcoroutine<QueryResponse> requestSubcoroutine = new RequestSubcoroutine.Builder<QueryResponse>()
                    .address(subAddress.appendSuffix(state.nextRandomId()))
                    .request(request)
                    .timerAddress(timerAddress)
                    .destinationAddress(destination.appendSuffix(ROUTER_HANDLER_RELATIVE_ADDRESS))
                    .throwExceptionIfNoResponse(false)
                    .addExpectedResponseType(QueryResponse.class)
                    .build();
            QueryResponse response = requestSubcoroutine.run(cnt);

            if (response == null) {
                ctx.addOutgoingMessage(subAddress, logAddress, info("{} did not respond to query", linkId));
                continue;
            }

            ctx.addOutgoingMessage(subAddress, logAddress, info("{} responded to query with {}", linkId, response.getLinkIds()));
            Set<String> newLinkIds = response.getLinkIds();
            state.addCachedLinkIds(newLinkIds);
        }
    }

}
