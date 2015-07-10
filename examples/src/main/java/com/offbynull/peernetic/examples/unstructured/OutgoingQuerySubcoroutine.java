package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.unstructured.AddressConstants.HANDLER_ADDRESS_SUFFIX;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryResponse;
import java.time.Duration;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class OutgoingQuerySubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    private final Address timerAddress;
    private final Address logAddress;
    private final State state;

    public OutgoingQuerySubcoroutine(Address sourceId, Address timerAddress, Address logAddress, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.timerAddress = timerAddress;
        this.logAddress = logAddress;
        this.state = state;
    }

    @Override
    public Address getId() {
        return sourceId;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        while (true) {
            new SleepSubcoroutine.Builder()
                    .id(sourceId.appendSuffix(state.nextRandomId()))
                    .timerAddressPrefix(timerAddress)
                    .duration(Duration.ofSeconds(1L))
                    .build()
                    .run(cnt);

            if (!state.hasMoreCachedAddresses()) {
                ctx.addOutgoingMessage(sourceId, logAddress, warn("No further cached addresses are available."));
                continue;
            }

            String linkId = state.getNextCachedLinkId();
            Address destination = state.getAddressTransformer().linkIdToRemoteAddress(linkId);
            ctx.addOutgoingMessage(sourceId, logAddress, info("Querying {}", linkId));

            QueryRequest request = new QueryRequest();
            RequestSubcoroutine<QueryResponse> requestSubcoroutine = new RequestSubcoroutine.Builder<QueryResponse>()
                    .id(sourceId.appendSuffix(state.nextRandomId()))
                    .request(request)
                    .timerAddressPrefix(timerAddress)
                    .destinationAddress(destination.appendSuffix(HANDLER_ADDRESS_SUFFIX))
                    .throwExceptionIfNoResponse(false)
                    .addExpectedResponseType(QueryResponse.class)
                    .build();
            QueryResponse response = requestSubcoroutine.run(cnt);

            if (response == null) {
                ctx.addOutgoingMessage(sourceId, logAddress, info("{} did not respond to query", linkId));
                continue;
            }

            ctx.addOutgoingMessage(sourceId, logAddress, info("{} responded to query with {}", linkId, response.getLinkIds()));
            Set<String> newLinkIds = response.getLinkIds();
            state.addCachedLinkIds(newLinkIds);
        }
    }

}
