package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkFailedResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkKeepAliveRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkKeptAliveResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkSuccessResponse;
import com.offbynull.peernetic.visualizer.gateways.graph.AddEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleEdge;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

final class OutgoingLinkSubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    private final Address graphAddress;
    private final Address timerAddress;
    private final Address logAddress;
    private final State state;

    public OutgoingLinkSubcoroutine(
            Address sourceId,
            Address graphAddress,
            Address timerAddress,
            Address logAddress,
            State state) {
        Validate.notNull(sourceId);
        Validate.notNull(graphAddress);
        Validate.notNull(timerAddress);
        Validate.notNull(logAddress);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.graphAddress = graphAddress;
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
        
        reconnect:
        while (true) {
            new SleepSubcoroutine.Builder()
                    .id(sourceId.appendSuffix(state.nextRandomId()))
                    .timerAddressPrefix(timerAddress)
                    .duration(Duration.ofSeconds(1L))
                    .build()
                    .run(cnt);
            
            if (!state.hasMoreCachedAddresses()) {
                ctx.addOutgoingMessage(sourceId, logAddress, warn("No further cached addresses are available"));
                continue;
            }
            
            Address address = state.getNextCachedAddress();
            
            // make sure address we're connecting to isn't an already we're already connected to
            for (Address link : state.getLinks()) {
                if (address.isPrefixOf(link)) {
                    ctx.addOutgoingMessage(sourceId, logAddress,
                            warn("Rejecting to link to {} (already linked), trying again", address));
                    continue reconnect;
                }
            }
            
            // make sure address we're conencting to isn't an already we're already CONNECTING TO (not connected to, but connecting to)
            for (Address link : state.getPendingOutgoingLinks()) {
                if (address.isPrefixOf(link)) {
                    ctx.addOutgoingMessage(sourceId, logAddress,
                            warn("Rejecting to link to {} (already attempting linking), trying again", address));
                    continue reconnect;
                }
            }

            ctx.addOutgoingMessage(sourceId, logAddress, info("Linking to {}", address));
            ctx.addOutgoingMessage(graphAddress,
                    new AddEdge(
                            state.getAddressTransformer().selfAddressToLinkId(ctx.getSelf()),
                            state.getAddressTransformer().remoteAddressToLinkId(address)
                    )
            );
            ctx.addOutgoingMessage(graphAddress,
                    new StyleEdge(
                            state.getAddressTransformer().selfAddressToLinkId(ctx.getSelf()),
                            state.getAddressTransformer().remoteAddressToLinkId(address),
                            "-fx-stroke: yellow"
                    )
            );
            boolean lineIsGreen = false;

            state.addPendingOutgoingLink(address);
            
            RequestSubcoroutine<Object> linkRequestSubcoroutine = new RequestSubcoroutine.Builder<>()
                    .id(sourceId.appendSuffix(state.nextRandomId()))
                    .request(new LinkRequest())
                    .timerAddressPrefix(timerAddress)
                    .destinationAddress(address.appendSuffix("router", "handler"))
                    .throwExceptionIfNoResponse(false)
                    .addExpectedResponseType(LinkSuccessResponse.class)
                    .addExpectedResponseType(LinkFailedResponse.class)
                    .build();
            Object response = linkRequestSubcoroutine.run(cnt);

            if (response == null) {
                state.removePendingOutgoingLink(address);
                ctx.addOutgoingMessage(sourceId, logAddress, info("{} did not respond to link", address));
                ctx.addOutgoingMessage(graphAddress,
                        new RemoveEdge(
                            state.getAddressTransformer().selfAddressToLinkId(ctx.getSelf()),
                            state.getAddressTransformer().remoteAddressToLinkId(address)
                        )
                );
                continue reconnect;
            } else if (response instanceof LinkFailedResponse) {
                state.removePendingOutgoingLink(address);
                ctx.addOutgoingMessage(sourceId, logAddress, info("{} responded with link failure", address));
                ctx.addOutgoingMessage(graphAddress,
                        new RemoveEdge(
                            state.getAddressTransformer().selfAddressToLinkId(ctx.getSelf()),
                            state.getAddressTransformer().remoteAddressToLinkId(address)
                        )
                );
                continue reconnect;
            }
            
            LinkSuccessResponse successResponse = (LinkSuccessResponse) response;
            Address dstAddress = address.appendSuffix(successResponse.getId());
            state.addOutgoingLink(dstAddress);

            connected:
            while (true) {
                ctx.addOutgoingMessage(sourceId, logAddress, info("Waiting to refresh link to {}", address));
                new SleepSubcoroutine.Builder()
                        .id(sourceId.appendSuffix(state.nextRandomId()))
                        .timerAddressPrefix(timerAddress)
                        .duration(Duration.ofSeconds(1L))
                        .build()
                        .run(cnt);
                
                ctx.addOutgoingMessage(sourceId, logAddress, info("Refreshing link to {}", address));
                
                RequestSubcoroutine<LinkKeptAliveResponse> keepAliveRequestSubcoroutine
                        = new RequestSubcoroutine.Builder<LinkKeptAliveResponse>()
                        .id(sourceId.appendSuffix(state.nextRandomId()))
                        .request(new LinkKeepAliveRequest())
                        .timerAddressPrefix(timerAddress)
                        .destinationAddress(dstAddress)
                        .throwExceptionIfNoResponse(false)
                        .addExpectedResponseType(LinkKeptAliveResponse.class)
                        .build();
                LinkKeptAliveResponse resp = keepAliveRequestSubcoroutine.run(cnt);
                
                if (resp == null) {
                    ctx.addOutgoingMessage(sourceId, logAddress, info("{} did not respond to link refresh", address));
                    ctx.addOutgoingMessage(graphAddress,
                            new RemoveEdge(
                                    state.getAddressTransformer().selfAddressToLinkId(ctx.getSelf()),
                                    state.getAddressTransformer().remoteAddressToLinkId(address)
                            )
                    );
                    state.removeOutgoingLink(dstAddress);
                    continue reconnect;
                }
                
                ctx.addOutgoingMessage(sourceId, logAddress, info("{} responded to link refresh", address));
                    
                if (!lineIsGreen) {
                    ctx.addOutgoingMessage(graphAddress,
                            new StyleEdge(
                                    state.getAddressTransformer().selfAddressToLinkId(ctx.getSelf()),
                                    state.getAddressTransformer().remoteAddressToLinkId(address),
                                    "-fx-stroke: green"
                            )
                    );
                    lineIsGreen = true;
                }
            }
        }
    }

}
