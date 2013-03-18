package com.offbynull.peernetic.eventframework.tests;

import com.offbynull.eventframework.network.message.Request;

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
