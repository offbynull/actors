package com.offbynull.peernetic.chord.test;

import com.offbynull.peernetic.eventframework.impl.network.simpletcp.ReceiveResponseIncomingEvent;
import com.offbynull.peernetic.eventframework.impl.network.simpletcp.SendMessageOutgoingEvent;
import com.offbynull.peernetic.chord.messages.StatusRequest;
import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.chord.processors.RouteProcessor;
import com.offbynull.peernetic.chord.processors.RouteProcessor.RouteProcessorResult;
import com.offbynull.peernetic.chord.processors.RouteProcessor.RouteFailedBackwardException;
import com.offbynull.peernetic.chord.processors.RouteProcessor.RouteFailedException;
import com.offbynull.peernetic.chord.processors.RouteProcessor.RouteFailedSelfException;
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
import com.offbynull.peernetic.p2ptools.identification.Address;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
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
    public void testSuccess() throws Exception {
        // Setup
        TrackedIdGenerator tidGen = new TrackedIdGenerator();
        BitLimitedId _000Id = TestUtils.generateId(3, 0x00L);
        BitLimitedId _001Id = TestUtils.generateId(3, 0x01L);
        BitLimitedId _010Id = TestUtils.generateId(3, 0x02L);
        BitLimitedId _011Id = TestUtils.generateId(3, 0x03L);
        BitLimitedId _100Id = TestUtils.generateId(3, 0x04L);
        BitLimitedId _101Id = TestUtils.generateId(3, 0x05L);
        BitLimitedId _110Id = TestUtils.generateId(3, 0x06L);
        BitLimitedId _111Id = TestUtils.generateId(3, 0x07L);
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
        assertEquals(_000Address.getIpAsString(), host);
        assertEquals(_000Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_000Id, null,
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
        assertEquals(_100Address.getIpAsString(), host);
        assertEquals(_100Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_100Id, null,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Ensure RP found exact match
        RouteProcessorResult res = extractProcessResultResult(pr);
        
        BitLimitedPointer expectedPtr = new BitLimitedPointer(_101Id, _101Address);
        Set<Address> expectedAddresses = new HashSet<>();
        expectedAddresses.add(_000Address);
        expectedAddresses.add(_100Address);
        
        assertEquals(expectedPtr, res.getFound());
        assertEquals(expectedAddresses, res.viewAccessedAddresses());
    }

    @Test(expected = RouteFailedBackwardException.class)
    public void testBackwardFailure() throws Exception {
        // Setup
        TrackedIdGenerator tidGen = new TrackedIdGenerator();
        BitLimitedId _000Id = TestUtils.generateId(3, 0x00L);
        BitLimitedId _001Id = TestUtils.generateId(3, 0x01L);
        BitLimitedId _010Id = TestUtils.generateId(3, 0x02L);
        BitLimitedId _011Id = TestUtils.generateId(3, 0x03L);
        BitLimitedId _100Id = TestUtils.generateId(3, 0x04L);
        BitLimitedId _101Id = TestUtils.generateId(3, 0x05L);
        BitLimitedId _110Id = TestUtils.generateId(3, 0x06L);
        BitLimitedId _111Id = TestUtils.generateId(3, 0x07L);
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
        assertEquals(_000Address.getIpAsString(), host);
        assertEquals(_000Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_000Id, null,
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
        assertEquals(_100Address.getIpAsString(), host);
        assertEquals(_100Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_000Id, null,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
        
        
        // Ensure RP found exact match
        RouteProcessorResult res = extractProcessResultResult(pr);
        
        BitLimitedPointer expectedPtr = new BitLimitedPointer(_101Id, _101Address);
        Set<Address> expectedAddresses = new HashSet<>();
        expectedAddresses.add(_000Address);
        expectedAddresses.add(_100Address);
        
        assertEquals(expectedPtr, res.getFound());
        assertEquals(expectedAddresses, res.viewAccessedAddresses());
    }
    
    @Test(expected = RouteFailedSelfException.class)
    public void testSelfFailure() throws Exception {
        // Setup
        TrackedIdGenerator tidGen = new TrackedIdGenerator();
        BitLimitedId _000Id = TestUtils.generateId(3, 0x00L);
        BitLimitedId _001Id = TestUtils.generateId(3, 0x01L);
        BitLimitedId _010Id = TestUtils.generateId(3, 0x02L);
        BitLimitedId _011Id = TestUtils.generateId(3, 0x03L);
        BitLimitedId _100Id = TestUtils.generateId(3, 0x04L);
        BitLimitedId _101Id = TestUtils.generateId(3, 0x05L);
        BitLimitedId _110Id = TestUtils.generateId(3, 0x06L);
        BitLimitedId _111Id = TestUtils.generateId(3, 0x07L);
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
        assertEquals(_000Address.getIpAsString(), host);
        assertEquals(_000Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_001Id, null,
                null, null, 3L /* Points to _000Id */);
        
        
        // Pass in response to RP
        inEvent = new ReceiveResponseIncomingEvent(statusResp, trackedId);
        pr = rp.process(1L, inEvent, tidGen);
    }
    
    @Test(expected = RouteFailedException.class)
    public void testCommunicationFailure() throws Exception {
        // Setup
        TrackedIdGenerator tidGen = new TrackedIdGenerator();
        BitLimitedId _000Id = TestUtils.generateId(3, 0x00L);
        BitLimitedId _001Id = TestUtils.generateId(3, 0x01L);
        BitLimitedId _010Id = TestUtils.generateId(3, 0x02L);
        BitLimitedId _011Id = TestUtils.generateId(3, 0x03L);
        BitLimitedId _100Id = TestUtils.generateId(3, 0x04L);
        BitLimitedId _101Id = TestUtils.generateId(3, 0x05L);
        BitLimitedId _110Id = TestUtils.generateId(3, 0x06L);
        BitLimitedId _111Id = TestUtils.generateId(3, 0x07L);
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
        assertEquals(_000Address.getIpAsString(), host);
        assertEquals(_000Address.getPort(), port);

        statusResp = TestUtils.generateStatusResponse(_000Id, null,
                0L, 0L, 0L);
        
        
        // Pass in response to RP
        inEvent = new DefaultErrorIncomingEvent(trackedId);
        pr = rp.process(1L, inEvent, tidGen);
    }
}