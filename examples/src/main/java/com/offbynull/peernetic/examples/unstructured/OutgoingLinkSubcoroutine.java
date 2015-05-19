package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.common.request.ExternalMessageIdGenerator;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkFailedResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkKeepAliveRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkKeptAliveResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkSuccessResponse;
import com.offbynull.peernetic.visualizer.gateways.graph.AddEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleEdge;
import java.util.Random;
import org.apache.commons.lang3.Validate;

final class OutgoingLinkSubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    private final Address graphAddress;
    private final Address timerAddress;
    private final AddressCache addressCache;

    public OutgoingLinkSubcoroutine(Address sourceId, Address graphAddress, Address timerAddress, AddressCache addressCache) {
        Validate.notNull(sourceId);
        Validate.notNull(graphAddress);
        Validate.notNull(timerAddress);
        Validate.notNull(addressCache);
        this.sourceId = sourceId;
        this.graphAddress = graphAddress;
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

        reconnect:
        while (true) {
            Address address = addressCache.next();

            ctx.addOutgoingMessage(graphAddress, new AddEdge(ctx.getSelf().toString(), address.toString()));
            ctx.addOutgoingMessage(graphAddress, new StyleEdge(ctx.getSelf().toString(), address.toString(), "-fx-stroke: yellow"));
            boolean lineIsGreen = false;

            RequestSubcoroutine<Object> linkRequestSubcoroutine = new RequestSubcoroutine.Builder<>()
                    .id(sourceId.appendSuffix("" + gen.generateId()))
                    .request(new LinkRequest())
                    .timerAddressPrefix(timerAddress)
                    .destinationAddress(address.appendSuffix("router", "handler"))
                    .throwExceptionIfNoResponse(false)
                    .addExpectedResponseType(LinkSuccessResponse.class)
                    .addExpectedResponseType(LinkFailedResponse.class)
                    .build();
            Object response = linkRequestSubcoroutine.run(cnt);

            if (response == null || response instanceof LinkFailedResponse) {
                ctx.addOutgoingMessage(graphAddress, new RemoveEdge(ctx.getSelf().toString(), address.toString()));
                continue reconnect;
            }

            LinkSuccessResponse successResponse = (LinkSuccessResponse) response;
            Address dstAddress = successResponse.getAddress();

            connected:
            while (true) {
                RequestSubcoroutine<LinkKeptAliveResponse> keepAliveRequestSubcoroutine
                        = new RequestSubcoroutine.Builder<LinkKeptAliveResponse>()
                        .id(sourceId.appendSuffix("" + gen.generateId()))
                        .request(new LinkKeepAliveRequest())
                        .timerAddressPrefix(timerAddress)
                        .destinationAddress(dstAddress)
                        .throwExceptionIfNoResponse(false)
                        .addExpectedResponseType(LinkKeptAliveResponse.class)
                        .build();

                LinkKeptAliveResponse resp = keepAliveRequestSubcoroutine.run(cnt);
                if (resp != null && !lineIsGreen) {
                    ctx.addOutgoingMessage(graphAddress, new StyleEdge(ctx.getSelf().toString(), address.toString(), "-fx-stroke: green"));
                    lineIsGreen = true;
                } else {
                    ctx.addOutgoingMessage(graphAddress, new RemoveEdge(ctx.getSelf().toString(), address.toString()));
                    continue reconnect;
                }
            }
        }
    }

}
