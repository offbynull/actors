package com.offbynull.peernetic.chord;

import java.net.NetworkInterface;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Enumeration;

public final class DefaultIdGenerator implements IdGenerator {

    private SecureRandom secureRandom;

    public DefaultIdGenerator() throws NoSuchAlgorithmException,
            NoSuchProviderException {
        secureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");
    }

    @Override
    public Id generate(int bitCount) {
        if (bitCount <= 0) {
            throw new IllegalArgumentException();
        }
        try {
            int rawLen = (bitCount / 8) + 1;
            byte[] raw = new byte[rawLen];
            secureRandom.nextBytes(raw);
            
            Enumeration<NetworkInterface> niEnum
                    = NetworkInterface.getNetworkInterfaces();

            boolean[] rawOccupied = new boolean[rawLen];
            while (niEnum.hasMoreElements()) {
                NetworkInterface ni = niEnum.nextElement();
                byte[] address = ni.getHardwareAddress();

                for (int i = 0; i < address.length; i++) {
                    if (secureRandom.nextBoolean()) {
                        int rawPos = secureRandom.nextInt(rawLen);
                        if (!rawOccupied[rawPos]) {
                            raw[rawPos] ^= address[i];
                            rawOccupied[rawPos] = true;
                        }
                    }
                }
            }

            return new Id(bitCount, raw);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
