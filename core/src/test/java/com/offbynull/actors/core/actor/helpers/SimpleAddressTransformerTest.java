package com.offbynull.actors.core.actor.helpers;

import com.offbynull.actors.core.actor.helpers.SimpleAddressTransformer;
import com.offbynull.actors.core.shuttle.Address;
import org.junit.Assert;
import org.junit.Test;

public class SimpleAddressTransformerTest {
    
    private static final Address REMOTE_BASE_ADDRESS = Address.of("level1", "level2");
    private static final Address SELF_ADDRESS = Address.of("self");
    private static final String SELF_ID = "selfId";
    private static final String REMOTE_ID = "remoteId";
    
    private SimpleAddressTransformer fixture = new SimpleAddressTransformer(REMOTE_BASE_ADDRESS, SELF_ADDRESS, SELF_ID);

    @Test
    public void mustProperlyConvertSelfAddressToId() {
        String id = fixture.toLinkId(SELF_ADDRESS);
        Assert.assertEquals(SELF_ID, id);
    }

    @Test
    public void mustProperlyConvertSelfAddressWithSuffixToId() {
        String id = fixture.toLinkId(SELF_ADDRESS.appendSuffix("hi", "bye"));
        Assert.assertEquals(SELF_ID, id);
    }

    @Test
    public void mustProperlyConvertRemoteAddressToId() {
        String id = fixture.toLinkId(REMOTE_BASE_ADDRESS.appendSuffix(REMOTE_ID));
        Assert.assertEquals(REMOTE_ID, id);
    }

    @Test
    public void mustProperlyConvertRemoteAddressWithSuffixToId() {
        String id = fixture.toLinkId(REMOTE_BASE_ADDRESS.appendSuffix(REMOTE_ID).appendSuffix("hi", "bye"));
        Assert.assertEquals(REMOTE_ID, id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustFailToConvertRemoteAddressToIdWhenPrefixIsTooShort() {
        Address remoteAddress = REMOTE_BASE_ADDRESS.removeSuffix(1).appendSuffix(REMOTE_ID); // level1:remoteId
        fixture.toLinkId(remoteAddress);
    }

    @Test
    public void mustProperlyConvertSelfLinkToAddress() {
        Address address = fixture.toAddress(SELF_ID);
        Assert.assertEquals(SELF_ADDRESS, address);
    }
    
    @Test
    public void mustProperlyConvertRemoteLinkToAddress() {
        Address address = fixture.toAddress(REMOTE_ID);
        Assert.assertEquals(REMOTE_BASE_ADDRESS.appendSuffix(REMOTE_ID), address);
    }
    
}
