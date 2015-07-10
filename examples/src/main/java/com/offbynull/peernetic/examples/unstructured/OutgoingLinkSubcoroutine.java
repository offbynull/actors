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
            
            String selfLinkId = state.getAddressTransformer().selfAddressToLinkId(ctx.getSelf());
            String outLinkId = state.getNextCachedLinkId();
            
            // make sure address we're connecting to isn't an already we're already connected to
            if (state.getLinks().contains(outLinkId)) {
                ctx.addOutgoingMessage(sourceId, logAddress, warn("Rejecting to link to {} (already linked), trying again", outLinkId));
                continue;
            }
            
            // make sure address we're conencting to isn't an already we're already CONNECTING TO (not connected to, but connecting to)
            if (state.getPendingOutgoingLinks().contains(outLinkId)) {
                ctx.addOutgoingMessage(sourceId, logAddress,
                        warn("Rejecting to link to {} (already attempting linking), trying again", outLinkId));
                continue;
            }

            ctx.addOutgoingMessage(sourceId, logAddress, info("Linking to {}", outLinkId));
            ctx.addOutgoingMessage(graphAddress, new AddEdge(selfLinkId, outLinkId));
            ctx.addOutgoingMessage(graphAddress, new StyleEdge(selfLinkId, outLinkId, "-fx-stroke: yellow"));
            boolean lineIsGreen = false;

            state.addPendingOutgoingLink(outLinkId);
            
            Address baseAddr = state.getAddressTransformer().linkIdToRemoteAddress(outLinkId);
            
            RequestSubcoroutine<Object> linkRequestSubcoroutine = new RequestSubcoroutine.Builder<>()
                    .id(sourceId.appendSuffix(state.nextRandomId()))
                    .request(new LinkRequest())
                    .timerAddressPrefix(timerAddress)
                    .destinationAddress(baseAddr.appendSuffix(HANDLER_ADDRESS_SUFFIX))
                    .throwExceptionIfNoResponse(false)
                    .addExpectedResponseType(LinkSuccessResponse.class)
                    .addExpectedResponseType(LinkFailedResponse.class)
                    .build();
            Object response = linkRequestSubcoroutine.run(cnt);

            if (response == null) {
                state.removePendingOutgoingLink(outLinkId);
                ctx.addOutgoingMessage(sourceId, logAddress, info("{} did not respond to link", outLinkId));
                ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLinkId, outLinkId));
                continue;
            } else if (response instanceof LinkFailedResponse) {
                state.removePendingOutgoingLink(outLinkId);
                ctx.addOutgoingMessage(sourceId, logAddress, info("{} responded with link failure", outLinkId));
                ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLinkId, outLinkId));
                continue;
            }
            
            LinkSuccessResponse successResponse = (LinkSuccessResponse) response;
            Address suffix = successResponse.getSuffix();
            state.addOutgoingLink(outLinkId, suffix);
            
            Address updateAddr = baseAddr.appendSuffix(suffix);

            connected:
            while (true) {
                ctx.addOutgoingMessage(sourceId, logAddress, info("Waiting to refresh link to {}", outLinkId));
                new SleepSubcoroutine.Builder()
                        .id(sourceId.appendSuffix(state.nextRandomId()))
                        .timerAddressPrefix(timerAddress)
                        .duration(Duration.ofSeconds(1L))
                        .build()
                        .run(cnt);
                
                ctx.addOutgoingMessage(sourceId, logAddress, info("Refreshing link to {}", outLinkId));
                
                RequestSubcoroutine<LinkKeptAliveResponse> keepAliveRequestSubcoroutine
                        = new RequestSubcoroutine.Builder<LinkKeptAliveResponse>()
                        .id(sourceId.appendSuffix(state.nextRandomId()))
                        .request(new LinkKeepAliveRequest())
                        .timerAddressPrefix(timerAddress)
                        .destinationAddress(updateAddr)
                        .throwExceptionIfNoResponse(false)
                        .addExpectedResponseType(LinkKeptAliveResponse.class)
                        .build();
                LinkKeptAliveResponse resp = keepAliveRequestSubcoroutine.run(cnt);
                
                if (resp == null) {
                    ctx.addOutgoingMessage(sourceId, logAddress, info("{} did not respond to link refresh", outLinkId));
                    ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLinkId, outLinkId));
                    state.removeOutgoingLink(outLinkId);
                    continue reconnect;
                }
                
                ctx.addOutgoingMessage(sourceId, logAddress, info("{} responded to link refresh", outLinkId));
                    
                if (!lineIsGreen) {
                    ctx.addOutgoingMessage(graphAddress, new StyleEdge(selfLinkId, outLinkId, "-fx-stroke: green"));
                    lineIsGreen = true;
                }
            }
        }
    }

}
