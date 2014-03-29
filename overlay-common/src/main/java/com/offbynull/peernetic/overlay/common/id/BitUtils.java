/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.overlay.common.id;

import java.util.Arrays;

final class BitUtils {

    private BitUtils() {
        // Do nothing
    }
    
    public static byte[] getMaxValue(int bitCount) {
        if (bitCount <= 0) {
            throw new IllegalArgumentException();
        }

        byte[] limitRaw = new byte[(bitCount / 8) + 1]; // NOPMD
        for (int i = 0; i < limitRaw.length; i++) {
            limitRaw[i] = (byte) 0xFF;
        }
        limitRaw = truncateBytes(limitRaw, bitCount);

        return limitRaw;
    }

    public static byte[] truncateBytes(byte[] bytes, int bitCount) {
        if (bitCount <= 0) {
            throw new IllegalArgumentException();
        }

        if (bytes.length * 8 < bitCount) {
            return Arrays.copyOf(bytes, bytes.length);
        }

        boolean alignedToByte = (bitCount % 8) == 0; // NOPMD
        int finalBytesRemainder = 8 - (bitCount % 8);  // NOPMD
        int finalBytesCount = (bitCount / 8) + (alignedToByte ? 0 : 1);  // NOPMD
        int diffBytes = bytes.length - finalBytesCount;

        byte[] finalBytes = new byte[finalBytesCount];
        System.arraycopy(bytes, diffBytes, finalBytes, 0, finalBytesCount);

        if (!alignedToByte) {
            byte mask = -128; // 1000 0000
            mask >>= finalBytesRemainder - 1;
            mask = (byte) ~mask;
            finalBytes[0] = (byte) (finalBytes[0] & mask);
        }

        return finalBytes;
    }
}
