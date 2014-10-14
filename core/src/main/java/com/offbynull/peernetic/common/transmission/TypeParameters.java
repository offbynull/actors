package com.offbynull.peernetic.common.transmission;

import org.apache.commons.lang3.Validate;

final class TypeParameters {
    private final IncomingRequestTypeParameters incomingRequestTypeParameters;
    private final IncomingResponseTypeParameters incomingResponseTypeParameters;
    private final OutgoingRequestTypeParameters outgoingRequestTypeParameters;
    private final OutgoingResponseTypeParameters outgoingResponseTypeParameters;

    public TypeParameters(IncomingRequestTypeParameters incomingRequestTypeParameters,
            IncomingResponseTypeParameters incomingResponseTypeParameters,
            OutgoingRequestTypeParameters outgoingRequestTypeParameters,
            OutgoingResponseTypeParameters outgoingResponseTypeParameters) {
        Validate.isTrue(incomingRequestTypeParameters != null
                || incomingResponseTypeParameters != null
                || outgoingRequestTypeParameters != null
                || outgoingResponseTypeParameters != null, "Atleast one argument must be non-null");
        this.incomingRequestTypeParameters = incomingRequestTypeParameters;
        this.incomingResponseTypeParameters = incomingResponseTypeParameters;
        this.outgoingRequestTypeParameters = outgoingRequestTypeParameters;
        this.outgoingResponseTypeParameters = outgoingResponseTypeParameters;
    }

    public IncomingRequestTypeParameters getIncomingRequestTypeParameters() {
        return incomingRequestTypeParameters;
    }

    public IncomingResponseTypeParameters getIncomingResponseTypeParameters() {
        return incomingResponseTypeParameters;
    }

    public OutgoingRequestTypeParameters getOutgoingRequestTypeParameters() {
        return outgoingRequestTypeParameters;
    }

    public OutgoingResponseTypeParameters getOutgoingResponseTypeParameters() {
        return outgoingResponseTypeParameters;
    }
}
