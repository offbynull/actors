package com.offbynull.rpc.invoke;

public interface AsyncCapturerHandler {
    void invokationTriggered(byte[] data, AsyncCapturerHandlerCallback responseHandler);
    void invokationFailed(Throwable err);
}
