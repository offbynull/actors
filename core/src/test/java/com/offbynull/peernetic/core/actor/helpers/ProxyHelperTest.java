package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.ProxyHelper.ForwardInformation;
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
        fixture = new ProxyHelper(context, SOURCE_PREFIX);
    }

    @Test
    public void mustProperlyForwardOutgoing() {
        when(context.getSelf()).thenReturn(PROXY_PREFIX);
        when(context.getSource()).thenReturn(SOURCE_PREFIX + ":sourceid");
        when(context.getDestination()).thenReturn(PROXY_PREFIX + ":" + DESTINATION_PREFIX + ":destid");
        
        ForwardInformation forwardInfo = fixture.generateOutboundForwardInformation();
        
        assertEquals("sourceid", forwardInfo.getProxyFromId());
        assertEquals(DESTINATION_PREFIX + ":destid", forwardInfo.getProxyToAddress());
    }

    @Test
    public void mustProperlyForwardIncoming() {
        when(context.getSelf()).thenReturn(PROXY_PREFIX);
        when(context.getSource()).thenReturn(DESTINATION_PREFIX + ":destid");
        when(context.getDestination()).thenReturn(PROXY_PREFIX + ":sourceid");
        
        ForwardInformation forwardInfo = fixture.generatInboundForwardInformation();
        
        assertEquals(DESTINATION_PREFIX + ":destid", forwardInfo.getProxyFromId());
        assertEquals(SOURCE_PREFIX + ":sourceid", forwardInfo.getProxyToAddress());
    }

    @Test
    public void mustFailIfOutboundMessageIsNotFromProxiedActor() {
        when(context.getSelf()).thenReturn(PROXY_PREFIX);
        when(context.getSource()).thenReturn(DESTINATION_PREFIX + ":destid");
        when(context.getDestination()).thenReturn(PROXY_PREFIX + ":sourceid");
        
        exception.expect(IllegalArgumentException.class);
        fixture.generateOutboundForwardInformation();
    }
    
}
