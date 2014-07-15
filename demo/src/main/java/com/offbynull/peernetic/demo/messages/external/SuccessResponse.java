package com.offbynull.peernetic.demo.messages.external;

public final class SuccessResponse extends Response {

    public SuccessResponse(String nonce) {
        super(nonce);
    }

    public SuccessResponse(Request request) {
        super(request.getNonce());
    }

}
