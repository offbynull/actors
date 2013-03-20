package com.offbynull.peernetic.eventframework.tests;

import com.offbynull.peernetic.eventframework.impl.network.message.Response;

public final class FakeResponse implements Response {
    private String data;

    public FakeResponse(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
