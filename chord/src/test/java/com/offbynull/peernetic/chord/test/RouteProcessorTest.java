package com.offbynull.peernetic.chord.test;

import com.offbynull.peernetic.eventframework.impl.network.simpletcp.ReceiveResponseIncomingEvent;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageOutgoingEvent;
import com.offbynull.peernetic.chord.Address;
import com.offbynull.peernetic.chord.Id;
import com.offbynull.peernetic.chord.Pointer;
import com.offbynull.peernetic.chord.messages.StatusRequest;
import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.chord.processors.RouteProcessor;
import com.offbynull.peernetic.chord.processors.RouteProcessor.Result;
import com.offbynull.peernetic.chord.processors.RouteProcessor.RouteBackwardProcessorException;
import com.offbynull.peernetic.chord.processors.RouteProcessor.RouteFailedProcessorException;
import com.offbynull.peernetic.chord.processors.RouteProcessor.RouteSelfProcessorException;
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
import java.util.HashSet;
import java.util.Set;

public class RouteProcessorTest {

    public RouteProcessorTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSuccess() {
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
        RouteProcessor rp = new RouteProcessor(_111Id, _101Id,
                _000Address);

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
        assertEquals(_000Address.getHost(), host);
        assertEquals(_000Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_000Id,
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

        statusResp = TestUtils.generateStatusResponse(_100Id,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Ensure RP found exact match
        Result res = extractProcessResultResult(pr);
        
        Pointer expectedPtr = new Pointer(_101Id, _101Address);
        Set<Address> expectedAddresses = new HashSet<>();
        expectedAddresses.add(_000Address);
        expectedAddresses.add(_100Address);
        
        assertEquals(expectedPtr, res.getFound());
        assertEquals(expectedAddresses, res.viewAccessedAddresses());
    }

    @Test(expected = RouteBackwardProcessorException.class)
    public void testBackwardFailure() {
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
        RouteProcessor rp = new RouteProcessor(_111Id, _101Id,
                _000Address);

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
        assertEquals(_000Address.getHost(), host);
        assertEquals(_000Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_000Id,
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

        statusResp = TestUtils.generateStatusResponse(_000Id,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Ensure RP found exact match
        Result res = extractProcessResultResult(pr);
        
        Pointer expectedPtr = new Pointer(_101Id, _101Address);
        Set<Address> expectedAddresses = new HashSet<>();
        expectedAddresses.add(_000Address);
        expectedAddresses.add(_100Address);
        
        assertEquals(expectedPtr, res.getFound());
        assertEquals(expectedAddresses, res.viewAccessedAddresses());
    }
    
    @Test(expected = RouteSelfProcessorException.class)
    public void testSelfFailure() {
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
        RouteProcessor rp = new RouteProcessor(_000Id, _111Id,
                _000Address);

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
        assertEquals(_000Address.getHost(), host);
        assertEquals(_000Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_001Id,
                null, null, 3L /* Points to _000Id */);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
    }
    
    @Test(expected = RouteFailedProcessorException.class)
    public void testCommunicationFailure() {
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
        RouteProcessor rp = new RouteProcessor(_111Id, _101Id,
                _000Address);

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
        assertEquals(_000Address.getHost(), host);
        assertEquals(_000Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_000Id,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new DefaultErrorIncomingEvent(trackedId);
        pr = rp.process(1L, inEvent, tidGen);
    }
}