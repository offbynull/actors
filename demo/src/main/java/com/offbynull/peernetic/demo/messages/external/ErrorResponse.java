package com.offbynull.peernetic.demo.messages.external;

public final class ErrorResponse extends Response {

    public ErrorResponse(String nonce) {
        super(nonce);
    }

    public ErrorResponse(Request request) {
        super(request.getNonce());
    }

}
