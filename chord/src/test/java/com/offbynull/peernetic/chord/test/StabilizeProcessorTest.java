package com.offbynull.peernetic.chord.test;

import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.messages.SetPredecessorRequest;
import com.offbynull.peernetic.chord.messages.SetPredecessorResponse;
import com.offbynull.peernetic.chord.messages.StatusRequest;
import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.chord.processors.StabilizeProcessor;
import com.offbynull.peernetic.chord.processors.StabilizeProcessor.StabilizeFailedException;
import static com.offbynull.peernetic.chord.test.TestUtils.assertOutgoingEventTypes;
import static com.offbynull.peernetic.chord.test.TestUtils.extractProcessResultEvent;
import static com.offbynull.peernetic.chord.test.TestUtils.extractProcessResultResult;
import com.offbynull.peernetic.eventframework.event.DefaultErrorIncomingEvent;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.impl.lifecycle.InitializeIncomingEvent;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.ReceiveResponseIncomingEvent;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageOutgoingEvent;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class StabilizeProcessorTest {
    
    public StabilizeProcessorTest() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void successNoAdjustTest() throws Exception {
        TrackedIdGenerator tidGen = new TrackedIdGenerator();
        Id _000Id = TestUtils.generateId(3, 0x00L);
        Id _001Id = TestUtils.generateId(3, 0x01L);
        Id _010Id = TestUtils.generateId(3, 0x02L);
        Id _011Id = TestUtils.generateId(3, 0x03L);
        Id _100Id = TestUtils.generateId(3, 0x04L);
        Id _101Id = TestUtils.generateId(3, 0x05L);
        Id _110Id = TestUtils.generateId(3, 0x06L);
        Id _111Id = TestUtils.generateId(3, 0x07L);
        Address _000Address = TestUtils.generateAddressFromId(_000Id);
        Address _001Address = TestUtils.generateAddressFromId(_001Id);
        Address _010Address = TestUtils.generateAddressFromId(_010Id);
        Address _011Address = TestUtils.generateAddressFromId(_011Id);
        Address _100Address = TestUtils.generateAddressFromId(_100Id);
        Address _101Address = TestUtils.generateAddressFromId(_101Id);
        Address _110Address = TestUtils.generateAddressFromId(_110Id);
        Address _111Address = TestUtils.generateAddressFromId(_111Id);
        Pointer base = new Pointer(_000Id, _000Address);
        Pointer successor = new Pointer(_001Id, _001Address);
        StabilizeProcessor sp = new StabilizeProcessor(base, successor);
        
        SendMessageOutgoingEvent smOutEvent;
        IncomingEvent inEvent;
        ProcessResult pr;
        StatusResponse statusResp;
        SetPredecessorResponse setPredResp;
        long trackedId;
        String host;
        int port;

        
        // Trigger to start by sending in garbage event
        inEvent = new InitializeIncomingEvent();
        pr = sp.process(1L, inEvent, tidGen);

        
        // Get message to be sent out and generate fake response
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_001Address.getHost(), host);
        assertEquals(_001Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_001Id, 0L,
                0L, 0L, 0L);
        
        
        // Pass in response
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = sp.process(1L, inEvent, tidGen);
        
        
        // Get message to be sent out and generate fake response
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);

        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(SetPredecessorRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_001Address.getHost(), host);
        assertEquals(_001Address.getPort(), port);
        
        setPredResp = TestUtils.generateSetPredecessorResponse(_001Id, 0L);


        // Pass in response
        inEvent = new ReceiveResponseIncomingEvent(setPredResp, trackedId);
        pr = sp.process(1L, inEvent, tidGen);
        
        
        // Ensure found exact match
        Pointer res = extractProcessResultResult(pr);
        
        assertEquals(_001Id, res.getId());
        assertEquals(_001Address, res.getAddress());
    }

    @Test
    public void successAdjustTest() throws Exception {
        TrackedIdGenerator tidGen = new TrackedIdGenerator();
        Id _000Id = TestUtils.generateId(3, 0x00L);
        Id _001Id = TestUtils.generateId(3, 0x01L);
        Id _010Id = TestUtils.generateId(3, 0x02L);
        Id _011Id = TestUtils.generateId(3, 0x03L);
        Id _100Id = TestUtils.generateId(3, 0x04L);
        Id _101Id = TestUtils.generateId(3, 0x05L);
        Id _110Id = TestUtils.generateId(3, 0x06L);
        Id _111Id = TestUtils.generateId(3, 0x07L);
        Address _000Address = TestUtils.generateAddressFromId(_000Id);
        Address _001Address = TestUtils.generateAddressFromId(_001Id);
        Address _010Address = TestUtils.generateAddressFromId(_010Id);
        Address _011Address = TestUtils.generateAddressFromId(_011Id);
        Address _100Address = TestUtils.generateAddressFromId(_100Id);
        Address _101Address = TestUtils.generateAddressFromId(_101Id);
        Address _110Address = TestUtils.generateAddressFromId(_110Id);
        Address _111Address = TestUtils.generateAddressFromId(_111Id);
        Pointer base = new Pointer(_000Id, _000Address);
        Pointer successor = new Pointer(_001Id, _001Address);
        StabilizeProcessor sp = new StabilizeProcessor(base, successor);
        
        SendMessageOutgoingEvent smOutEvent;
        IncomingEvent inEvent;
        ProcessResult pr;
        StatusResponse statusResp;
        SetPredecessorResponse setPredResp;
        long trackedId;
        String host;
        int port;

        
        // Trigger to start by sending in garbage event
        inEvent = new InitializeIncomingEvent();
        pr = sp.process(1L, inEvent, tidGen);

        
        // Get message to be sent out and generate fake response
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_001Address.getHost(), host);
        assertEquals(_001Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_001Id, 0L,
                1L, 0L, 0L);
        
        
        // Pass in response
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = sp.process(1L, inEvent, tidGen);
        
        
        // Get message to be sent out and generate fake response
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);

        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(SetPredecessorRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_001Address.getHost(), host);
        assertEquals(_001Address.getPort(), port);
        
        setPredResp = TestUtils.generateSetPredecessorResponse(_010Id, 0L);


        // Pass in response
        inEvent = new ReceiveResponseIncomingEvent(setPredResp, trackedId);
        pr = sp.process(1L, inEvent, tidGen);
        
        
        // Ensure found exact match
        Pointer res = extractProcessResultResult(pr);
        
        assertEquals(_001Id, res.getId());
        assertEquals(_001Address, res.getAddress());
    }
    
    @Test(expected = StabilizeFailedException.class)
    public void failureOnPredecessorQueryTest() throws Exception {
        TrackedIdGenerator tidGen = new TrackedIdGenerator();
        Id _000Id = TestUtils.generateId(3, 0x00L);
        Id _001Id = TestUtils.generateId(3, 0x01L);
        Id _010Id = TestUtils.generateId(3, 0x02L);
        Id _011Id = TestUtils.generateId(3, 0x03L);
        Id _100Id = TestUtils.generateId(3, 0x04L);
        Id _101Id = TestUtils.generateId(3, 0x05L);
        Id _110Id = TestUtils.generateId(3, 0x06L);
        Id _111Id = TestUtils.generateId(3, 0x07L);
        Address _000Address = TestUtils.generateAddressFromId(_000Id);
        Address _001Address = TestUtils.generateAddressFromId(_001Id);
        Address _010Address = TestUtils.generateAddressFromId(_010Id);
        Address _011Address = TestUtils.generateAddressFromId(_011Id);
        Address _100Address = TestUtils.generateAddressFromId(_100Id);
        Address _101Address = TestUtils.generateAddressFromId(_101Id);
        Address _110Address = TestUtils.generateAddressFromId(_110Id);
        Address _111Address = TestUtils.generateAddressFromId(_111Id);
        Pointer base = new Pointer(_000Id, _000Address);
        Pointer successor = new Pointer(_001Id, _001Address);
        StabilizeProcessor sp = new StabilizeProcessor(base, successor);
        
        SendMessageOutgoingEvent smOutEvent;
        IncomingEvent inEvent;
        ProcessResult pr;
        StatusResponse statusResp;
        SetPredecessorResponse setPredResp;
        long trackedId;
        String host;
        int port;

        
        // Trigger to start by sending in garbage event
        inEvent = new InitializeIncomingEvent();
        pr = sp.process(1L, inEvent, tidGen);

        
        // Get message to be sent out and generate fake response
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_001Address.getHost(), host);
        assertEquals(_001Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_001Id, 0L,
                1L, 0L, 0L);
        
        
        // Pass in response
        inEvent = new DefaultErrorIncomingEvent(trackedId, null);
        sp.process(1L, inEvent, tidGen);
    }

    @Test(expected = StabilizeFailedException.class)
    public void failureOnPredecessorSetTest() throws Exception {
        TrackedIdGenerator tidGen = new TrackedIdGenerator();
        Id _000Id = TestUtils.generateId(3, 0x00L);
        Id _001Id = TestUtils.generateId(3, 0x01L);
        Id _010Id = TestUtils.generateId(3, 0x02L);
        Id _011Id = TestUtils.generateId(3, 0x03L);
        Id _100Id = TestUtils.generateId(3, 0x04L);
        Id _101Id = TestUtils.generateId(3, 0x05L);
        Id _110Id = TestUtils.generateId(3, 0x06L);
        Id _111Id = TestUtils.generateId(3, 0x07L);
        Address _000Address = TestUtils.generateAddressFromId(_000Id);
        Address _001Address = TestUtils.generateAddressFromId(_001Id);
        Address _010Address = TestUtils.generateAddressFromId(_010Id);
        Address _011Address = TestUtils.generateAddressFromId(_011Id);
        Address _100Address = TestUtils.generateAddressFromId(_100Id);
        Address _101Address = TestUtils.generateAddressFromId(_101Id);
        Address _110Address = TestUtils.generateAddressFromId(_110Id);
        Address _111Address = TestUtils.generateAddressFromId(_111Id);
        Pointer base = new Pointer(_000Id, _000Address);
        Pointer successor = new Pointer(_001Id, _001Address);
        StabilizeProcessor sp = new StabilizeProcessor(base, successor);
        
        SendMessageOutgoingEvent smOutEvent;
        IncomingEvent inEvent;
        ProcessResult pr;
        StatusResponse statusResp;
        SetPredecessorResponse setPredResp;
        long trackedId;
        String host;
        int port;

        
        // Trigger to start by sending in garbage event
        inEvent = new InitializeIncomingEvent();
        pr = sp.process(1L, inEvent, tidGen);

        
        // Get message to be sent out and generate fake response
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_001Address.getHost(), host);
        assertEquals(_001Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_001Id, 0L,
                0L, 0L, 0L);
        
        
        // Pass in response
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = sp.process(1L, inEvent, tidGen);
        
        
        // Get message to be sent out and generate fake response
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);

        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(SetPredecessorRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_001Address.getHost(), host);
        assertEquals(_001Address.getPort(), port);
        
        setPredResp = TestUtils.generateSetPredecessorResponse(_001Id, 0L);


        // Pass in response
        inEvent = new DefaultErrorIncomingEvent(trackedId, null);
        sp.process(1L, inEvent, tidGen);
    }
}