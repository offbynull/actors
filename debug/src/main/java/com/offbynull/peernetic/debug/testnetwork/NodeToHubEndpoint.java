package com.offbynull.peernetic.debug.testnetwork;

import com.offbynull.peernetic.actor.Endpoint;
import com.offbynull.peernetic.debug.testnetwork.messages.DepartMessage;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.Validate;

final class NodeToHubEndpoint<A> implements Endpoint {
    private Endpoint hubEndpoint;
    private A srcId;
    private A dstId;

    public NodeToHubEndpoint(Endpoint hubEndpoint, A srcId, A dstId) {
        Validate.notNull(hubEndpoint);
        Validate.notNull(srcId);
        Validate.notNull(dstId);
        
        this.hubEndpoint = hubEndpoint;
        this.srcId = srcId;
        this.dstId = dstId;
    }

    public Endpoint getHubEndpoint() {
        return hubEndpoint;
    }

    public A getAddress() {
        return dstId;
    }

    @Override
    public void send(Endpoint source, Object message) {
        ObjectOutputStream oos = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(message);
            byte[] data = baos.toByteArray();
            ByteBuffer buffer = ByteBuffer.wrap(data);
            hubEndpoint.send(source, new DepartMessage<>(buffer, srcId, dstId));
        } catch (IOException ex) {
            // TODO: Log and do nothing
        } finally {
            IOUtils.closeQuietly(oos);
        }
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash + Objects.hashCode(this.hubEndpoint);
        hash = 61 * hash + Objects.hashCode(this.srcId);
        hash = 61 * hash + Objects.hashCode(this.dstId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodeToHubEndpoint<?> other = (NodeToHubEndpoint<?>) obj;
        if (!Objects.equals(this.hubEndpoint, other.hubEndpoint)) {
            return false;
        }
        if (!Objects.equals(this.srcId, other.srcId)) {
            return false;
        }
        if (!Objects.equals(this.dstId, other.dstId)) {
            return false;
        }
        return true;
    }

}
