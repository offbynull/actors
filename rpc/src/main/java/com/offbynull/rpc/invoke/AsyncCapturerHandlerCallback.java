package com.offbynull.rpc.invoke;

public interface AsyncCapturerHandlerCallback {
    void responseArrived(byte[] response);
    void responseFailed(Throwable err);
}
