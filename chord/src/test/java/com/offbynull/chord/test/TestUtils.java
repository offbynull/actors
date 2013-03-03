package com.offbynull.chord.test;

import com.offbynull.chord.Address;
import com.offbynull.chord.FingerTable;
import com.offbynull.chord.Id;
import com.offbynull.chord.Pointer;
import com.offbynull.chord.messages.StatusResponse;
import com.offbynull.chord.util.MessageUtils;
import com.offbynull.eventframework.handler.OutgoingEvent;
import com.offbynull.eventframework.processor.FinishedProcessResult;
import com.offbynull.eventframework.processor.ProcessResult;
import java.nio.ByteBuffer;
import java.util.List;
import static org.junit.Assert.*;

public final class TestUtils {
    
    private TestUtils() {
        
    }

    public static void assertOutgoingEventTypes(
            ProcessResult procRes, Class<? extends OutgoingEvent> ... classes) {
        List<OutgoingEvent> outEvents = procRes.viewOutgoingEvents();
        assertEquals(classes.length, outEvents.size());
        
        for (int i = 0; i < classes.length; i++) {
            assertEquals(classes[i], outEvents.get(i).getClass());
        }
    }

    public static <T extends OutgoingEvent> T extractProcessResultEvent(
            ProcessResult procRes, int index) {
        List<OutgoingEvent> outEvents = procRes.viewOutgoingEvents();
        return (T) outEvents.get(index);
    }

    public static <T extends Object> T extractProcessResultResult(
            ProcessResult procRes) {
        FinishedProcessResult fpr = (FinishedProcessResult) procRes;
        return (T) fpr.getResult();
    }
    
    public static Id generateId(int bitCount, long idData) {
        byte[] bytes = ByteBuffer.allocate(8).putLong(idData).array();
        return new Id(bitCount, bytes);
    }
    
    public static Address generateAddressFromId(Id id) {
        Address selfAddress = new Address(id.asBigInteger().toString(), 1);
        return selfAddress;
    }
    
    public static Pointer generatePointer(int bitCount, long idData) {
        Id id = generateId(bitCount, idData);
        Address address = generateAddressFromId(id);
        return new Pointer(id, address);
    }
    
    public static StatusResponse generateStatusResponse(Id id,
            Long ... entryOvershoot) {
        if (id.getBitCount() != entryOvershoot.length) {
            throw new IllegalArgumentException();
        }
        byte[] bytes = id.asByteArray();
        long value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value += ((long) bytes[i] & 0xffL) << (8 * i);
        }
        
        return generateStatusResponse(value, entryOvershoot);
    }
    
    public static StatusResponse generateStatusResponse(long idData,
            Long ... entryOvershoot) {
        FingerTable ft = generateFingerTable(idData, entryOvershoot);
        return MessageUtils.createFrom(ft.getBaseId(), ft.dump(), false);
    }
    
    public static FingerTable generateFingerTable(long idData,
            Long ... entryOvershoot) {
        byte[] bytes = ByteBuffer.allocate(8).putLong(idData).array();
        byte[][] convertEntryOvershoot = new byte[entryOvershoot.length][];
        
        for (int i = 0; i < entryOvershoot.length; i++) {
            Long val = entryOvershoot[i];
            byte[] data;
            
            if (val == null) {
                data = null;
            } else {
                data = ByteBuffer.allocate(8).putLong(val).array();
            }
            
            convertEntryOvershoot[i] = data;
        }
        
        return generateFingerTable(bytes, convertEntryOvershoot);
    }
    
    public static FingerTable generateFingerTable(byte[] idData,
            byte[] ... entryOvershoot) {
        int bitCount = entryOvershoot.length;
        
        Id selfId = new Id(bitCount, idData);
        Address selfAddress = generateAddressFromId(selfId);
        FingerTable ft = new FingerTable(new Pointer(selfId, selfAddress));
        
        for (int i = 0; i < entryOvershoot.length; i++) {
            if (entryOvershoot[i] != null) {
                Id overshootAmountId = new Id(bitCount, entryOvershoot[i]);
                Id fingId = ft.getExpectedId(i).add(overshootAmountId);
                Address fingAddress = generateAddressFromId(fingId);
                
                // must be greater than or equal to expected id
                Id expId = ft.getExpectedId(i);
                if (fingId.comparePosition(selfId, expId) < 0) {
                    throw new IllegalArgumentException();
                }
                
                // must be less than next id
                if (i < entryOvershoot.length - 1) {
                    Id nextId = ft.getExpectedId(i + 1);
                    if (fingId.comparePosition(selfId, nextId) >= 0) {
                        throw new IllegalArgumentException();
                    }
                }
                
                Pointer fingPtr = new Pointer(fingId, fingAddress);
                ft.put(fingPtr);
            }
        }
        
        return ft;
    }
}
