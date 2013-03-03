package com.offbynull.eventframework.tests;

import com.offbynull.eventframework.handler.communication.Response;

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
