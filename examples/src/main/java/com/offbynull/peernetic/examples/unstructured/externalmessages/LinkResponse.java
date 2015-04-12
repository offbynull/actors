package com.offbynull.peernetic.examples.unstructured.externalmessages;

public final class LinkResponse extends ExternalMessage {

    public final boolean successful;
    
    public LinkResponse(long id, boolean successful) {
        super(id);
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
