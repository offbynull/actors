package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.ProxyHelper.ForwardInformation;
import com.offbynull.peernetic.core.shuttle.Address;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProxyHelperTest {
    
    private static final String SOURCE_PREFIX = "source";
    private static final String PROXY_PREFIX = "proxy";
    private static final String DESTINATION_PREFIX = "destination";
    
    @Rule
    public ExpectedException exception = ExpectedException.none();
    
    private Context context;
    private ProxyHelper fixture;
    
    @Before
    public void setUp() {
        context = mock(Context.class);
        fixture = new ProxyHelper(context, Address.of(SOURCE_PREFIX));
    }

    @Test
    public void mustProperlyForwardOutgoing() {
        when(context.self()).thenReturn(Address.of(PROXY_PREFIX));
        when(context.source()).thenReturn(Address.of(SOURCE_PREFIX, "sourceid"));
        when(context.destination()).thenReturn(Address.of(PROXY_PREFIX, DESTINATION_PREFIX, "destid"));
        
        ForwardInformation forwardInfo = fixture.generateOutboundForwardInformation();
        
        assertEquals(Address.of("sourceid"), forwardInfo.getProxyFromAddress());
        assertEquals(Address.of(DESTINATION_PREFIX, "destid"), forwardInfo.getProxyToAddress());
    }

    @Test
    public void mustProperlyForwardIncoming() {
        when(context.self()).thenReturn(Address.of(PROXY_PREFIX));
        when(context.source()).thenReturn(Address.of(DESTINATION_PREFIX, "destid"));
        when(context.destination()).thenReturn(Address.of(PROXY_PREFIX, "sourceid"));
        
        ForwardInformation forwardInfo = fixture.generateInboundForwardInformation();
        
        assertEquals(Address.of(DESTINATION_PREFIX, "destid"), forwardInfo.getProxyFromAddress());
        assertEquals(Address.of(SOURCE_PREFIX, "sourceid"), forwardInfo.getProxyToAddress());
    }

    @Test
    public void mustFailIfOutboundMessageIsNotFromProxiedActor() {
        when(context.self()).thenReturn(Address.of(PROXY_PREFIX));
        when(context.source()).thenReturn(Address.of(DESTINATION_PREFIX, "destid"));
        when(context.destination()).thenReturn(Address.of(PROXY_PREFIX + "sourceid"));
        
        exception.expect(IllegalArgumentException.class);
        fixture.generateOutboundForwardInformation();
    }
    
}
