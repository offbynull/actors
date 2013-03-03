package com.offbynull.chord;

public final class RouteResult {

    private ResultType resultType;
    private Pointer pointer;

    public RouteResult(ResultType resultType, Pointer pointer) {
        if (resultType == null || pointer == null) {
            throw new NullPointerException();
        }
        this.resultType = resultType;
        this.pointer = pointer;
    }

    public ResultType getResultType() {
        return resultType;
    }

    public Pointer getPointer() {
        return pointer;
    }
    
    public enum ResultType {
        SELF,
        FOUND,
        CLOSEST_PREDECESSOR
    }
}
