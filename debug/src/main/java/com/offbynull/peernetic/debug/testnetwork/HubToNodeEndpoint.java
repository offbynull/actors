package com.offbynull.peernetic.debug.testnetwork;

import com.offbynull.peernetic.actor.Endpoint;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;

final class HubToNodeEndpoint<A> implements Endpoint {
    private Endpoint nodeEndpoint;

    public HubToNodeEndpoint(Endpoint nodeEndpoint) {
        Validate.notNull(nodeEndpoint);
        this.nodeEndpoint = nodeEndpoint;
    }

    public Endpoint getNodeEndpoint() {
        return nodeEndpoint;
    }

    @Override
    public void send(Endpoint source, Object message) {
        ObjectInputStream ois = null;
        try {
            byte[] data = ByteBufferUtils.copyContentsToArray((ByteBuffer) message);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ois = new ObjectInputStream(bais);
            Object obj = ois.readObject();
            nodeEndpoint.send(source, obj);
        } catch (ClassNotFoundException | ClassCastException | IOException ex) {
            // TODO: Log and do nothing
        } finally {
            IOUtils.closeQuietly(ois);
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.nodeEndpoint);
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
        final HubToNodeEndpoint<?> other = (HubToNodeEndpoint<?>) obj;
        if (!Objects.equals(this.nodeEndpoint, other.nodeEndpoint)) {
            return false;
        }
        return true;
    }

}
