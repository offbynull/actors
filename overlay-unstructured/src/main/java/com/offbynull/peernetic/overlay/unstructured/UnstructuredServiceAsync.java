package com.offbynull.peernetic.overlay.unstructured;

import com.offbynull.peernetic.rpc.invoke.AsyncResultListener;

public interface UnstructuredServiceAsync<A> {
    void getState(AsyncResultListener<State<A>> result);

    void join(AsyncResultListener<Boolean> result, byte[] secret);
    void keepAlive(AsyncResultListener<Boolean> result, byte[] secret);
}
