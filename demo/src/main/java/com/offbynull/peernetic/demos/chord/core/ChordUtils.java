package com.offbynull.peernetic.demos.chord.core;

import com.offbynull.peernetic.common.Id;
import java.math.BigInteger;
import org.apache.commons.lang3.Validate;

public final class ChordUtils {
    private ChordUtils() {
        // do nothing
    }
    
    /**
     * Checks if the limit of the id passed in satisfies {@code 2^n-1}. In other words, ensures that all bits making up the limit are
     * {@code 1}.
     * @param id id to test
     * @return {@code true} if limit of id matches {@code 2^n-1}, {@code false} otherwise.
     * @throws NullPointerException if any argument is {@code null}
     */
    public static boolean isUseableId(Id id) {
        Validate.notNull(id);
        BigInteger limit = id.getLimitAsBigInteger();
        int bitLength = limit.bitLength();
        
        for (int i = 0; i < bitLength; i++) {
            if (!limit.testBit(i)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Gets the bit size of the limit of the of id passed in. The id must be a chord ID (see
     * {@link #isUseableId(com.offbynull.peernetic.common.Id) }).
     * @param id id to grab bit length from
     * @return bit length of id
     * @throws IllegalArgumentException if limit of id doesn't satisfy {@code 2^n-1}
     * @throws NullPointerException if any argument is {@code null}
     */
    public static int getBitLength(Id id) {
        Validate.isTrue(isUseableId(id));
        BigInteger limit = id.getLimitAsBigInteger();
        return limit.not().getLowestSetBit();
    }
}
