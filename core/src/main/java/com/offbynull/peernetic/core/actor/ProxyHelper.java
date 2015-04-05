package com.offbynull.peernetic.core.actor;

import com.offbynull.peernetic.core.common.AddressUtils;
import static com.offbynull.peernetic.core.common.AddressUtils.SEPARATOR;
import org.apache.commons.lang3.Validate;

public final class ProxyHelper {

    private final Context context;
    private final String actorPrefix;

    public ProxyHelper(Context context, String actorPrefix) {
        Validate.notNull(context);
        Validate.notNull(actorPrefix);
        this.context = context;
        this.actorPrefix = actorPrefix;
    }

    public ForwardInformation generateOutboundForwardInformation() {
        // Get address to proxy to
        String selfAddr = context.getSelf();
        String dstAddr = context.getDestination();
        String proxyToAddress = AddressUtils.relativize(selfAddr, dstAddr); // treat suffix for dst of this msg as address to proxy to

        // Get suffix for from address
        String srcAddr = context.getSource();
        String proxyFromId = AddressUtils.relativize(actorPrefix, srcAddr);
        
        return new ForwardInformation(proxyFromId, proxyToAddress);
    }
    
    public ForwardInformation generatInboundForwardInformation() {
        // Get suffix portion of incoming message's destination address
        String selfAddr = context.getSelf();
        String dstAddr = context.getDestination();
        String suffix = AddressUtils.relativize(selfAddr, dstAddr);
        String proxyToAddress = actorPrefix + (suffix != null ? SEPARATOR + suffix : "");
        
        // Get sender
        String proxyFromId = context.getSource();
        
        return new ForwardInformation(proxyFromId, proxyToAddress);
    }
    
    public void forwardToOutside(Object message) {
        Validate.notNull(message);
        
        ForwardInformation forwardInfo = generateOutboundForwardInformation();

        context.addOutgoingMessage(
                forwardInfo.getProxyFromId(),
                forwardInfo.getProxyToAddress(),
                message);
    }

    public void forwardToActor(Object message) {
        Validate.notNull(message);
        
        ForwardInformation forwardInfo = generatInboundForwardInformation();

        context.addOutgoingMessage(
                forwardInfo.getProxyFromId(),
                forwardInfo.getProxyToAddress(),
                message);
    }
    
    public boolean isMessageFromActor() {
        return isMessageFrom(actorPrefix);
    }
    
    public boolean isMessageFrom(String addressPrefix) {
        Validate.notNull(addressPrefix);
        return AddressUtils.isParent(addressPrefix, context.getSource());
    }
    
    public static final class ForwardInformation {
        private final String proxyFromId;
        private final String proxyToAddress;

        private ForwardInformation(String proxyFromId, String proxyToAddress) {
            this.proxyFromId = proxyFromId;
            this.proxyToAddress = proxyToAddress;
        }

        public String getProxyFromId() {
            return proxyFromId;
        }

        public String getProxyToAddress() {
            return proxyToAddress;
        }
        
    }
}
