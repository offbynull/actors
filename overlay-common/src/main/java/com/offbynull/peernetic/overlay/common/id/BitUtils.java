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

        byte[] limitRaw = new byte[(bitCount / 8) + 1];
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

        boolean alignedToByte = (bitCount % 8) == 0 ? true : false;
        int finalBytesRemainder = 8 - (bitCount % 8);
        int finalBytesCount = (bitCount / 8)
                + (alignedToByte ? 0 : 1);
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
