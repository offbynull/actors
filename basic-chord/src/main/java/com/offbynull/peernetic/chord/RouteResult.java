package com.offbynull.peernetic.chord;

import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;


/**
 * The result of a routing operation from {@link FingerTable}.
 * @author Kasra Faghihi
 */
public final class RouteResult {
    private ResultType resultType;
    private BitLimitedPointer pointer;

    public RouteResult(ResultType resultType, BitLimitedPointer pointer) {
        if (resultType == null || pointer == null) {
            throw new NullPointerException();
        }
        this.resultType = resultType;
        this.pointer = pointer;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public BitLimitedPointer getPointer() {
        return pointer;
    }

    public enum ResultType {

        SELF, FOUND, CLOSEST_PREDECESSOR
    }
    
}
