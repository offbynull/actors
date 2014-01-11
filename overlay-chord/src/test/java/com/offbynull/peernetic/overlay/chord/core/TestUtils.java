package com.offbynull.peernetic.overlay.chord.core;

import com.offbynull.peernetic.overlay.chord.core.FingerTable;
import com.offbynull.peernetic.overlay.common.id.Id;
import com.offbynull.peernetic.overlay.common.id.Pointer;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TestUtils {
    
    private TestUtils() {
        
    }
    
    public static Id generateId(int bitCount, long idData) {
        byte[] bytes = ByteBuffer.allocate(8).putLong(idData).array();
        return new Id(bytes, new BigInteger("2").pow(bitCount).subtract(BigInteger.ONE).toByteArray());
    }
    
    public static InetSocketAddress generateAddressFromId(Id id) {
        try {
            int port = generatePortFromId(id);
            return InetSocketAddress.createUnresolved(
                    new String(id.getValueAsByteArray(), "US-ASCII"), port);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException();
        }
    }
    
    public static Pointer<InetSocketAddress> generatePointer(int bitCount, long idData) {
        Id id = generateId(bitCount, idData);
        InetSocketAddress address = generateAddressFromId(id);
        return new Pointer<>(id, address);
    }
    
    public static FingerTable<InetSocketAddress> generateFingerTable(long idData,
            Long ... entryOvershoot) {
        byte[] bytes = ByteBuffer.allocate(8).putLong(idData).array();
        byte[][] convertEntryOvershoot = new byte[entryOvershoot.length][];
        
        for (int i = 0; i < entryOvershoot.length; i++) {
            Long val = entryOvershoot[i];
            byte[] data = val == null ? null
                    : ByteBuffer.allocate(8).putLong(val).array();
            convertEntryOvershoot[i] = data;
        }
        
        return generateFingerTable(bytes, convertEntryOvershoot);
    }
    
    public static FingerTable<InetSocketAddress> generateFingerTable(byte[] idData,
            byte[] ... entryOvershoot) {
        int bitCount = entryOvershoot.length;
        
        Id selfId = new Id(idData, new BigInteger("2").pow(bitCount).subtract(BigInteger.ONE).toByteArray());
        InetSocketAddress selfAddress = generateAddressFromId(selfId);
        FingerTable<InetSocketAddress> ft = new FingerTable<>(new Pointer<>(selfId, selfAddress));
        
        for (int i = 0; i < entryOvershoot.length; i++) {
            if (entryOvershoot[i] != null) {
                Id overshootAmountId = new Id(entryOvershoot[i], new BigInteger("2").pow(bitCount).subtract(BigInteger.ONE).toByteArray());
                Id fingId = ft.getExpectedId(i).add(overshootAmountId);
                InetSocketAddress fingAddress = generateAddressFromId(fingId);
                
                // must be greater than or equal to expected id
                Id expId = ft.getExpectedId(i);
                if (fingId.comparePosition(selfId, expId) < 0) {
                    throw new IllegalArgumentException();
                }
                
                // must be less than or equal to next expected id
                if (i < entryOvershoot.length - 1) {
                    Id nextId = ft.getExpectedId(i + 1);
                    if (fingId.comparePosition(selfId, nextId) > 0) {
                        throw new IllegalArgumentException();
                    }
                }
                
                Pointer<InetSocketAddress> fingPtr = new Pointer<>(fingId, fingAddress);
                ft.put(fingPtr);
            }
        }
        
        return ft;
    }
    
    public static List<Pointer<InetSocketAddress>> generatePointers(int bitCount,
            long startIdData) {
        List<Pointer<InetSocketAddress>> ret = new ArrayList<>();
        
        long idData = startIdData;
        long len = 1L << (long) bitCount;
        for (long i = 0; i < len; i++) {
            ret.add(generatePointer(bitCount, idData + i));
        }
        
        return ret;
    }

    public static List<Id> generateIds(int bitCount, long startIdData) {
        List<Id> ret = new ArrayList<>();
        
        long idData = startIdData;
        long len = 1L << (long) bitCount;
        for (long i = 0; i < len; i++) {
            ret.add(generateId(bitCount, idData + i));
        }
        
        return ret;
    }
    
    public static int generatePortFromId(Id id) {
        int hash = Arrays.hashCode(id.getValueAsByteArray());
        int port = (hash % 65535) + 1;
        return port;
    }
}
