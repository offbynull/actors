package com.offbynull.peernetic.debug.localgateway;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.debug.actornetwork.HubEndpointDirectory;
import com.offbynull.peernetic.debug.actornetwork.HubEndpointIdentifier;
import com.offbynull.peernetic.debug.actornetwork.messages.JoinHub;
import com.offbynull.peernetic.debug.actornetwork.messages.LeaveHub;
import com.offbynull.peernetic.network.Gateway;
import com.offbynull.peernetic.network.GatewayListener;
import com.offbynull.peernetic.network.Message;
import org.apache.commons.lang3.Validate;

public final class LocalGateway<A> implements Gateway<A> {
    private final HubEndpointDirectory<A> hubEndpointDirectory;
    private final HubEndpointIdentifier<A> hubEndpointIdentifier;
    private final Endpoint hubEndpoint;
    private final GatewayListener gatewayListener;
    private final MockEndpoint mockEndpoint;
    private final A localAddress;

    public LocalGateway(A localAddress, LocalGatewayHub<A> localGatewayHub, GatewayListener gatewayListener) {
        Validate.notNull(localAddress);
        Validate.notNull(localGatewayHub);
        Validate.notNull(gatewayListener);
        
        this.localAddress = localAddress;
        this.hubEndpoint = localGatewayHub.getHubEndpoint();
        this.hubEndpointDirectory = new HubEndpointDirectory<>(localAddress, hubEndpoint);
        this.hubEndpointIdentifier = new HubEndpointIdentifier<>();
        this.gatewayListener = gatewayListener;
        this.mockEndpoint = new MockEndpoint();
        
        hubEndpoint.send(mockEndpoint, new JoinHub<>(localAddress));
    }
    
    @Override
    public void send(A destination, Object message) {
        Endpoint nodeToHubEndpoint = hubEndpointDirectory.lookup(destination);
        nodeToHubEndpoint.send(mockEndpoint, message);
    }

    @Override
    public void close() throws Exception {
        hubEndpoint.send(mockEndpoint, new LeaveHub<>(localAddress));
    }
    
    private final class MockEndpoint implements Endpoint {

        @Override
        public void send(Endpoint source, Object message) {
            A remoteAddress = hubEndpointIdentifier.identify(source);
            Message<A> incomingMessage = new Message<>(localAddress, remoteAddress, message, LocalGateway.this);
            gatewayListener.onReadMessage(incomingMessage);
        }
    }
}
