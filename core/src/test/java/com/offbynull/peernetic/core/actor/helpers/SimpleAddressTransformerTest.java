package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.peernetic.core.shuttle.Address;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SimpleAddressTransformerTest {
    
    private static final Address REMOTE_BASE = Address.of("level1", "level2");
    private static final String SELF_ID = "selfId";
    private static final String REMOTE_ID = "remoteId";
    
    private SimpleAddressTransformer fixture;
    
    @Before
    public void setUp() {
        fixture = new SimpleAddressTransformer(REMOTE_BASE, SELF_ID);
    }

    @Test
    public void mustProperlyConvertSelfAddressToId() {
        Address selfAddress = Address.of();
        String id = fixture.selfAddressToLinkId(selfAddress); // doesn't matter way selfAddress is
        
        Assert.assertEquals(SELF_ID, id);
    }

    @Test
    public void mustProperlyConvertRemoteAddressToId() {
        Address remoteAddress = REMOTE_BASE.appendSuffix(REMOTE_ID);
        String id = fixture.remoteAddressToLinkId(remoteAddress);
        
        Assert.assertEquals(REMOTE_ID, id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustFailToConvertRemoteAddressToIdWhenPrefixIsTooShort() {
        Address remoteAddress = REMOTE_BASE.removeSuffix(1).appendSuffix(REMOTE_ID); // level1:remoteId
        String id = fixture.remoteAddressToLinkId(remoteAddress);
        
        Assert.assertEquals(REMOTE_ID, id);
    }

    @Test(expected = IllegalArgumentException.class)
    public void mustFailToConvertRemoteAddressToIdWhenPrefixIsTooLong() {
        Address remoteAddress = REMOTE_BASE.appendSuffix("level3", REMOTE_ID); // level1:level2:level3:remoteId
        String id = fixture.remoteAddressToLinkId(remoteAddress);
        
        Assert.assertEquals(REMOTE_ID, id);
    }

    @Test
    public void mustProperlyConvertLinkToRemoteAddress() {
        Address remoteAddress = fixture.linkIdToRemoteAddress(REMOTE_ID);
        
        Assert.assertEquals(REMOTE_BASE.appendSuffix(REMOTE_ID), remoteAddress);
    }
    
}
