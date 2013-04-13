package com.offbynull.peernetic.chord.test;

import com.offbynull.peernetic.eventframework.impl.network.simpletcp.ReceiveResponseIncomingEvent;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageOutgoingEvent;
import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.FingerTable;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.messages.StatusRequest;
import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.chord.processors.FixFingerProcessor;
import com.offbynull.peernetic.eventframework.event.IncomingEvent;
import com.offbynull.peernetic.eventframework.event.TrackedIdGenerator;
import com.offbynull.peernetic.eventframework.impl.lifecycle.InitializeIncomingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.offbynull.peernetic.chord.test.TestUtils.*;
import com.offbynull.peernetic.eventframework.event.DefaultErrorIncomingEvent;
import com.offbynull.peernetic.eventframework.processor.ProcessResult;

public class FixFingerProcessorTest {

    public FixFingerProcessorTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSuccess() throws Exception {
        // Setup
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
        FingerTable ft = TestUtils.generateFingerTable(0L, 0L, 0L, 1L);
        FixFingerProcessor rp = new FixFingerProcessor(ft, 2);

        SendMessageOutgoingEvent smOutEvent;
        IncomingEvent inEvent;
        ProcessResult pr;
        StatusResponse statusResp;
        long trackedId;
        String host;
        int port;


        // Trigger RP to start by sending in garbage event
        inEvent = new InitializeIncomingEvent();
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Get message to be sent out and generate fake response from 000b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_101Address.getHost(), host);
        assertEquals(_101Address.getPort(), port);
        
        
        // Pass in response to RP
        inEvent = new DefaultErrorIncomingEvent(trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Get message to be sent out and generate fake response from 001b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_001Address.getHost(), host);
        assertEquals(_001Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_001Id, null,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        

        // Get message to be sent out and generate fake response from 011b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_011Address.getHost(), host);
        assertEquals(_011Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_011Id, null,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        

        // Get message to be sent out and generate fake response from 100b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_100Address.getHost(), host);
        assertEquals(_100Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_100Id, null,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Ensure RP found exact match
        boolean res = extractProcessResultResult(pr);
        assertTrue(res);
        
        assertEquals(new Pointer(_100Id, _100Address), ft.get(2));
    }

    @Test
    public void testFailureOnScan() throws Exception {
        // Setup
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
        FingerTable ft = TestUtils.generateFingerTable(0L, 0L, 0L, 1L);
        FixFingerProcessor rp = new FixFingerProcessor(ft, 2);

        SendMessageOutgoingEvent smOutEvent;
        IncomingEvent inEvent;
        ProcessResult pr;
        StatusResponse statusResp;
        long trackedId;
        String host;
        int port;


        // Trigger RP to start by sending in garbage event
        inEvent = new InitializeIncomingEvent();
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Get message to be sent out and generate fake response from 000b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_101Address.getHost(), host);
        assertEquals(_101Address.getPort(), port);
        
        statusResp = TestUtils.generateStatusResponse(_101Id, null,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Get message to be sent out and generate fake response from 001b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_001Address.getHost(), host);
        assertEquals(_001Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_001Id, null,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        

        // Get message to be sent out and generate fake response from 011b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_011Address.getHost(), host);
        assertEquals(_011Address.getPort(), port);
        
        
        // Pass in response to RP
        inEvent = new DefaultErrorIncomingEvent(trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        

        // Ensure RP found exact match
        boolean res = extractProcessResultResult(pr);
        assertFalse(res);
        
        assertEquals(new Pointer(_101Id, _101Address), ft.get(2));
    }

    @Test
    public void testFailureOnSuccessorTest() throws Exception {
        // Setup
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
        FingerTable ft = TestUtils.generateFingerTable(0L, 0L, 0L, 1L);
        FixFingerProcessor rp = new FixFingerProcessor(ft, 2);

        SendMessageOutgoingEvent smOutEvent;
        IncomingEvent inEvent;
        ProcessResult pr;
        StatusResponse statusResp;
        long trackedId;
        String host;
        int port;


        // Trigger RP to start by sending in garbage event
        inEvent = new InitializeIncomingEvent();
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Get message to be sent out and generate fake response from 000b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_101Address.getHost(), host);
        assertEquals(_101Address.getPort(), port);
        
        
        // Pass in response to RP
        inEvent = new DefaultErrorIncomingEvent(trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Get message to be sent out and generate fake response from 001b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_001Address.getHost(), host);
        assertEquals(_001Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_001Id, null,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        

        // Get message to be sent out and generate fake response from 011b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_011Address.getHost(), host);
        assertEquals(_011Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_011Id, null,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        

        // Get message to be sent out and generate fake response from 100b
        assertOutgoingEventTypes(pr, SendMessageOutgoingEvent.class);
        smOutEvent = extractProcessResultEvent(pr, 0);
        
        trackedId = smOutEvent.getTrackedId();
        host = smOutEvent.getHost();
        port = smOutEvent.getPort();
        
        assertEquals(StatusRequest.class, smOutEvent.getRequest().getClass());
        assertEquals(_100Address.getHost(), host);
        assertEquals(_100Address.getPort(), port);
        
        
        // Pass in response to RP
        inEvent = new DefaultErrorIncomingEvent(trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Ensure RP found exact match
        boolean res = extractProcessResultResult(pr);
        assertFalse(res);
        
        // this is 000 because the finger was removed in the initial test that
        // failed
        assertEquals(new Pointer(_000Id, _000Address), ft.get(2));
    }
}