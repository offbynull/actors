package com.offbynull.peernetic.actor;

public final class MessageUtils {
    private MessageUtils() {
    }
    
    public static Incoming flip(Endpoint source, Outgoing outgoing) {
        if (outgoing instanceof OutgoingRequest) {
            OutgoingRequest outgoingRequest = (OutgoingRequest) outgoing;
            return new IncomingRequest(outgoingRequest.getId(), source, outgoingRequest.getContent());
        } else if (outgoing instanceof OutgoingResponse) {
            OutgoingResponse outgoingResponse = (OutgoingResponse) outgoing;
            return new IncomingResponse(outgoingResponse.getId(), source, outgoingResponse.getContent());
        } else {
            throw new IllegalArgumentException();
        }
    }
}
