package com.offbynull.eventframework.tests;

import com.offbynull.eventframework.handler.communication.Request;

public final class FakeRequest implements Request {
    private String data;

    public FakeRequest(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
