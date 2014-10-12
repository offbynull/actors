package com.offbynull.peernetic.playground.chorddht.messages.internal;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.EndpointDirectory;
import com.offbynull.peernetic.actor.EndpointIdentifier;
import com.offbynull.peernetic.actor.EndpointScheduler;
import com.offbynull.peernetic.common.identification.Id;
import com.offbynull.peernetic.playground.chorddht.ChordActiveListener;
import com.offbynull.peernetic.playground.chorddht.ChordDeactiveListener;
import com.offbynull.peernetic.playground.chorddht.ChordLinkListener;
import com.offbynull.peernetic.playground.chorddht.ChordUnlinkListener;
import org.apache.commons.lang3.Validate;

public final class Start<A> {

    private final ChordActiveListener<Id> activeListener;
    private final ChordLinkListener<Id> linkListener;
    private final ChordUnlinkListener<Id> unlinkListener;
    private final ChordDeactiveListener<Id> deactiveListener;
    private final EndpointDirectory<A> endpointDirectory;
    private final EndpointIdentifier<A> endpointIdentifier;
    private final EndpointScheduler endpointScheduler;
    private final Endpoint selfEndpoint;
    private final Id selfId;
    private final A bootstrapAddress;

    public Start(ChordActiveListener<Id> activeListener, ChordLinkListener<Id> linkListener, ChordUnlinkListener<Id> unlinkListener,
            ChordDeactiveListener<Id> deactiveListener, EndpointDirectory<A> endpointDirectory, EndpointIdentifier<A> endpointIdentifier,
            EndpointScheduler endpointScheduler, Endpoint selfEndpoint, Id selfId, A bootstrapAddress) {
        Validate.notNull(activeListener);
        Validate.notNull(linkListener);
        Validate.notNull(unlinkListener);
        Validate.notNull(deactiveListener);
        Validate.notNull(endpointDirectory);
        Validate.notNull(endpointIdentifier);
        Validate.notNull(endpointScheduler);
        Validate.notNull(selfEndpoint);
        Validate.notNull(selfId);
//        Validate.notNull(bootstrapAddress); // may be null
        
        this.activeListener = activeListener;
        this.linkListener = linkListener;
        this.unlinkListener = unlinkListener;
        this.deactiveListener = deactiveListener;
        this.endpointDirectory = endpointDirectory;
        this.endpointIdentifier = endpointIdentifier;
        this.endpointScheduler = endpointScheduler;
        this.selfEndpoint = selfEndpoint;
        this.selfId = selfId;
        this.bootstrapAddress = bootstrapAddress;
    }

    public ChordActiveListener<Id> getActiveListener() {
        return activeListener;
    }

    public ChordLinkListener<Id> getLinkListener() {
        return linkListener;
    }

    public ChordUnlinkListener<Id> getUnlinkListener() {
        return unlinkListener;
    }

    public ChordDeactiveListener<Id> getDeactiveListener() {
        return deactiveListener;
    }

    public EndpointDirectory<A> getEndpointDirectory() {
        return endpointDirectory;
    }

    public EndpointIdentifier<A> getEndpointIdentifier() {
        return endpointIdentifier;
    }

    public EndpointScheduler getEndpointScheduler() {
        return endpointScheduler;
    }

    public Endpoint getSelfEndpoint() {
        return selfEndpoint;
    }

    public Id getSelfId() {
        return selfId;
    }

    public A getBootstrapAddress() {
        return bootstrapAddress;
    }
    
}
