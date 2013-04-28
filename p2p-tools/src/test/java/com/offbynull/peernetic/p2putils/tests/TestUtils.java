package com.offbynull.peernetic.p2putils.tests;

import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import com.offbynull.peernetic.p2ptools.overlay.chord.FingerTable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class TestUtils {
    
    private TestUtils() {
        
    }
    
    public static BitLimitedId generateId(int bitCount, long idData) {
        byte[] bytes = ByteBuffer.allocate(8).putLong(idData).array();
        return new BitLimitedId(bitCount, bytes);
    }
    
    public static InetSocketAddress generateAddressFromId(BitLimitedId id) {
        byte[] data = Arrays.copyOf(id.asByteArray(), 16);
        try {
            return new InetSocketAddress(InetAddress.getByAddress(data), 1);
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static BitLimitedPointer generatePointer(int bitCount, long idData) {
        BitLimitedId id = generateId(bitCount, idData);
        InetSocketAddress address = generateAddressFromId(id);
        return new BitLimitedPointer(id, address);
    }
    
    public static FingerTable generateFingerTable(long idData,
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
    
    public static FingerTable generateFingerTable(byte[] idData,
            byte[] ... entryOvershoot) {
        int bitCount = entryOvershoot.length;
        
        BitLimitedId selfId = new BitLimitedId(bitCount, idData);
        InetSocketAddress selfAddress = generateAddressFromId(selfId);
        FingerTable ft = new FingerTable(new BitLimitedPointer(selfId, selfAddress));
        
        for (int i = 0; i < entryOvershoot.length; i++) {
            if (entryOvershoot[i] != null) {
                BitLimitedId overshootAmountId = new BitLimitedId(bitCount, entryOvershoot[i]);
                BitLimitedId fingId = ft.getExpectedId(i).add(overshootAmountId);
                InetSocketAddress fingAddress = generateAddressFromId(fingId);
                
                // must be greater than or equal to expected id
                BitLimitedId expId = ft.getExpectedId(i);
                if (fingId.comparePosition(selfId, expId) < 0) {
                    throw new IllegalArgumentException();
                }
                
                // must be less than or equal to next expected id
                if (i < entryOvershoot.length - 1) {
                    BitLimitedId nextId = ft.getExpectedId(i + 1);
                    if (fingId.comparePosition(selfId, nextId) > 0) {
                        throw new IllegalArgumentException();
                    }
                }
                
                BitLimitedPointer fingPtr = new BitLimitedPointer(fingId, fingAddress);
                ft.put(fingPtr);
            }
        }
        
        return ft;
    }
}
