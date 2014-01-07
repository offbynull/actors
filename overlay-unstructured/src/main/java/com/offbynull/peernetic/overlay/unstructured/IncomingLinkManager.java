package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.actor.PushQueue;
import com.offbynull.peernetic.actor.helpers.TimeoutManager;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.Validate;

final class IncomingLinkManager<A> {
    private int maxLinks;
    
    private Map<Endpoint, ByteBuffer> links; // address to secret
    private TimeoutManager<Endpoint> expireTimeoutManager;
    private long expireDuration;
    
    private LinkRepository<A> linkRepository;

    public IncomingLinkManager(int maxLinks, long expireDuration, LinkRepository<A> linkRepository) {
        Validate.inclusiveBetween(0, Integer.MAX_VALUE, maxLinks);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, expireDuration);
        Validate.notNull(linkRepository);
        
        this.maxLinks = maxLinks;
        this.expireDuration = expireDuration;
        links = new HashMap<>();
        expireTimeoutManager = new TimeoutManager<>();
        
        this.linkRepository = linkRepository;
    }

    public boolean processCommand(long timestamp, Endpoint from, Object content, PushQueue pushQueue) {
        if (content instanceof InitiateJoinCommand) {
            InitiateJoinCommand ijc = (InitiateJoinCommand) content;
            
            ByteBuffer secret = ijc.getSecret();
            if (links.size() == maxLinks || links.containsKey(from) || secret.remaining() != Constants.SECRET_SIZE) {
                pushQueue.push(from, new JoinFailedCommand());
                return true;
            }
            
            links.put(from, secret);
            expireTimeoutManager.add(from, timestamp + expireDuration);
            
            linkRepository.addLink(LinkType.INCOMING, from);
            
            State<A> state = linkRepository.toState();
            pushQueue.push(from, new JoinSuccessfulCommand(state));
            
            return true;
        } else if (content instanceof InitiateKeepAliveCommand) {
            InitiateKeepAliveCommand ikac = (InitiateKeepAliveCommand) content;
            
            ByteBuffer secret = ikac.getSecret();
            if (!links.containsKey(from) || secret.remaining() != Constants.SECRET_SIZE || !links.get(from).equals(secret)) {
                pushQueue.push(from, new KeepAliveFailedCommand());
                return true;
            }
            
            expireTimeoutManager.cancel(from);
            expireTimeoutManager.add(from, timestamp + expireDuration);
            
            State<A> state = linkRepository.toState();
            pushQueue.push(from, new KeepAliveSuccessfulCommand(state));
            
            return true;
        }
        
        return false;
    }
    
    public long process(long timestamp, PushQueue pushQueue) {
        TimeoutManager.TimeoutManagerResult<Endpoint> expiredTimeouts = expireTimeoutManager.process(timestamp);
        for (Endpoint endpoint : expiredTimeouts.getTimedout()) {
            links.remove(endpoint);
            linkRepository.removeLink(LinkType.INCOMING, endpoint);
        }
        
        return expiredTimeouts.getNextTimeoutTimestamp();
    }
}
